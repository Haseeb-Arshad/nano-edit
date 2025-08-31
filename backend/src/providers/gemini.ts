import { config } from '../config'
import { logger } from '../logger'
import { request } from 'undici'

// Minimal Gemini image generateContent call wrapper
export async function callGemini(imageBase64: string, imageMime: string, prompt: string, maskBase64?: string) {
  if (!config.gemini.apiKey) throw new Error('GEMINI_API_KEY missing')
  const url = `${config.gemini.endpoint}/v1beta/models/${encodeURIComponent(config.gemini.model)}:generateContent`

  const parts: any[] = []
  // Instruction to respect mask if provided
  const instruction = maskBase64
    ? `${prompt}\n\nApply edits only to regions marked in the provided mask: white=edit, black=keep.`
    : prompt
  parts.push({ text: instruction })
  // Append main image
  parts.push({ inline_data: { mime_type: imageMime, data: imageBase64 } })
  // Append mask if present
  if (maskBase64) {
    parts.push({ inline_data: { mime_type: 'image/png', data: maskBase64, purpose: 'inpaint' } })
  }

  const body = {
    contents: [
      {
        role: 'user',
        parts
      }
    ]
  }

  const start = Date.now()
  const res = await request(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'x-goog-api-key': config.gemini.apiKey },
    body: JSON.stringify(body),
    maxRedirections: 0,
    bodyTimeout: config.gemini.timeoutMs
  })
  const ms = Date.now() - start
  logger.info({ ms }, 'Gemini response time')
  if (ms > config.gemini.longLatencyMs) {
    logger.warn({ ms }, 'Gemini long latency')
  }
  if (res.statusCode >= 400) {
    const text = await res.body.text()
    throw new Error(`Gemini error ${res.statusCode}: ${text}`)
  }
  return await res.body.json()
}

export function pickImageBase64FromResponse(json: any): string | null {
  try {
    const candidates = json.candidates || []
    for (const c of candidates) {
      const parts = c?.content?.parts || []
      for (const p of parts) {
        const inline = p.inline_data
        if (inline?.data) return inline.data
      }
    }
  } catch {}
  return null
}

