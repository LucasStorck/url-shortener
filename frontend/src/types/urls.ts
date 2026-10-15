export interface CreateUrlRequest {
  url: string
  expiresInDays: number
}

export interface CreateUrlResponse {
  code: string
  shortUrl: string
  targetUrl: string
  expiresAt: string | null
}