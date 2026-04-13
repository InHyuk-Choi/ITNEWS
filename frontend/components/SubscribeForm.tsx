'use client'

import { useState } from 'react'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

export function SubscribeForm() {
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
      if (res.ok) setEmail('')
    } catch {
      setStatus('error')
      setMessage('오류가 발생했습니다. 다시 시도해주세요.')
    }
  }

  return (
    <div className="border-t border-gray-200 dark:border-[#2a2a2a] mt-16 pt-12 pb-8">
      <div className="max-w-md mx-auto text-center">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-1">
          주간 IT 뉴스레터
        </h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
          매주 월요일, 주요 IT 뉴스를 이메일로 받아보세요.
        </p>

        {status === 'success' ? (
          <p className="text-sm text-emerald-600 dark:text-emerald-400 font-medium">
            ✓ {message}
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="flex gap-2">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="이메일 주소"
              required
              className="flex-1 px-4 py-2 text-sm rounded-lg border border-gray-200 dark:border-[#2a2a2a] bg-gray-50 dark:bg-[#1a1a1a] text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-orange-400/50 focus:border-orange-400"
            />
            <button
              type="submit"
              disabled={status === 'loading'}
              className="px-4 py-2 text-sm font-medium rounded-lg bg-gradient-to-r from-yellow-400 to-orange-500 text-white hover:opacity-90 disabled:opacity-50 transition-opacity whitespace-nowrap"
            >
              {status === 'loading' ? '...' : '구독하기'}
            </button>
          </form>
        )}

        {status === 'error' && (
          <p className="text-xs text-red-500 mt-2">{message}</p>
        )}
      </div>
    </div>
  )
}
