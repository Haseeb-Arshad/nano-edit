End-to-End Test Checklist

Prereqs
- Backend running (mock provider): bun dev and bun worker
- MinIO + Redis via docker-compose up -d
- TOKEN=dev-token

Exact Cases
1) Happy path no mask
   - Input: normal landscape, prompt: "Make colors vivid"
   - Expect: status=done, result_url valid, overlay visible (mock)

2) Happy path with mask
   - Input: portrait + matching mask
   - Expect: background changed only (mock applies overlay restricted by mask)

3) Huge file
   - Input: ~12MB image
   - Expect: backend auto-scale to <=2048px and recompress; success

4) Slow provider
   - Simulate: include "[SLOW]" in prompt
   - Expect: job remains processing for >30s, polling continues, final status=done

5) Provider error
   - Simulate: include "[ERROR]" in prompt
   - Expect: job status=error with message; retry policy attempts once (BullMQ attempts=2)

6) NSFW blocked
   - Simulate: header X-Force-NSFW: true or prompt contains [NSFW]
   - Expect: 403 and error message

7) Mask misalignment
   - Send mask with different dimensions than image
   - Expect: 400 with clear message

8) Quota exceeded
   - Make > DAILY_BUDGET_MAX_CALLS submissions in a day
   - Expect: POST returns 429

9) Race / duplicate requests
   - Send duplicate requests with same client_request_id
   - Expect: same job_id returned

Metrics to record per test
- Request size, response size to provider (worker logs)
- Roundtrip time (harness prints)
- Provider latency (worker logs)
- Cost estimate (GET /api/v1/edit shows via status -> job; also saved in DB)
- Screenshots of before/after when using real provider

Harness
- bun run scripts/run_tests.ts <image> [mask]
- Exposes NSFW/Slow/Error via prompt tags

