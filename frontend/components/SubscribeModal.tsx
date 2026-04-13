'use client'

import { useState } from 'react'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

interface Props {
  onClose: () => void
}

export function SubscribeModal({ onClose }: Props) {
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle')
  const [message, setMessage] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!email.trim()) return
    setStatus('loading')
    try {
      const res = await fetch(`${API_URL}/api/subscribe`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      })
      const data = await res.json()
      setMessage(data.message)
      setStatus(res.ok ? 'success' : 'error')
    } catch {
      setStatus('error')
      setMessage('오류가 발생했습니다. 다시 시도해주세요.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60" onClick={onClose}>
      <div
        className="bg-white dark:bg-[#1a1a1a] rounded-2xl p-8 w-full max-w-md mx-4 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">📰 주간 뉴스레터 구독</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
          매주 월요일, AI가 선별한 주요 IT 뉴스 10개를 이메일로 받아보세요.
        </p>

        {status === 'success' ? (
          <div className="text-center py-4">
            <div className="text-3xl mb-3">✓</div>
            <p className="text-emerald-600 dark:text-emerald-400 font-medium">{message}</p>
            <button onClick={onClose} className="mt-4 text-sm text-gray-400 hover:text-gray-600">닫기</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="이메일 주소를 입력하세요"
              required
              className="w-full px-4 py-2.5 text-sm rounded-lg border border-gray-200 dark:border-[#2a2a2a] bg-gray-50 dark:bg-[#0a0a0a] text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-400/50 focus:border-orange-400"
            />
            {status === 'error' && (
              <p className="text-xs text-red-500">{message}</p>
            )}
            <button
              type="submit"
              disabled={status === 'loading'}
              className="w-full py-2.5 text-sm font-medium rounded-lg bg-gradient-to-r from-yellow-400 to-orange-500 text-white hover:opacity-90 disabled:opacity-50 transition-opacity"
            >
              {status === 'loading' ? '처리 중...' : '구독하기'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
