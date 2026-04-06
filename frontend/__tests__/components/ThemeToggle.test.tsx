import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'

// ── Mock: next-themes ─────────────────────────────────────────────────────────
const mockSetTheme = jest.fn()
let mockTheme = 'light'

jest.mock('next-themes', () => ({
  useTheme: () => ({
    theme: mockTheme,
    setTheme: mockSetTheme,
    resolvedTheme: mockTheme,
    themes: ['light', 'dark', 'system'],
  }),
}))

// ── Component stub (실제 컴포넌트가 없을 때 대체) ─────────────────────────────
// 실제 ThemeToggle 컴포넌트가 생성되면 아래 import 주석 해제:
// import ThemeToggle from '@/components/ThemeToggle'

const { useTheme } = jest.requireMock('next-themes')

const ThemeToggle: React.FC = () => {
  const { theme, setTheme } = useTheme()

  const toggle = () => {
    setTheme(theme === 'dark' ? 'light' : 'dark')
  }

  return (
    <button
      data-testid="theme-toggle"
      onClick={toggle}
      aria-label={theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
    >
      {theme === 'dark' ? '☀️' : '🌙'}
    </button>
  )
}

// ── 테스트 ────────────────────────────────────────────────────────────────────
describe('ThemeToggle', () => {
  beforeEach(() => {
    mockSetTheme.mockClear()
    mockTheme = 'light'
  })

  it('초기 렌더링 시 토글 버튼이 존재한다', () => {
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    expect(button).toBeInTheDocument()
  })

  it('light 모드일 때 다크 모드로 전환하는 버튼이 렌더링된다', () => {
    mockTheme = 'light'
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    expect(button).toHaveAttribute('aria-label', '다크 모드로 전환')
  })

  it('dark 모드일 때 라이트 모드로 전환하는 버튼이 렌더링된다', () => {
    mockTheme = 'dark'
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    expect(button).toHaveAttribute('aria-label', '라이트 모드로 전환')
  })

  it('light 모드에서 클릭 시 setTheme("dark")가 호출된다', () => {
    mockTheme = 'light'
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    fireEvent.click(button)

    expect(mockSetTheme).toHaveBeenCalledTimes(1)
    expect(mockSetTheme).toHaveBeenCalledWith('dark')
  })

  it('dark 모드에서 클릭 시 setTheme("light")가 호출된다', () => {
    mockTheme = 'dark'
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    fireEvent.click(button)

    expect(mockSetTheme).toHaveBeenCalledTimes(1)
    expect(mockSetTheme).toHaveBeenCalledWith('light')
  })

  it('버튼이 접근성 aria-label을 가진다', () => {
    render(<ThemeToggle />)

    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('aria-label')
    expect(button.getAttribute('aria-label')).not.toBe('')
  })

  it('연속 클릭 시 setTheme이 매번 호출된다', () => {
    render(<ThemeToggle />)

    const button = screen.getByTestId('theme-toggle')
    fireEvent.click(button)
    fireEvent.click(button)
    fireEvent.click(button)

    expect(mockSetTheme).toHaveBeenCalledTimes(3)
  })
})
