Backend Integration and API Spec

Overview
- This app captures/edit images locally and offloads AI-powered edits to a Node/Express backend (in `backend/`) with Supabase storage/DB.
- The Android client uses two API shapes:
  - Compatibility: `POST /v1/edits` + `GET /v1/jobs/{id}` (JSON body), used by `NanoBananaApi`.
  - Preferred: `POST /api/v1/edit` + `GET /api/v1/edit/{id}` (multipart upload), used by `ApiService`.
- The backend persists job/task metadata in Supabase and stores outputs in a storage bucket (or IPFS via Pinata).

Android App Config
- In `local.properties` or Gradle env, set:
  - Local emulator: `NB_BASE_URL=http://10.0.2.2:4000/`
  - Physical device on LAN: `NB_BASE_URL=http://<LAN-IP>:4000/`
  - Deployed (Render): `NB_BASE_URL=https://nemo-edit.onrender.com/`
  - Token: `NB_API_KEY=<your backend DEV_TOKEN>` (default dev is `dev-token`)
- BuildConfig is wired in `di/NetworkModule.kt` to attach `Authorization: Bearer <NB_API_KEY>` to all requests.

Backend Setup (Node/Express + Supabase)
- From the repo root:
  - `cd backend`
  - `cp .env.example .env` and fill at least:
    - `PORT=4000`
    - `DEV_TOKEN=dev-token` (matches the Android NB_API_KEY)
    - `SUPABASE_URL=...` and `SUPABASE_SERVICE_ROLE_KEY=...` (from your Supabase project)
    - `SUPABASE_IMAGE_BUCKET=gen-images` (or your bucket name)
    - For real image generation: `GEMINI_API_KEY=...` (Google Generative AI)
    - Optional (IPFS): `PINATA_JWT=...` and related PINATA_* vars
  - Apply schema in Supabase: open the SQL editor and run `backend/supabase/schema.sql`, then seed with `backend/supabase/seed.sql` (optional).
  - Install and run:
    - `npm install`
    - `npm run dev` (or `npm run build && npm start`)

Auth Model
- The backend expects `Authorization: Bearer <token>`.
- In dev, `DEV_TOKEN` equals `dev-token`. The Android app should use the same value via `NB_API_KEY`.
- For production, replace `getUserIdFromAuth` with your real auth (e.g., Supabase JWT). Do not ship the Supabase service role key to clients.

Endpoints (Used by the App)
- Edit Jobs (preferred)
  - `POST /api/v1/edit`
    - Headers: `Authorization: Bearer <NB_API_KEY>`
    - Multipart form-data fields:
      - `file` (required): captured image
      - `prompt` (required): text description of edit(s)
      - `mask` (optional): PNG mask (white=edit, black=keep)
      - `client_request_id` (optional): any client correlation id
    - Response: `{ job_id: string, status: "accepted", estimated_cost_cents: number }`
    - Behavior: creates a Supabase `generation_tasks` row and starts background generation; original/mask uploaded to storage.
  - `GET /api/v1/edit/{id}`
    - Response: `{ job_id, status: "processing"|"done"|"error", result_url?: string }`
  - `GET /api/v1/edit/{id}/result`
    - Returns base64 for small images or redirects (302) to a signed URL when large.

- Edit Jobs (compat for `NanoBananaApi`)
  - `POST /v1/edits`
    - JSON body: `{ imageUrl: string, maskUrl?: string, prompt: string, strength?: number, upscaling?: boolean, size?: string }`
    - Response: `{ id: string, status: "running", resultUrl: null }`
    - Behavior: backend fetches `imageUrl`/`maskUrl`, creates a task, and runs edit in background.
  - `GET /v1/jobs/{id}`
    - Response: `{ id: string, status: "queued"|"running"|"done"|"failed", resultUrl?: string }`

- Generation APIs (optional, used by web/experiments)
  - `GET /health` → `{ ok: true }`
  - `GET /styles` → `{ categories, styles }` (from `style_categories`/`image_styles`)
  - `GET /styles/:slug` → `{ style, presets, filters }`
  - `GET /prompts?style=:slug` → `{ presets }`
  - `POST /generate-image` (multipart `image` optional + fields `styleSlug`, `promptId`, `promptText`, `quality`, `filters`, `variations`)
  - `GET /tasks/:id` → `{ task, outputs: [{..., publicUrl}] }`

Supabase Schema (data model)
- Storage
  - Bucket: `gen-images` (public or signed access). Configurable via `SUPABASE_IMAGE_BUCKET`.
  - Alternative: IPFS via Pinata (set `PINATA_JWT`), in which case URLs are public through the gateway.
- Tables (from `backend/supabase/schema.sql`)
  - Catalog:
    - `style_categories(id, slug, name, description, sort_order)`
    - `image_styles(id, category_id, slug, name, description, base_prompt, attributes, active, sort_order)`
    - `prompt_presets(id, style_id, slug, name, prompt_template, variables, active)`
    - `filters(id, slug, name, type, config, active)` and `style_filters(style_id, filter_id, default_strength)`
    - `qualities(id, slug, name, model, config, active, sort_order)`
  - Runtime:
    - `generation_tasks(id, user_id, status, style_id, prompt_id, prompt, params, input_image_path, output_text, error, created_at, completed_at)`
    - `generation_outputs(id, task_id, index, storage_bucket, storage_path, mime, size, width, height, metadata, created_at)`
  - Indexes and RLS are included; by default, catalog and tasks are readable (adjust for your auth model).

Android ↔ Backend Mapping
- Editor flow (current code):
  - Compatibility path: `EditRepository.submitEdit(imageUrl, prompt)` calls `POST /v1/edits`, then polls `GET /v1/jobs/{id}` until `done`.
  - Preferred high-quality path: `uploadFullResAndPoll(context, uri, prompt)` posts the raw image via `POST /api/v1/edit` and polls `GET /api/v1/edit/{id}`.
  - Both are implemented on the backend and persist outputs to Supabase storage; the first output file becomes `result_url`.
- Other app features (Camera, Gallery, Settings) are local-only and don’t require backend. Saved images go to the device’s MediaStore.

Production Notes
- Replace `getUserIdFromAuth` with real auth (e.g., Supabase JWT) and tie `generation_tasks.user_id` to the authenticated user.
- Tighten RLS: restrict `generation_tasks`/`generation_outputs` select to `auth.uid()`; keep catalog read-only to anon or authenticated roles as needed.
- Do not embed Supabase service role keys in clients. Use only your own access tokens for the backend.

Troubleshooting
- If Android cannot reach the backend from emulator, ensure `NB_BASE_URL` uses `10.0.2.2` rather than `localhost`.
- If `/api/v1/edit` returns 401, verify `NB_API_KEY` matches backend `DEV_TOKEN`.
- If no output URLs are returned, check Supabase storage bucket name and that `schema.sql` has been applied.

Render Deployment Notes
- Set `DEV_TOKEN` in Render env and use the same value in Android `NB_API_KEY`.
- Ensure `GEMINI_API_KEY`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, and `SUPABASE_IMAGE_BUCKET` are present in Render env.
- The app defaults to `https://nemo-edit.onrender.com/` and `dev-token` if not overridden.
