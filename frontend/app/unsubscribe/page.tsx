'use client'

import { useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'

import { API_URL } from '@/lib/api'

function UnsubscribeContent() {
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
    fetch(`${API_URL}/api/unsubscribe?token=${token}`)
      .then((res) => res.json())
      .then((data) => {
        setMessage(data.message)
        setStatus('success')
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
          <p className="text-gray-500 dark:text-gray-400">처리 중...</p>
        )}
        {status === 'success' && (
          <>
            <div className="text-4xl mb-4">✓</div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-white mb-2">{message}</h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">더 이상 뉴스레터를 받지 않습니다.</p>
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

export default function UnsubscribePage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><p className="text-gray-500">처리 중...</p></div>}>
      <UnsubscribeContent />
    </Suspense>
  )
}
