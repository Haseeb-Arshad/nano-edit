import sharp from 'sharp'

export interface ImageInfo {
  width: number
  height: number
  sizeBytes: number
  mime: string
}

export async function getImageInfo(buf: Buffer): Promise<ImageInfo> {
  const m = await sharp(buf).metadata()
  return {
    width: m.width || 0,
    height: m.height || 0,
    sizeBytes: buf.length,
    mime: m.format === 'png' ? 'image/png' : 'image/jpeg'
  }
}

export async function compressIfNeeded(buf: Buffer, maxBytes = 3 * 1024 * 1024, maxDim = 2048, mime: string = 'image/jpeg'): Promise<{ out: Buffer, width: number, height: number } | { out: Buffer, width: number, height: number }> {
  const img = sharp(buf)
  const meta = await img.metadata()
  const width = meta.width || 0
  const height = meta.height || 0

  if (buf.length <= maxBytes && Math.max(width, height) <= maxDim) {
    return { out: buf, width, height }
  }

  const scale = Math.min(1, maxDim / Math.max(width, height))
  const targetW = Math.round(width * scale)
  const targetH = Math.round(height * scale)

  let pipeline = sharp(buf).resize({ width: targetW, height: targetH, fit: 'inside' })
  if (mime === 'image/png') {
    pipeline = pipeline.png({ compressionLevel: 9 })
  } else {
    pipeline = pipeline.jpeg({ quality: 85 })
  }
  let out = await pipeline.toBuffer()
  // If still too large, try lowering quality for jpeg
  if (out.length > maxBytes && mime !== 'image/png') {
    out = await sharp(buf).resize({ width: targetW, height: targetH, fit: 'inside' }).jpeg({ quality: 75 }).toBuffer()
  }
  const dim = await sharp(out).metadata()
  return { out, width: dim.width || targetW, height: dim.height || targetH }
}

export async function resizeMaskTo(mask: Buffer, w: number, h: number): Promise<Buffer> {
  // Use nearest-neighbor to preserve mask edges
  return sharp(mask).resize({ width: w, height: h, fit: 'fill', kernel: sharp.kernel.nearest }).png().toBuffer()
}

export function estimateMb(bytes: number): number {
  return bytes / (1024 * 1024)
}
