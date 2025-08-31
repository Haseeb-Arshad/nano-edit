import dayjs from 'dayjs'
import Redis from 'ioredis'
import { config } from './config'

export class BudgetGuard {
  constructor(private redis: Redis) {}

  private key(date: string) { return `budget:${date}` }
  private userKey(userId: string, date: string) { return `quota:${userId}:${date}` }

  async checkAndIncrement(userId: string) {
    const today = dayjs().format('YYYY-MM-DD')
    const [daily, user] = await this.redis.mget(this.key(today), this.userKey(userId, today))
    const dailyCount = Number(daily || '0')
    const userCount = Number(user || '0')

    if (dailyCount >= config.budgets.dailyMaxCalls) {
      return { ok: false, code: 429, message: 'Daily budget exceeded' }
    }
    if (userCount >= config.budgets.userDailyQuota) {
      return { ok: false, code: 429, message: 'User daily quota exceeded' }
    }
    const pipeline = this.redis.multi()
    pipeline.incr(this.key(today))
    pipeline.incr(this.userKey(userId, today))
    // set TTL of 48h to auto-expire keys
    pipeline.expire(this.key(today), 172800)
    pipeline.expire(this.userKey(userId, today), 172800)
    await pipeline.exec()
    return { ok: true }
  }
}

