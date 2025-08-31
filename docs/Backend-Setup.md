Backend Setup (Bun + Mock Gemini)

Run Services
- cd backend
- docker-compose up -d  # MinIO + Redis
- cp .env.example .env  # adjust if needed
- bun install
- bun dev               # server on :8000
- bun worker            # queue worker

Android App Config
- In local.properties or env, set:
  - NB_BASE_URL=http://10.0.2.2:8000/   (Android emulator) or http://<LAN-IP>:8000/
  - NB_API_KEY=dev-token

Retrofit
- `ApiService` added at `app/src/main/java/com/example/myapplication/network/ApiService.kt`
- You can inject and call it from your repository using Multipart upload as in the checklist.

Switch to Real Provider
- Set PROVIDER=gemini and GEMINI_API_KEY=... in backend .env
- Keep quotas low initially; validate logs to confirm response parsing uses candidates[0].content.parts[*].inline_data.data

