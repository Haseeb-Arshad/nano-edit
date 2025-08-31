export interface PostEditResponse {
  job_id: string
  status: 'accepted'
  estimated_cost_cents: number
}

export interface GetEditResponse {
  job_id: string
  status: 'pending' | 'processing' | 'done' | 'error'
  progress?: number
  result_url?: string | null
  error?: string | null
}

