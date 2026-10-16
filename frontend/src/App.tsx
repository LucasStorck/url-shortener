import {useState, type SubmitEvent} from 'react'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card'
import {Alert, AlertDescription} from '@/components/ui/alert'
import type {CreateUrlResponse} from "@/types/urls";
import {ApiError, createShortUrl} from "@/services/urlService";

export default function App() {
  const [url, setUrl] = useState("")
  const [expiresInDays, setExpiresInDays] = useState("")
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<CreateUrlResponse | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [copied, setCopied] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    setLoading(true)
    setError(null)
    setResult(null)
    setCopied(false)
    try {
      setResult(await createShortUrl({
        url,
        expiresInDays: expiresInDays ? Number(expiresInDays) : undefined
      }))
    } catch (e) {
      setError(e instanceof ApiError ? e : new ApiError("Unexpected error."))
    } finally {
      setLoading(false)
    }
  }

  async function handleCopy() {
    if (!result) return
    await navigator.clipboard.writeText(result.shortUrl)
    setCopied(true)
  }

  return (
    <main className="mx-auto max-w-xl p-8">
      <h1 className="font-heading mb-6 text-5xl uppercase">Encurtador</h1>
      <Card>
        <CardHeader><CardTitle>Nova URL</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <Input value={url} onChange={(e) => setUrl(e.target.value)}
                     placeholder="https://exemplo.com/pagina-longa"/>
              {error?.fieldErrors.url && <p className="mt-1 text-sm">{error.fieldErrors.url}</p>}
            </div>
            <div>
              <Input type="number" value={expiresInDays}
                     onChange={(e) => setExpiresInDays(e.target.value)}
                     placeholder="Expira em (dias), opcional"/>
              {error?.fieldErrors.expiresInDays &&
                  <p className="mt-1 text-sm">{error.fieldErrors.expiresInDays}</p>}
            </div>
            <Button type="submit" disabled={loading}>{loading ? "Encurtando..." : "Encurtar"}</Button>
          </form>
        </CardContent>
      </Card>

      {error && Object.keys(error.fieldErrors).length === 0 && (
        <Alert className="mt-6"><AlertDescription>{error.message}</AlertDescription></Alert>
      )}

      {result && (
        <Card className="mt-6">
          <CardContent className="flex items-center justify-between gap-4">
            <a href={result.shortUrl.replace(":5173", ":8080")}
               className="font-mono break-all underline">{result.shortUrl}</a>
            <Button variant="neutral" onClick={handleCopy}>{copied ? "Copiado!" : "Copiar"}</Button>
          </CardContent>
        </Card>
      )}
    </main>
  )
}