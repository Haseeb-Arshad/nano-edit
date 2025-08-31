import Redis from 'ioredis'
import dayjs from 'dayjs'

const REDIS_URL = process.env.REDIS_URL || 'redis://127.0.0.1:6379'
const redis = new Redis(REDIS_URL)

async function main() {
  const today = dayjs().format('YYYY-MM-DD')
  const dailyKey = `budget:${today}`
  const count = Number(await redis.get(dailyKey) || '0')
  console.log(`Daily calls today: ${count}`)
  if (process.argv[2] === 'halt') {
    await redis.set(dailyKey, '9999999')
    console.log('Budget effectively halted for today')
  }
  await redis.quit()
}

main()

