// Mock provider: echoes image with semi-transparent overlay to prove E2E integrity
import sharp from 'sharp'

function sleep(ms: number) { return new Promise(res => setTimeout(res, ms)) }

export async function callMockProvider(image: Buffer, prompt: string, mask?: Buffer) {
  if (prompt.includes('[ERROR]')) {
    throw new Error('Mock provider forced error')
  }
  if (prompt.includes('[SLOW]')) {
    await sleep(35000)
  }
  // Create a simple overlay rectangle (semi-transparent red) same dims
  const meta = await sharp(image).metadata()
  const width = meta.width || 64
  const height = meta.height || 64

  const overlay = await sharp({
    create: {
      width,
      height,
      channels: 4,
      background: { r: 255, g: 0, b: 0, alpha: 0.18 }
    }
  }).png().toBuffer()

  // If mask present, compose overlay only where mask white
  let composed: Buffer
  if (mask) {
    // Use mask as alpha channel for overlay
    const maskAlpha = await sharp(mask).ensureAlpha().toBuffer()
    const overlayMasked = await sharp(overlay).joinChannel(maskAlpha).toBuffer()
    composed = await sharp(image).composite([{ input: overlayMasked }]).png().toBuffer()
  } else {
    composed = await sharp(image).composite([{ input: overlay }]).png().toBuffer()
  }

  // Return Gemini-like structure
  const base64 = composed.toString('base64')
  return {
    candidates: [
      {
        content: {
          parts: [
            { text: prompt },
            { inline_data: { mime_type: 'image/png', data: base64 } }
          ]
        }
      }
    ]
  }
}
