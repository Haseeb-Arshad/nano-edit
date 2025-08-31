import { connection } from './queue'
import { Worker } from 'bullmq'
import { logger } from './logger'
import { config } from './config'
import { getJobById, getUploadById, setJobStatus } from './db'
import { getObjectStream, putObject } from './storage/s3'
import { callMockProvider } from './providers/mock'
import { callGemini, pickImageBase64FromResponse } from './providers/gemini'
import { compressIfNeeded, getImageInfo, resizeMaskTo, estimateMb } from './utils/image'

async function readKeyToBuffer(key: string): Promise<Buffer> {
  const stream = await getObjectStream(key)
  const chunks: Buffer[] = []
  for await (const chunk of stream) chunks.push(Buffer.from(chunk))
  return Buffer.concat(chunks)
}

const worker = new Worker('edits', async (job) => {
  const { jobId, userId } = job.data as { jobId: string, userId: string }
  const row = getJobById(jobId)
  if (!row) {
    logger.error({ jobId }, 'Job not found in DB')
    return
  }
  setJobStatus(jobId, 'processing', { progress: 5 })

  try {
    // Load original and optional mask
    const upload = getUploadById(row.upload_id)
    if (!upload) throw new Error('Upload not found')
    const original = await readKeyToBuffer(upload.original_key)
    const origInfo = await getImageInfo(original)

    let prepared = await compressIfNeeded(original, 3 * 1024 * 1024, 2048, origInfo.mime)
    let imageForProvider = prepared.out
    let w = prepared.width
    let h = prepared.height

    let maskBuf: Buffer | undefined
    if (row.mask_key) {
      const maskOriginal = await readKeyToBuffer(row.mask_key)
      // If scaled image, scale mask to match
      const resizedMask = await resizeMaskTo(maskOriginal, w, h)
      maskBuf = resizedMask
    }

    setJobStatus(jobId, 'processing', { progress: 20 })

    const imageBase64 = imageForProvider.toString('base64')
    const maskBase64 = maskBuf ? maskBuf.toString('base64') : undefined

    const reqBytes = imageForProvider.length + (maskBuf?.length || 0) + Buffer.byteLength(row.prompt)
    let resBytes = 0

    let resultBase64: string | null = null
    const start = Date.now()
    if (config.provider === 'mock') {
      const json = await callMockProvider(imageForProvider, row.prompt, maskBuf)
      resultBase64 = pickImageBase64FromResponse(json)
    } else {
      const json = await callGemini(imageBase64, origInfo.mime, row.prompt, maskBase64)
      resultBase64 = pickImageBase64FromResponse(json)
    }
    const ms = Date.now() - start
    logger.info({ jobId, ms }, 'Provider call complete')

    if (!resultBase64) throw new Error('Provider returned no image result')
    const resultBuf = Buffer.from(resultBase64, 'base64')
    resBytes = resultBuf.length

    // Save result as PNG
    const resultKey = `edited/${userId}/${jobId}.png`
    await putObject(resultKey, resultBuf, 'image/png')

    // Estimate final cost
    const cost = Math.round(estimateMb(reqBytes) * config.costs.perInputMbCents + estimateMb(resBytes) * config.costs.perOutputMbCents)

    setJobStatus(jobId, 'done', { progress: 100, result_key: resultKey, req_bytes: reqBytes, res_bytes: resBytes, estimated_cost_cents: cost })
  } catch (e: any) {
    logger.error({ err: e?.message || String(e), jobId }, 'Job failed')
    setJobStatus(jobId, 'error', { error: e?.message || 'Unknown error' })
    throw e
  }
}, { connection })

worker.on('ready', () => logger.info('Worker ready'))
worker.on('failed', (job, err) => logger.error({ jobId: job?.id, err: err?.message }, 'Worker job failed'))
worker.on('completed', (job) => logger.info({ jobId: job?.id }, 'Worker job completed'))
