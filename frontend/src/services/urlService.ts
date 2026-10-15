import type {CreateUrlRequest, CreateUrlResponse} from "@/types/urls.ts";

export class ApiError extends Error {
  fieldErrors: Record<string, string>

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.fieldErrors = fieldErrors
  }
}

export async function createShortUrl(request: CreateUrlRequest): Promise<CreateUrlResponse> {
  let response: Response
  try {
    response = await fetch("/api/urls", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(request)
    })
  } catch {
    throw new ApiError("Unable to communicate with the server.")
  }

  if (response.ok) {
    return response.json()
  }

  if (response.status === 400) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(problem?.detail ?? "Invalid request.", problem?.errors ?? {})
  }

  throw new ApiError("Unexpected server error. Please try again.")
}