Backend (Bun) — Image Edit Service

Overview
- Bun-based HTTP API that accepts image edit requests, enqueues jobs, calls a provider (mock Gemini by default), stores files in MinIO (S3-compatible), and exposes polling + result endpoints.
- Implements safety guardrails (NSFW stub, daily budget), idempotency, image compression/scaling, and cost accounting.

Quick Start
1) Prereqs
   - Bun: https://bun.sh (>= 1.1)
   - Docker + Docker Compose (for MinIO + Redis)

2) Start dependencies
   - docker-compose up -d
   - MinIO console on http://localhost:9001 (user: minioadmin / pass: minioadmin)

3) Configure env
   - Copy .env.example to .env and adjust if needed

4) Install & run
   - bun install
   - bun dev

5) Test
   - Use the curl examples below (Authorization: Bearer dev-token by default).

Env & Safety
- Never put GEMINI_API_KEY in mobile. Set it here in .env or secret manager.
- Default provider is mock (no cost). Switch to real by PROVIDER=gemini and set GEMINI_API_KEY.
- DAILY_BUDGET_MAX_CALLS limits POST /api/v1/edit calls per day; returns 429 when exceeded.

API
- POST /api/v1/edit (multipart: file [jpeg/png], prompt [text], optional mask [png], optional client_request_id)
  Returns: { job_id, status:"accepted", estimated_cost_cents }

- GET /api/v1/edit/{job_id}
  Returns: { job_id, status, progress?, result_url?, error? }

- GET /api/v1/edit/{job_id}/result
  - If small, responds with { result_base64 } (JSON)
  - Else 302 redirect to presigned S3 URL (5 min TTL)

Curl Examples
Synchronous submit
curl -X POST "http://localhost:8000/api/v1/edit" \
  -H "Authorization: Bearer dev-token" \
  -F "file=@/path/to/photo.jpg" \
  -F "prompt=Remove the trash can near subject and make sky look brighter." \
  -F "client_request_id=$(uuidgen)"

Poll
curl -X GET "http://localhost:8000/api/v1/edit/<job_id>" -H "Authorization: Bearer dev-token"

Result fetch
curl -i -X GET "http://localhost:8000/api/v1/edit/<job_id>/result" -H "Authorization: Bearer dev-token"

Mocks
- POST /mock/gemini: internal mock provider, returns original with a semi-transparent overlay; used by worker when PROVIDER=mock.

Notes
- Large images (>4MB) are downscaled to max 2048px on longer side and recompressed to keep provider payload ~<4MB. Mask is resized to match.
- NSFW check is a stub that flags when header X-Force-NSFW: true or prompt contains [NSFW].
- Idempotency: client_request_id + user_id deduplicates requests.

