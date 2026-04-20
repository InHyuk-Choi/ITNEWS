// 서버사이드(Docker 내부)는 API_URL, 클라이언트사이드(브라우저)는 NEXT_PUBLIC_API_URL
export const API_URL = typeof window === 'undefined'
  ? (process.env.API_URL || 'http://localhost:8080')
  : (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080')

export interface NewsItem {
  id: number
  title: string
  url: string
  source: string
  summary: string | null
  thumbnail: string | null
  publishedAt: string
}

export interface NewsResponse {
  content: NewsItem[]
  totalPages: number
  currentPage: number
  totalElements: number
}

export async function searchNews(
  q: string,
  page = 0,
  size = 20
): Promise<NewsResponse> {
  const params = new URLSearchParams({ q, page: String(page), size: String(size) })
  try {
    const res = await fetch(`${API_URL}/api/news/search?${params.toString()}`)
    if (!res.ok) throw new Error(`API error: ${res.status}`)
    return res.json()
  } catch (error) {
    console.error('Search failed:', error)
    return { content: [], totalPages: 0, currentPage: 0, totalElements: 0 }
  }
}

export async function fetchNews(
  source?: string,
  page = 0,
  size = 20
): Promise<NewsResponse> {
  const params = new URLSearchParams()
  if (source && source !== 'all') params.set('source', source)
  params.set('page', String(page))
  params.set('size', String(size))

  const url = `${API_URL}/api/news?${params.toString()}`

  try {
    const res = await fetch(url, {
      next: { revalidate: 3600 },
    })

    if (!res.ok) {
      throw new Error(`API error: ${res.status}`)
    }

    return res.json()
  } catch (error) {
    console.error('Failed to fetch news:', error)
    // Fallback: return empty response when API is unavailable
    return {
      content: [],
      totalPages: 0,
      currentPage: 0,
      totalElements: 0,
    }
  }
}
