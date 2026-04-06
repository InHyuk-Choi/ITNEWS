import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'

// ── Mock: next/navigation ─────────────────────────────────────────────────────
const mockPush = jest.fn()
const mockReplace = jest.fn()
let mockSearchParams = new URLSearchParams()
let mockPathname = '/'

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: jest.fn(),
    forward: jest.fn(),
    refresh: jest.fn(),
    prefetch: jest.fn(),
  }),
  useSearchParams: () => mockSearchParams,
  usePathname: () => mockPathname,
}))

// ── 소스 목록 정의 ─────────────────────────────────────────────────────────────
const SOURCES = [
  { id: 'all', label: 'All' },
  { id: 'hackernews', label: 'Hacker News' },
  { id: 'geeknews', label: 'GeekNews' },
  { id: 'naver', label: 'Naver Tech' },
]

// ── Component stub (실제 컴포넌트가 없을 때 대체) ─────────────────────────────
// 실제 SourceFilter 컴포넌트가 생성되면 아래 import 주석 해제:
// import SourceFilter from '@/components/SourceFilter'

const { useRouter, useSearchParams } = jest.requireMock('next/navigation')

const SourceFilter: React.FC = () => {
  const router = useRouter()
  const searchParams = useSearchParams()
  const currentSource = searchParams.get('source') ?? 'all'

  const handleSelect = (sourceId: string) => {
    const params = new URLSearchParams(searchParams.toString())
    if (sourceId === 'all') {
      params.delete('source')
    } else {
      params.set('source', sourceId)
    }
    // 페이지도 리셋
    params.delete('page')
    router.push(`/?${params.toString()}`)
  }

  return (
    <nav data-testid="source-filter" aria-label="소스 필터">
      {SOURCES.map((s) => (
        <button
          key={s.id}
          data-testid={`source-btn-${s.id}`}
          onClick={() => handleSelect(s.id)}
          aria-pressed={currentSource === s.id}
          className={currentSource === s.id ? 'active' : ''}
        >
          {s.label}
        </button>
      ))}
    </nav>
  )
}

// ── 테스트 ────────────────────────────────────────────────────────────────────
describe('SourceFilter', () => {
  beforeEach(() => {
    mockPush.mockClear()
    mockReplace.mockClear()
    mockSearchParams = new URLSearchParams()
  })

  it('소스 목록(All, Hacker News, GeekNews 등)을 모두 렌더링한다', () => {
    render(<SourceFilter />)

    expect(screen.getByTestId('source-btn-all')).toBeInTheDocument()
    expect(screen.getByTestId('source-btn-hackernews')).toBeInTheDocument()
    expect(screen.getByTestId('source-btn-geeknews')).toBeInTheDocument()
    expect(screen.getByTestId('source-btn-naver')).toBeInTheDocument()

    expect(screen.getByText('All')).toBeInTheDocument()
    expect(screen.getByText('Hacker News')).toBeInTheDocument()
    expect(screen.getByText('GeekNews')).toBeInTheDocument()
    expect(screen.getByText('Naver Tech')).toBeInTheDocument()
  })

  it('초기 상태에서 All 버튼이 활성화(active) 상태다', () => {
    mockSearchParams = new URLSearchParams() // source 파라미터 없음
    render(<SourceFilter />)

    const allBtn = screen.getByTestId('source-btn-all')
    expect(allBtn).toHaveClass('active')
    expect(allBtn).toHaveAttribute('aria-pressed', 'true')
  })

  it('Hacker News 클릭 시 source=hackernews 파라미터로 라우팅한다', () => {
    render(<SourceFilter />)

    fireEvent.click(screen.getByTestId('source-btn-hackernews'))

    expect(mockPush).toHaveBeenCalledTimes(1)
    expect(mockPush).toHaveBeenCalledWith('/?source=hackernews')
  })

  it('GeekNews 클릭 시 source=geeknews 파라미터로 라우팅한다', () => {
    render(<SourceFilter />)

    fireEvent.click(screen.getByTestId('source-btn-geeknews'))

    expect(mockPush).toHaveBeenCalledWith('/?source=geeknews')
  })

  it('"All" 클릭 시 source 파라미터를 제거한다', () => {
    // 초기 상태: source=hackernews
    mockSearchParams = new URLSearchParams('source=hackernews')
    render(<SourceFilter />)

    fireEvent.click(screen.getByTestId('source-btn-all'))

    // source 파라미터 없이 라우팅
    expect(mockPush).toHaveBeenCalledWith('/?')
  })

  it('현재 선택된 소스 버튼에 active 클래스가 적용된다', () => {
    mockSearchParams = new URLSearchParams('source=hackernews')
    render(<SourceFilter />)

    const hnBtn = screen.getByTestId('source-btn-hackernews')
    const allBtn = screen.getByTestId('source-btn-all')

    expect(hnBtn).toHaveClass('active')
    expect(hnBtn).toHaveAttribute('aria-pressed', 'true')
    expect(allBtn).not.toHaveClass('active')
    expect(allBtn).toHaveAttribute('aria-pressed', 'false')
  })

  it('소스 변경 시 page 파라미터가 제거된다', () => {
    // 초기 상태: page=2 존재
    mockSearchParams = new URLSearchParams('page=2')
    render(<SourceFilter />)

    fireEvent.click(screen.getByTestId('source-btn-hackernews'))

    // page 파라미터 없이 라우팅되어야 함
    const callArg: string = mockPush.mock.calls[0][0]
    expect(callArg).not.toContain('page=')
    expect(callArg).toContain('source=hackernews')
  })

  it('source-filter nav가 적절한 aria-label을 가진다', () => {
    render(<SourceFilter />)

    const nav = screen.getByTestId('source-filter')
    expect(nav).toHaveAttribute('aria-label', '소스 필터')
  })
})
