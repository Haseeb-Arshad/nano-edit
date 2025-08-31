import { readFileSync } from 'node:fs'
import { callGemini, pickImageBase64FromResponse } from '../src/providers/gemini'

async function main() {
  const imgPath = process.argv[2]
  const prompt = process.argv.slice(3).join(' ') || 'Make colors vivid'
  if (!imgPath) {
    console.error('Usage: bun run scripts/direct_gemini.ts <imagePath> [prompt]')
    process.exit(1)
  }
  const img = readFileSync(imgPath)
  const b64 = img.toString('base64')
  const json = await callGemini(b64, 'image/jpeg', prompt)
  const out = pickImageBase64FromResponse(json)
  console.log('Has image?', !!out)
}

main().catch(e => { console.error(e); process.exit(1) })

