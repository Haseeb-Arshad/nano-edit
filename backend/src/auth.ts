import { Context } from 'hono'
import { config } from './config'

export function getUserIdFromAuth(c: Context): string | null {
  const auth = c.req.header('authorization') || ''
  const m = auth.match(/^Bearer\s+(.+)$/i)
  const token = m?.[1]
  if (!token) return null
  // Simple dev token check; in real use, map token -> user_id
  if (token === config.devToken) return 'dev-user'
  return null
}

