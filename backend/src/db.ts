import { Database } from 'bun:sqlite'
import dayjs from 'dayjs'
import { randomUUID } from 'node:crypto'

export type JobStatus = 'pending' | 'processing' | 'done' | 'error'

export interface UploadRow {
  id: string
  user_id: string
  original_key: string
  original_mime: string
  original_bytes: number
  created_at: string
}

export interface JobRow {
  id: string
  user_id: string
  upload_id: string
  mask_key: string | null
  prompt: string
  status: JobStatus
  progress: number | null
  result_key: string | null
  error: string | null
  client_request_id: string | null
  provider: string
  req_bytes: number | null
  res_bytes: number | null
  estimated_cost_cents: number | null
  created_at: string
  updated_at: string
}

const db = new Database('data.db', { create: true })
db.exec('PRAGMA journal_mode = WAL;')

db.exec(`
  CREATE TABLE IF NOT EXISTS uploads (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    original_key TEXT NOT NULL,
    original_mime TEXT NOT NULL,
    original_bytes INTEGER NOT NULL,
    created_at TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS jobs (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    upload_id TEXT NOT NULL,
    mask_key TEXT,
    prompt TEXT NOT NULL,
    status TEXT NOT NULL,
    progress INTEGER,
    result_key TEXT,
    error TEXT,
    client_request_id TEXT,
    provider TEXT NOT NULL,
    req_bytes INTEGER,
    res_bytes INTEGER,
    estimated_cost_cents INTEGER,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  );
  CREATE INDEX IF NOT EXISTS idx_jobs_user_client ON jobs(user_id, client_request_id);
`)

const stmtInsertUpload = db.query('INSERT INTO uploads (id,user_id,original_key,original_mime,original_bytes,created_at) VALUES (?,?,?,?,?,?)')
const stmtInsertJob = db.query('INSERT INTO jobs (id,user_id,upload_id,mask_key,prompt,status,progress,result_key,error,client_request_id,provider,req_bytes,res_bytes,estimated_cost_cents,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)')
const stmtGetJobById = db.query('SELECT * FROM jobs WHERE id = ?')
const stmtGetJobByClient = db.query('SELECT * FROM jobs WHERE user_id = ? AND client_request_id = ?')
const stmtUpdateJobStatus = db.query('UPDATE jobs SET status=?, progress=?, result_key=?, error=?, req_bytes=?, res_bytes=?, estimated_cost_cents=?, updated_at=? WHERE id = ?')
const stmtGetUploadById = db.query('SELECT * FROM uploads WHERE id = ?')

export function nowISO() {
  return dayjs().toISOString()
}

export function newId() {
  return randomUUID()
}

export function createUpload(userId: string, originalKey: string, originalMime: string, originalBytes: number): UploadRow {
  const row: UploadRow = {
    id: newId(),
    user_id: userId,
    original_key: originalKey,
    original_mime: originalMime,
    original_bytes: originalBytes,
    created_at: nowISO()
  }
  stmtInsertUpload.run(row.id, row.user_id, row.original_key, row.original_mime, row.original_bytes, row.created_at)
  return row
}

export function createJob(params: Omit<JobRow, 'created_at' | 'updated_at'>): JobRow {
  const created_at = nowISO()
  const updated_at = created_at
  const row: JobRow = { ...params, created_at, updated_at }
  stmtInsertJob.run(
    row.id, row.user_id, row.upload_id, row.mask_key, row.prompt, row.status, row.progress, row.result_key,
    row.error, row.client_request_id, row.provider, row.req_bytes, row.res_bytes, row.estimated_cost_cents, row.created_at, row.updated_at
  )
  return row
}

export function setJobStatus(id: string, status: JobStatus, patch: Partial<Pick<JobRow, 'progress' | 'result_key' | 'error' | 'req_bytes' | 'res_bytes' | 'estimated_cost_cents'>> = {}) {
  const progress = patch.progress ?? null
  const result_key = patch.result_key ?? null
  const error = patch.error ?? null
  const req_bytes = patch.req_bytes ?? null
  const res_bytes = patch.res_bytes ?? null
  const estimated_cost_cents = patch.estimated_cost_cents ?? null
  stmtUpdateJobStatus.run(status, progress, result_key, error, req_bytes, res_bytes, estimated_cost_cents, nowISO(), id)
}

export function getJobById(id: string): JobRow | null {
  return (stmtGetJobById.get(id) as JobRow) || null
}

export function getJobByClientId(userId: string, clientId: string): JobRow | null {
  return (stmtGetJobByClient.get(userId, clientId) as JobRow) || null
}

export function getUploadById(id: string): UploadRow | null {
  return (stmtGetUploadById.get(id) as UploadRow) || null
}

export { db }
