'use client'

import { useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { formatDistanceToNow } from 'date-fns'
import { ko } from 'date-fns/locale'
import { getSourceMeta } from '@/lib/sources'
import type { NewsItem } from '@/lib/api'

interface NewsModalProps {
  item: NewsItem | null
  onClose: () => void
}

export function NewsModal({ item, onClose }: NewsModalProps) {
  useEffect(() => {
    if (!item) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [item, onClose])

  // 스크롤 잠금
  useEffect(() => {
    if (item) document.body.style.overflow = 'hidden'
    else document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [item])

  const sourceMeta = item ? getSourceMeta(item.source) : null

  const timeAgo = item ? (() => {
    try {
      return formatDistanceToNow(new Date(item.publishedAt), { addSuffix: true, locale: ko })
    } catch { return '' }
  })() : ''

  return (
    <AnimatePresence>
      {item && (
        <>
          {/* Backdrop */}
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 bg-black/70"
            onClick={onClose}
          />

          {/* Modal */}
          <motion.div
            key="modal"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none"
          >
            <div
              className="relative w-full max-w-2xl max-h-[85vh] overflow-y-auto pointer-events-auto
                bg-white dark:bg-[#1a1a1a]
                rounded-2xl shadow-2xl
                border border-gray-100 dark:border-[#2a2a2a]"
              onClick={e => e.stopPropagation()}
            >
              {/* Close button */}
              <button
                onClick={onClose}
                className="absolute top-4 right-4 z-10
                  w-8 h-8 flex items-center justify-center rounded-full
                  bg-gray-100 dark:bg-[#2a2a2a]
                  text-gray-500 dark:text-gray-400
                  hover:bg-gray-200 dark:hover:bg-[#333]
                  transition-colors duration-150"
                aria-label="닫기"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                  <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                </svg>
              </button>

              {/* Header gradient (no thumbnail) */}
              <div
                className="h-2 rounded-t-2xl"
                style={{ background: sourceMeta!.bg }}
              />

              <div className="p-6 sm:p-8">
                {/* Source + time */}
                <div className="flex items-center gap-3 mb-4">
                  <span
                    className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold"
                    style={{ backgroundColor: sourceMeta!.bg + '20', color: sourceMeta!.bg }}
                  >
                    <span className="w-1.5 h-1.5 rounded-full mr-1.5" style={{ backgroundColor: sourceMeta!.bg }} />
                    {sourceMeta!.label}
                  </span>
                  {timeAgo && (
                    <span className="text-xs text-gray-400 dark:text-gray-500">{timeAgo}</span>
                  )}
                </div>

                {/* Title */}
                <h2 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white leading-snug mb-5">
                  {item.title}
                </h2>

                {/* Summary */}
                {item.summary ? (
                  <div className="mb-6">
                    <div className="flex items-center gap-2 mb-3">
                      <span className="text-xs font-semibold uppercase tracking-wider text-orange-500">
                        AI 요약
                      </span>
                      <div className="flex-1 h-px bg-orange-500/20" />
                    </div>
                    <p className="text-gray-700 dark:text-gray-300 leading-relaxed text-base whitespace-pre-line">
                      {item.summary}
                    </p>
                  </div>
                ) : (
                  <p className="text-gray-400 dark:text-gray-500 text-sm mb-6 italic">
                    요약을 생성 중이에요...
                  </p>
                )}

                {/* CTA */}
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-5 py-2.5
                    bg-orange-500 hover:bg-orange-600
                    text-white font-semibold text-sm rounded-xl
                    transition-colors duration-150"
                >
                  원문 보기
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                    <path fillRule="evenodd" d="M4.25 5.5a.75.75 0 00-.75.75v8.5c0 .414.336.75.75.75h8.5a.75.75 0 00.75-.75v-4a.75.75 0 011.5 0v4A2.25 2.25 0 0112.75 17h-8.5A2.25 2.25 0 012 14.75v-8.5A2.25 2.25 0 014.25 4h5a.75.75 0 010 1.5h-5z" clipRule="evenodd" />
                    <path fillRule="evenodd" d="M6.194 12.753a.75.75 0 001.06.053L16.5 4.44v2.81a.75.75 0 001.5 0v-4.5a.75.75 0 00-.75-.75h-4.5a.75.75 0 000 1.5h2.553l-9.056 8.194a.75.75 0 00-.053 1.06z" clipRule="evenodd" />
                  </svg>
                </a>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
