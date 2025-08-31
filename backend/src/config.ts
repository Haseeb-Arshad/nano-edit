import 'dotenv/config'

const num = (v: string | undefined, d: number) => {
  const n = v ? Number(v) : NaN
  return Number.isFinite(n) ? n : d
}

export const config = {
  port: num(process.env.PORT, 8000),
  devToken: process.env.DEV_TOKEN || 'dev-token',
  provider: (process.env.PROVIDER || 'mock') as 'mock' | 'gemini',
  gemini: {
    apiKey: process.env.GEMINI_API_KEY || '',
    model: process.env.GEMINI_MODEL || 'gemini-1.5-flash',
    endpoint: 'https://generativelanguage.googleapis.com',
    timeoutMs: 60_000,
    longLatencyMs: 20_000
  },
  s3: {
    endpoint: process.env.S3_ENDPOINT || 'http://127.0.0.1:9000',
    region: process.env.S3_REGION || 'us-east-1',
    bucket: process.env.S3_BUCKET || 'ai-edits',
    accessKey: process.env.S3_ACCESS_KEY || 'minioadmin',
    secretKey: process.env.S3_SECRET_KEY || 'minioadmin',
    usePathStyle: (process.env.S3_USE_PATH_STYLE || 'true') === 'true',
    presignTtlSeconds: num(process.env.S3_PRESIGN_TTL_SECONDS, 300)
  },
  redisUrl: process.env.REDIS_URL || 'redis://127.0.0.1:6379',
  budgets: {
    dailyMaxCalls: num(process.env.DAILY_BUDGET_MAX_CALLS, 100),
    userDailyQuota: num(process.env.USER_DAILY_QUOTA, 50)
  },
  costs: {
    perInputMbCents: num(process.env.COST_PER_INPUT_MB_CENTS, 20),
    perOutputMbCents: num(process.env.COST_PER_OUTPUT_MB_CENTS, 10)
  }
}

export type ProviderName = typeof config.provider

