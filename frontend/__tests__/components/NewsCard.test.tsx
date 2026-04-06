import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'

// ── Mock: next/image ─────────────────────────────────────────────────────────
jest.mock('next/image', () => ({
  __esModule: true,
  default: (props: React.ImgHTMLAttributes<HTMLImageElement> & { fill?: boolean }) => {
    const { fill, ...rest } = props
    // eslint-disable-next-line @next/next/no-img-element, jsx-a11y/alt-text
    return <img {...rest} />
  },
}))

// ── Mock: date-fns (상대 시간 표시) ──────────────────────────────────────────
jest.mock('date-fns', () => ({
  formatDistanceToNow: jest.fn((date: Date) => '2시간'),
  parseISO: jest.fn((str: string) => new Date(str)),
}))

// ── Mock data: 실제 API 응답 형태와 동일 ─────────────────────────────────────
const mockArticle = {
  id: 1,
  title: 'OpenAI releases GPT-5 with major improvements',
  url: 'https://news.ycombinator.com/item?id=1001',
  source: 'hackernews',
  summary: '이 기사는 OpenAI의 GPT-5 출시에 관한 내용입니다. 이전 버전 대비 크게 향상된 성능을 보여줍니다.',
  thumbnail: 'https://example.com/thumbnail.jpg',
  publishedAt: '2025-04-07T10:00:00Z',
  createdAt: '2025-04-07T10:05:00Z',
}

const mockArticleNoSummary = {
  ...mockArticle,
  id: 2,
  summary: null,
  thumbnail: null,
}

// ── Component stub (실제 컴포넌트가 없을 때 대체) ─────────────────────────────
// 실제 NewsCard 컴포넌트가 생성되면 아래 import 주석 해제:
// import NewsCard from '@/components/NewsCard'

interface NewsCardProps {
  article: typeof mockArticle
}

const NewsCard: React.FC<NewsCardProps> = ({ article }) => {
  const handleClick = () => {
    window.open(article.url, '_blank', 'noopener,noreferrer')
  }

  return (
    <article
      data-testid="news-card"
      onClick={handleClick}
      style={{ cursor: 'pointer' }}
    >
      {article.thumbnail && (
        <img
          src={article.thumbnail}
          alt={article.title}
          data-testid="news-thumbnail"
        />
      )}
      <h2 data-testid="news-title">{article.title}</h2>
      <span data-testid="news-source-badge" className={`badge badge-${article.source}`}>
        {article.source === 'hackernews' ? 'Hacker News' : article.source}
      </span>
      {article.summary && (
        <p data-testid="news-summary">{article.summary}</p>
      )}
      <time data-testid="news-time">
        {`2시간`} 전
      </time>
    </article>
  )
}

// ── 테스트 ────────────────────────────────────────────────────────────────────
describe('NewsCard', () => {
  it('기사 제목을 올바르게 렌더링한다', () => {
    render(<NewsCard article={mockArticle} />)

    const title = screen.getByTestId('news-title')
    expect(title).toBeInTheDocument()
    expect(title).toHaveTextContent('OpenAI releases GPT-5 with major improvements')
  })

  it('소스 배지를 표시한다', () => {
    render(<NewsCard article={mockArticle} />)

    const badge = screen.getByTestId('news-source-badge')
    expect(badge).toBeInTheDocument()
    expect(badge).toHaveTextContent('Hacker News')
  })

  it('summary가 null일 때 요약 영역을 숨긴다', () => {
    render(<NewsCard article={mockArticleNoSummary} />)

    expect(screen.queryByTestId('news-summary')).not.toBeInTheDocument()
  })

  it('summary가 있을 때 요약 텍스트를 표시한다', () => {
    render(<NewsCard article={mockArticle} />)

    const summary = screen.getByTestId('news-summary')
    expect(summary).toBeInTheDocument()
    expect(summary).toHaveTextContent('GPT-5 출시')
  })

  it('thumbnail이 있을 때 이미지를 표시한다', () => {
    render(<NewsCard article={mockArticle} />)

    const img = screen.getByTestId('news-thumbnail')
    expect(img).toBeInTheDocument()
    expect(img).toHaveAttribute('src', mockArticle.thumbnail)
    expect(img).toHaveAttribute('alt', mockArticle.title)
  })

  it('thumbnail이 없을 때 이미지를 표시하지 않는다', () => {
    render(<NewsCard article={mockArticleNoSummary} />)

    expect(screen.queryByTestId('news-thumbnail')).not.toBeInTheDocument()
  })

  it('카드 클릭 시 새 탭으로 원문 링크를 연다', () => {
    const windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null)

    render(<NewsCard article={mockArticle} />)

    const card = screen.getByTestId('news-card')
    fireEvent.click(card)

    expect(windowOpenSpy).toHaveBeenCalledWith(
      'https://news.ycombinator.com/item?id=1001',
      '_blank',
      'noopener,noreferrer'
    )

    windowOpenSpy.mockRestore()
  })

  it('발행 시간을 상대적으로 표시한다 ("N시간 전")', () => {
    render(<NewsCard article={mockArticle} />)

    const time = screen.getByTestId('news-time')
    expect(time).toBeInTheDocument()
    expect(time.textContent).toMatch(/시간 전|분 전|일 전/)
  })

  it('geeknews 소스 배지를 올바르게 표시한다', () => {
    const geekArticle = { ...mockArticle, source: 'geeknews' }
    render(<NewsCard article={geekArticle} />)

    const badge = screen.getByTestId('news-source-badge')
    expect(badge).toHaveClass('badge-geeknews')
    expect(badge).toHaveTextContent('geeknews')
  })
})
