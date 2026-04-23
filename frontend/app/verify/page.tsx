'use client'

import { useEffect, useState, Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import { API_URL } from '@/lib/api'

function VerifyContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get('token')
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setMessage('유효하지 않은 링크입니다.')
      return
    }
    fetch(`${API_URL}/api/subscribe/verify?token=${token}`)
      .then((res) => res.json())
      .then((data) => {
        setMessage(data.message)
        setStatus(data.success ? 'success' : 'error')
      })
      .catch(() => {
        setStatus('error')
        setMessage('오류가 발생했습니다.')
      })
  }, [token])

  return (
    <div className="min-h-screen bg-white dark:bg-[#0a0a0a] flex items-center justify-center">
      <div className="text-center px-4">
        {status === 'loading' && (
          <p className="text-gray-500 dark:text-gray-400">확인 중...</p>
        )}
        {status === 'success' && (
          <>
            <div className="text-4xl mb-4">✓</div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-2">{message}</h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">
              매주 월요일에 선별된 IT 뉴스를 받아보세요.
            </p>
            <a href="/" className="text-sm text-orange-500 hover:underline">홈으로 돌아가기</a>
          </>
        )}
        {status === 'error' && (
          <>
            <div className="text-4xl mb-4">✕</div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-2">{message}</h1>
            <a href="/" className="text-sm text-orange-500 hover:underline">홈으로 돌아가기</a>
          </>
        )}
      </div>
    </div>
  )
}

export default function VerifyPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center">
          <p className="text-gray-500">확인 중...</p>
        </div>
      }
    >
      <VerifyContent />
    </Suspense>
  )
}
