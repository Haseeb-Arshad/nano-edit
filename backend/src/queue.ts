import { Queue, QueueEvents, JobsOptions } from 'bullmq'
import Redis, { RedisOptions } from 'ioredis'
import { config } from './config'
import { logger } from './logger'

export interface EditJobPayload {
  jobId: string
  userId: string
}

const url = new URL(config.redisUrl)
const redisOptions: RedisOptions = {
  host: url.hostname,
  port: Number(url.port) || 6379,
  password: url.password || undefined,
  tls: url.protocol === 'rediss:' ? {} as any : undefined,
  maxRetriesPerRequest: null,
  enableReadyCheck: false
}

export const connection = new Redis(config.redisUrl, redisOptions)
connection.on('error', (err) => logger.warn({ err: err?.message }, 'Redis connection error'))

export const queue = new Queue<EditJobPayload>('edits', { connection: redisOptions })

export const queueEvents = new QueueEvents('edits', { connection: redisOptions })
queueEvents.on('error', (err) => logger.warn({ err: err?.message }, 'QueueEvents error'))

export function enqueueJob(payload: EditJobPayload, opts?: JobsOptions) {
  return queue.add('edit', payload, { attempts: 2, backoff: { type: 'exponential', delay: 5000 }, removeOnComplete: true, removeOnFail: 100, ...opts })
}
