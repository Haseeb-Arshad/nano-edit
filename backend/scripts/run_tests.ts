import { request } from 'undici'
import { readFileSync } from 'node:fs'

const BASE = process.env.BASE_URL || 'http://localhost:8000'
const TOKEN = process.env.TOKEN || 'dev-token'

async function postEdit(filePath: string, prompt: string, maskPath?: string, clientId?: string, headers: Record<string,string> = {}) {
  const fd = new FormData()
  const file = new File([readFileSync(filePath)], filePath.split(/[\\/]/).pop() || 'image.jpg', { type: 'image/jpeg' })
  fd.append('file', file)
  fd.append('prompt', prompt)
  if (maskPath) {
    const maskFile = new File([readFileSync(maskPath)], 'mask.png', { type: 'image/png' })
    fd.append('mask', maskFile)
  }
  if (clientId) fd.append('client_request_id', clientId)

  const res = await fetch(`${BASE}/api/v1/edit`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${TOKEN}`, ...headers },
    body: fd
  })
  const json = await res.json()
  return { status: res.status, json }
}

async function pollJob(jobId: string) {
  const start = Date.now()
  let attempts = 0
  let delay = 1500
  while (true) {
    attempts++
    const res = await fetch(`${BASE}/api/v1/edit/${jobId}`, { headers: { Authorization: `Bearer ${TOKEN}` } })
    const json: any = await res.json()
    if (json.status === 'done' || json.status === 'error') {
      const ms = Date.now() - start
      return { json, ms, attempts }
    }
    await new Promise(r => setTimeout(r, delay))
    if (attempts > 6) delay = 5000
  }
}

async function run() {
  const img = process.argv[2]
  if (!img) {
    console.error('Usage: bun run scripts/run_tests.ts <imagePath> [maskPath]')
    process.exit(1)
  }
  const mask = process.argv[3]

  const tests: Array<{ name: string, prompt: string, mask?: string, extraHeaders?: Record<string,string>, expect: string }>= [
    { name: 'Happy path no mask', prompt: 'Make colors vivid', expect: 'done' },
    { name: 'Happy path with mask', prompt: 'Replace background with soft bokeh out-of-focus park scene; keep person edges sharp and natural.', mask, expect: 'done' },
    { name: 'Huge file', prompt: 'Increase contrast and clarity', expect: 'done' },
    { name: 'Slow provider', prompt: 'Add warm golden-hour light to the scene, subtle rim-light on faces, maintain realism. [SLOW]', expect: 'done' },
    { name: 'Provider error', prompt: 'Remove the pole in the sky. [ERROR]', expect: 'error' },
    { name: 'NSFW blocked', prompt: 'Face enhancement. [NSFW]', extraHeaders: { 'X-Force-NSFW': 'true' }, expect: '403' },
  ]

  for (const t of tests) {
    console.log(`\n=== ${t.name} ===`)
    const start = Date.now()
    const r = await postEdit(img, t.prompt, t.mask, crypto.randomUUID(), t.extraHeaders)
    console.log('POST status:', r.status, r.json)
    if (String(t.expect) === '403') continue
    if (r.status !== 200) continue
    const jobId = r.json.job_id
    const poll = await pollJob(jobId)
    console.log('Final:', poll.json.status, 'time(ms)=', poll.ms)
    if (poll.json.status === 'done') {
      console.log('result_url:', poll.json.result_url)
    } else {
      console.log('error:', poll.json.error)
    }
  }

  // Idempotency test
  console.log('\n=== Idempotency ===')
  const clientId = crypto.randomUUID()
  const r1 = await postEdit(img, 'Subtle skin smoothing only, keep pores and natural texture, remove blemishes but not identity.', undefined, clientId)
  const r2 = await postEdit(img, 'Subtle skin smoothing only, keep pores and natural texture, remove blemishes but not identity.', undefined, clientId)
  console.log('IDs', r1.json.job_id, r2.json.job_id, 'same=', r1.json.job_id === r2.json.job_id)
}

run().catch(e => {
  console.error(e)
  process.exit(1)
})

