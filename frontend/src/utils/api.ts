const FALLBACK_API_ORIGIN = ''

const toApiUrl = (path: string, origin?: string) => {
  if (/^https?:\/\//.test(path)) {
    return path
  }
  if (path.startsWith('/')) {
    return origin ? `${origin}${path}` : path
  }
  return origin ? `${origin}/${path}` : `/${path}`
}

export const apiFetch = async (path: string, init?: RequestInit) => {
  const primaryUrl = toApiUrl(path, FALLBACK_API_ORIGIN)
  try {
    return await fetch(primaryUrl, init)
  } catch (error) {
    const fallbackUrl = toApiUrl(path, FALLBACK_API_ORIGIN)
    return await fetch(fallbackUrl, init)
  }
}
