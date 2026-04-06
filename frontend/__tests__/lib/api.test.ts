/**
 * api.ts 유닛 테스트
 * global.fetch를 mock하여 외부 API 호출 없이 로직을 검증합니다.
 */

// ── Mock data: 실제 API 응답 형태 ─────────────────────────────────────────────
const mockNewsItem = {
  id: 1,
  title: 'OpenAI releases GPT-5',
  url: 'https://news.ycombinator.com/item?id=1001',
  source: 'hackernews',
  summary: 'GPT-5 출시 소식입니다.',
  thumbnail: 'https://example.com/thumbnail.jpg',
  publishedAt: '2025-04-07T10:00:00Z',
  createdAt: '2025-04-07T10:05:00Z',
}

const mockNewsResponse = [mockNewsItem, { ...mockNewsItem, id: 2, title: 'Rust 2.0' }]

// ── api.ts 인라인 구현 (실제 파일이 없을 때 대체) ────────────────────────────
// 실제 lib/api.ts가 생성되면 아래 import 주석 해제:
// import { fetchNews, FetchNewsParams } from '@/lib/api'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

interface FetchNewsParams {
  source?: string
  page?: number
  size?: number
}

interface NewsItem {
  id: number
  title: string
  url: string
  source: string
  summary: string | null
  thumbnail: string | null
  publishedAt: string
  createdAt: string
}

async function fetchNews(params: FetchNewsParams = {}): Promise<NewsItem[]> {
  try {
    const query = new URLSearchParams()
    if (params.source) query.set('source', params.source)
    if (params.page !== undefined) query.set('page', String(params.page))
    if (params.size !== undefined) query.set('size', String(params.size))

    const queryStr = query.toString()
    const url = `${API_BASE_URL}/api/news${queryStr ? `?${queryStr}` : ''}`

    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
    })

    if (!response.ok) {
      console.error(`API error: ${response.status}`)
      return []
    }

    return await response.json()
  } catch (error) {
    console.error('fetchNews error:', error)
    return []
  }
}

// ── 테스트 ────────────────────────────────────────────────────────────────────
describe('fetchNews', () => {
  // fetch mock 초기화
  const originalFetch = global.fetch

  beforeEach(() => {
    global.fetch = jest.fn()
  })

  afterEach(() => {
    global.fetch = originalFetch
    jest.clearAllMocks()
  })

  // ── 성공 케이스 ───────────────────────────────────────────────────────────
  it('API 호출 성공 시 뉴스 배열을 반환한다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockNewsResponse,
    })

    const result = await fetchNews()

    expect(result).toHaveLength(2)
    expect(result[0].title).toBe('OpenAI releases GPT-5')
    expect(result[0].source).toBe('hackernews')
    expect(result[1].title).toBe('Rust 2.0')
  })

  // ── 에러 케이스: API 오류 시 빈 배열 fallback ─────────────────────────────
  it('API가 4xx/5xx 에러를 반환할 때 빈 배열을 반환한다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
    })

    const result = await fetchNews()

    expect(result).toEqual([])
  })

  it('fetch가 네트워크 오류를 던질 때 빈 배열을 반환한다', async () => {
    ;(global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Network error'))

    const result = await fetchNews()

    expect(result).toEqual([])
  })

  it('API가 404를 반환할 때 빈 배열을 반환한다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 404,
    })

    const result = await fetchNews()

    expect(result).toEqual([])
  })

  // ── source 파라미터 쿼리스트링 변환 ──────────────────────────────────────
  it('source 파라미터가 쿼리스트링으로 올바르게 변환된다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [mockNewsItem],
    })

    await fetchNews({ source: 'hackernews' })

    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string
    expect(calledUrl).toContain('source=hackernews')
    expect(calledUrl).toContain('/api/news')
  })

  it('page 파라미터가 쿼리스트링으로 올바르게 변환된다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockNewsResponse,
    })

    await fetchNews({ page: 2, size: 10 })

    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string
    expect(calledUrl).toContain('page=2')
    expect(calledUrl).toContain('size=10')
  })

  it('파라미터 없이 호출 시 쿼리스트링이 없다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockNewsResponse,
    })

    await fetchNews()

    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string
    expect(calledUrl).not.toContain('?')
    expect(calledUrl).toEndWith('/api/news')
  })

  it('source와 page를 함께 전달하면 모두 쿼리스트링에 포함된다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [mockNewsItem],
    })

    await fetchNews({ source: 'geeknews', page: 1, size: 5 })

    const calledUrl = (global.fetch as jest.Mock).mock.calls[0][0] as string
    expect(calledUrl).toContain('source=geeknews')
    expect(calledUrl).toContain('page=1')
    expect(calledUrl).toContain('size=5')
  })

  // ── 응답 데이터 형태 검증 ─────────────────────────────────────────────────
  it('응답 항목에 필수 필드(id, title, url, source, publishedAt)가 포함된다', async () => {
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [mockNewsItem],
    })

    const result = await fetchNews()

    const item = result[0]
    expect(item).toHaveProperty('id')
    expect(item).toHaveProperty('title')
    expect(item).toHaveProperty('url')
    expect(item).toHaveProperty('source')
    expect(item).toHaveProperty('publishedAt')
  })

  it('summary가 null인 항목도 정상적으로 처리된다', async () => {
    const noSummaryItem = { ...mockNewsItem, summary: null, thumbnail: null }
    ;(global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [noSummaryItem],
    })

    const result = await fetchNews()

    expect(result[0].summary).toBeNull()
    expect(result[0].thumbnail).toBeNull()
  })
})

// ── URL 빌더 유닛 테스트 ──────────────────────────────────────────────────────
describe('API URL builder', () => {
  it('소스 파라미터를 URL에 올바르게 인코딩한다', () => {
    const params = new URLSearchParams()
    params.set('source', 'hackernews')
    expect(params.toString()).toBe('source=hackernews')
  })

  it('page와 size를 함께 인코딩한다', () => {
    const params = new URLSearchParams()
    params.set('page', '0')
    params.set('size', '20')
    expect(params.toString()).toBe('page=0&size=20')
  })
})
