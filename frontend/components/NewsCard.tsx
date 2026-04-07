'use client'

import Image from 'next/image'
import { memo } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { ko } from 'date-fns/locale'
import { motion } from 'framer-motion'
import { getSourceMeta } from '@/lib/sources'
import type { NewsItem } from '@/lib/api'

interface NewsCardProps {
  item: NewsItem
  index: number
  onClick: (item: NewsItem) => void
}

export const NewsCard = memo(function NewsCard({ item, index, onClick }: NewsCardProps) {
  const sourceMeta = getSourceMeta(item.source)

  const timeAgo = (() => {
    try {
      return formatDistanceToNow(new Date(item.publishedAt), { addSuffix: true, locale: ko })
    } catch { return '' }
  })()

  return (
    <motion.article
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.05, ease: [0.25, 0.1, 0.25, 1] }}
      className="group relative"
    >
      <button
        onClick={() => onClick(item)}
        className="w-full text-left rounded-2xl overflow-hidden
          bg-white dark:bg-[#1a1a1a]
          border border-gray-100 dark:border-[#2a2a2a]
          shadow-sm hover:shadow-md dark:shadow-none
          dark:hover:border-[#3a3a3a]
          transition-all duration-200 ease-out
          hover:-translate-y-0.5
          cursor-pointer"
        aria-label={`${item.title} - 요약 보기`}
      >
        {/* Thumbnail */}
        {item.thumbnail && (
          <div className="relative h-48 overflow-hidden bg-gray-100 dark:bg-[#2a2a2a]">
            <Image
              src={item.thumbnail}
              alt={item.title}
              fill
              className="object-cover transition-transform duration-300 group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
              onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
            />
          </div>
        )}

        {!item.thumbnail && (
          <div
            className="h-36 flex items-center justify-center relative overflow-hidden"
            style={{ background: `linear-gradient(135deg, ${sourceMeta.bg}22, ${sourceMeta.bg}08)` }}
          >
            {/* Source watermark */}
            <span
              className="text-5xl font-black tracking-tighter select-none opacity-10"
              style={{ color: sourceMeta.bg }}
            >
              {sourceMeta.label.slice(0, 2).toUpperCase()}
            </span>
            {/* Top accent bar */}
            <div
              className="absolute top-0 left-0 right-0 h-0.5"
              style={{ background: sourceMeta.bg }}
            />
          </div>
        )}

        {/* Content */}
        <div className="p-5">
          {/* Source badge + time */}
          <div className="flex items-center justify-between mb-3">
            <span
              className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide"
              style={{ backgroundColor: sourceMeta.bg + '20', color: sourceMeta.bg }}
            >
              <span className="w-1.5 h-1.5 rounded-full mr-1.5" style={{ backgroundColor: sourceMeta.bg }} />
              {sourceMeta.label}
            </span>
            {timeAgo && (
              <span className="text-xs text-gray-400 dark:text-gray-500 tabular-nums">{timeAgo}</span>
            )}
          </div>

          {/* Title */}
          <h2 className="text-base font-semibold text-gray-900 dark:text-white leading-snug line-clamp-2 mb-2 group-hover:text-orange-500 dark:group-hover:text-orange-400 transition-colors duration-150">
            {item.title}
          </h2>

          {/* Summary preview */}
          {item.summary ? (
            <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed line-clamp-2 mb-4">
              {item.summary}
            </p>
          ) : (
            <p className="text-xs text-gray-400 dark:text-gray-600 italic mb-4">요약 생성 중...</p>
          )}

          {/* Footer */}
          <div className="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-[#2a2a2a]">
            <span className="text-xs text-gray-400 dark:text-gray-500 group-hover:text-orange-400 transition-colors">
              클릭해서 요약 보기
            </span>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"
              className="w-4 h-4 text-gray-300 dark:text-gray-600 group-hover:text-orange-400 transition-colors">
              <path fillRule="evenodd" d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
      </button>
    </motion.article>
  )
})
