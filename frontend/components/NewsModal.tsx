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
                bg-white dark:bg-[#111]
                rounded-lg shadow-2xl
                border border-gray-200 dark:border-[#2a2a2a]"
              onClick={e => e.stopPropagation()}
            >
              {/* Top accent bar */}
              <div className="h-[3px] rounded-t-lg" style={{ background: sourceMeta!.bg }} />

              {/* Close button */}
              <button
                onClick={onClose}
                className="absolute top-3 right-3 z-10
                  w-7 h-7 flex items-center justify-center rounded-md
                  text-gray-400 dark:text-gray-500
                  hover:bg-gray-100 dark:hover:bg-[#2a2a2a]
                  transition-colors duration-100"
                aria-label="닫기"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                  <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                </svg>
              </button>

              <div className="p-6">
                {/* Source + time */}
                <div className="flex items-center gap-2 mb-3">
                  <span className="font-mono text-[10px] uppercase tracking-wider" style={{ color: sourceMeta!.bg }}>
                    {sourceMeta!.label}
                  </span>
                  <span className="text-gray-300 dark:text-gray-700 text-[10px]">·</span>
                  {timeAgo && (
                    <span className="font-mono text-[10px] text-gray-400 dark:text-gray-500">{timeAgo}</span>
                  )}
                </div>

                {/* Title */}
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white leading-snug mb-5">
                  {item.title}
                </h2>

                {/* Summary */}
                {item.summary ? (
                  <div className="mb-6 border-l-2 border-gray-200 dark:border-[#2a2a2a] pl-4">
                    <p className="text-xs font-mono text-gray-400 dark:text-gray-500 mb-2">{"// AI 요약"}</p>
                    <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed whitespace-pre-line">
                      {item.summary}
                    </p>
                  </div>
                ) : (
                  <p className="font-mono text-xs text-gray-400 dark:text-gray-600 mb-6">
                    {"// generating summary..."}
                  </p>
                )}

                {/* CTA */}
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-4 py-2
                    border border-gray-200 dark:border-[#2a2a2a]
                    text-sm font-mono text-gray-700 dark:text-gray-300
                    rounded-md hover:border-gray-400 dark:hover:border-gray-500
                    hover:text-gray-900 dark:hover:text-white
                    transition-colors duration-100"
                >
                  원문 보기 →
                </a>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
