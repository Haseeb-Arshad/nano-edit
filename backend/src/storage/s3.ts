import { S3Client, PutObjectCommand, HeadObjectCommand, GetObjectCommand, CreateBucketCommand, ListBucketsCommand } from '@aws-sdk/client-s3'
import { getSignedUrl } from '@aws-sdk/s3-request-presigner'
import { fromIni } from '@aws-sdk/credential-providers'
import { config } from '../config'
import { Readable } from 'node:stream'

const s3 = new S3Client({
  region: config.s3.region,
  endpoint: config.s3.endpoint,
  forcePathStyle: config.s3.usePathStyle,
  credentials: config.s3.accessKey && config.s3.secretKey ? {
    accessKeyId: config.s3.accessKey,
    secretAccessKey: config.s3.secretKey
  } : fromIni()
})

async function ensureBucket() {
  const buckets = await s3.send(new ListBucketsCommand({}))
  const exists = (buckets.Buckets || []).some(b => b.Name === config.s3.bucket)
  if (!exists) {
    await s3.send(new CreateBucketCommand({ Bucket: config.s3.bucket }))
  }
}

ensureBucket().catch(() => {})

export async function putObject(key: string, body: Buffer, contentType: string) {
  try {
    await s3.send(new PutObjectCommand({ Bucket: config.s3.bucket, Key: key, Body: body, ContentType: contentType }))
  } catch (e: any) {
    // Try creating bucket once then retry
    await ensureBucket().catch(() => {})
    await s3.send(new PutObjectCommand({ Bucket: config.s3.bucket, Key: key, Body: body, ContentType: contentType }))
  }
}

export async function headObject(key: string) {
  return s3.send(new HeadObjectCommand({ Bucket: config.s3.bucket, Key: key }))
}

export async function getObjectStream(key: string) {
  const res = await s3.send(new GetObjectCommand({ Bucket: config.s3.bucket, Key: key }))
  return res.Body as Readable
}

export async function presignGetUrl(key: string, ttlSeconds = config.s3.presignTtlSeconds) {
  return getSignedUrl(s3, new GetObjectCommand({ Bucket: config.s3.bucket, Key: key }), { expiresIn: ttlSeconds })
}
