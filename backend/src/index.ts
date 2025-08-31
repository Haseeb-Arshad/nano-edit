import { Hono } from 'hono'
import { logger as rootLogger } from './logger'
import { config } from './config'
import { getUserIdFromAuth } from './auth'
import { BudgetGuard } from './budget'
import Redis from 'ioredis'
import { enqueueJob } from './queue'
import { createJob, createUpload, getJobByClientId, getJobById, newId } from './db'
import { getImageInfo, compressIfNeeded, resizeMaskTo, estimateMb } from './utils/image'
import { putObject, presignGetUrl, headObject, getObjectStream } from './storage/s3'
import { PostEditResponse, GetEditResponse } from './types'
import { mimeTypes } from './mime'
import { logger } from './logger'

const app = new Hono()
const redis = new Redis(config.redisUrl)
const budget = new BudgetGuard(redis)

// Health
app.get('/health', (c) => c.json({ ok: true }))

// Gemini mock endpoint (for debugging)
import { callMockProvider } from './providers/mock'
app.post('/mock/gemini', async (c) => {
  const form = await c.req.parseBody()
  const file = form['file'] as File
  if (!file) return c.json({ error: 'file required' }, 400)
  const prompt = (form['prompt'] as string) || ''
  const maskFile = form['mask'] as File | undefined
  const buf = Buffer.from(await file.arrayBuffer())
  const maskBuf = maskFile ? Buffer.from(await maskFile.arrayBuffer()) : undefined
  const res = await callMockProvider(buf, prompt, maskBuf)
  return c.json(res)
})

// POST /api/v1/edit
app.post('/api/v1/edit', async (c) => {
  const userId = getUserIdFromAuth(c)
  if (!userId) return c.json({ error: 'Unauthorized' }, 401)

  const check = await budget.checkAndIncrement(userId)
  if (!check.ok) return c.json({ error: check.message }, check.code as number)

  const form = await c.req.formData()
  const file = form.get('file') as File | null
  const prompt = (form.get('prompt') as string | null) || ''
  const mask = form.get('mask') as File | null
  const clientRequestId = (form.get('client_request_id') as string | null) || null

  if (!file) return c.json({ error: 'file is required' }, 400)
  if (!prompt) return c.json({ error: 'prompt is required' }, 400)

  // Idempotency
  if (clientRequestId) {
    const existing = getJobByClientId(userId, clientRequestId)
    if (existing) {
      const resp: PostEditResponse = { job_id: existing.id, status: 'accepted', estimated_cost_cents: existing.estimated_cost_cents || 0 }
      return c.json(resp)
    }
  }

  // NSFW quick check (stub)
  const forceNsfw = (c.req.header('x-force-nsfw') || '').toLowerCase() === 'true'
  if (forceNsfw || prompt.includes('[NSFW]')) {
    return c.json({ error: 'Content flagged by moderation' }, 403)
  }

  const fileBuf = Buffer.from(await file.arrayBuffer())
  const info = await getImageInfo(fileBuf)
  if (!['image/jpeg', 'image/png'].includes(info.mime)) {
    return c.json({ error: 'file must be image/jpeg or image/png' }, 400)
  }
  let maskBuf: Buffer | undefined
  let maskInfo: { width: number, height: number } | undefined
  if (mask) {
    const maskBytes = Buffer.from(await mask.arrayBuffer())
    const maskMeta = await getImageInfo(maskBytes)
    maskBuf = maskBytes
    maskInfo = { width: maskMeta.width, height: maskMeta.height }
    if (maskMeta.mime !== 'image/png') return c.json({ error: 'mask must be image/png' }, 400)
    if (maskMeta.width !== info.width || maskMeta.height !== info.height) {
      return c.json({ error: 'Mask dimensions must exactly match image dimensions' }, 400)
    }
  }

  // Store original upload
  const jobId = newId()
  const uploadKey = `uploads/${userId}/${jobId}.` + (info.mime === 'image/png' ? 'png' : 'jpg')
  await putObject(uploadKey, fileBuf, info.mime)

  const uploadRow = createUpload(userId, uploadKey, info.mime, info.sizeBytes)

  let maskKey: string | null = null
  if (maskBuf) {
    maskKey = `uploads/${userId}/${jobId}.mask.png`
    await putObject(maskKey, maskBuf, 'image/png')
  }

  // Early estimate of cost based on original size (adjusted later by worker with actual provider req/res sizes)
  const estCost = Math.round(estimateMb(info.sizeBytes) * 100 * 0.35) // simple heuristic = ~35 cents per MB

  const job = createJob({
    id: jobId,
    user_id: userId,
    upload_id: uploadRow.id,
    mask_key: maskKey,
    prompt,
    status: 'pending',
    progress: 0,
    result_key: null,
    error: null,
    client_request_id: clientRequestId,
    provider: config.provider,
    req_bytes: null,
    res_bytes: null,
    estimated_cost_cents: estCost
  })

  await enqueueJob({ jobId, userId })

  const resp: PostEditResponse = { job_id: jobId, status: 'accepted', estimated_cost_cents: estCost }
  return c.json(resp)
})

// GET /api/v1/edit/:id
app.get('/api/v1/edit/:id', async (c) => {
  const userId = getUserIdFromAuth(c)
  if (!userId) return c.json({ error: 'Unauthorized' }, 401)
  const id = c.req.param('id')
  const job = getJobById(id)
  if (!job || job.user_id !== userId) return c.json({ error: 'Not found' }, 404)
  const out: GetEditResponse = {
    job_id: job.id,
    status: job.status as any,
    progress: job.progress ?? undefined,
    result_url: null,
    error: job.error ?? undefined
  }
  if (job.status === 'done' && job.result_key) {
    out.result_url = await presignGetUrl(job.result_key)
  }
  return c.json(out)
})

// GET /api/v1/edit/:id/result
app.get('/api/v1/edit/:id/result', async (c) => {
  const userId = getUserIdFromAuth(c)
  if (!userId) return c.json({ error: 'Unauthorized' }, 401)
  const id = c.req.param('id')
  const job = getJobById(id)
  if (!job || job.user_id !== userId) return c.json({ error: 'Not found' }, 404)
  if (job.status !== 'done' || !job.result_key) return c.json({ error: 'Result not available' }, 400)
  const head = await headObject(job.result_key)
  const size = Number(head.ContentLength || 0)
  if (size < 800_000) {
    const stream = await getObjectStream(job.result_key)
    const chunks: Buffer[] = []
    for await (const chunk of stream) chunks.push(Buffer.from(chunk))
    const base64 = Buffer.concat(chunks).toString('base64')
    return c.json({ result_base64: base64 })
  } else {
    const url = await presignGetUrl(job.result_key)
    return c.redirect(url, 302)
  }
})

export default {
  fetch: app.fetch,
  port: config.port,
}
