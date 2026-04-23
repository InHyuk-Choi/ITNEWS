'use client'

import { memo } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { ko } from 'date-fns/locale'
import { getSourceMeta } from '@/lib/sources'
import type { NewsItem } from '@/lib/api'

interface NewsCardProps {
  item: NewsItem
  index: number
  onClick: (item: NewsItem) => void
}

export const NewsCard = memo(function NewsCard({ item, onClick }: NewsCardProps) {
  const sourceMeta = getSourceMeta(item.source)

  const timeAgo = (() => {
    try {
      return formatDistanceToNow(new Date(item.publishedAt), { addSuffix: true, locale: ko })
    } catch { return '' }
  })()

  return (
    <article className="group">
      <button
        onClick={() => onClick(item)}
        className="w-full text-left px-4 py-3.5 hover:bg-gray-50 dark:hover:bg-[#0f0f0f] transition-colors duration-100 flex items-start gap-3"
        aria-label={item.title}
      >
        {/* Source color accent */}
        <div
          className="shrink-0 w-[3px] self-stretch rounded-full opacity-70"
          style={{ backgroundColor: sourceMeta.bg }}
        />

        <div className="flex-1 min-w-0">
          {/* Meta row */}
          <div className="flex items-center gap-1.5 mb-1.5">
            <span className="font-mono text-[10px] uppercase tracking-wider text-gray-400 dark:text-gray-500">
              {sourceMeta.label}
            </span>
            <span className="text-gray-300 dark:text-gray-700 text-[10px]">·</span>
            <span className="font-mono text-[10px] text-gray-400 dark:text-gray-500">
              {timeAgo}
            </span>
          </div>

          {/* Title */}
          <h2 className="text-sm font-medium text-gray-900 dark:text-gray-100 leading-snug group-hover:text-orange-600 dark:group-hover:text-orange-400 transition-colors duration-100">
            {item.title}
          </h2>

          {/* Summary */}
          {item.summary ? (
            <p className="text-xs text-gray-500 dark:text-gray-500 mt-1 line-clamp-1 leading-relaxed">
              {item.summary}
            </p>
          ) : (
            <p className="text-[10px] text-gray-300 dark:text-gray-700 mt-1 font-mono italic">
              generating summary...
            </p>
          )}
        </div>

        {/* Arrow */}
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 16 16"
          fill="currentColor"
          className="shrink-0 w-3 h-3 text-gray-300 dark:text-gray-700 group-hover:text-orange-400 mt-1 transition-colors"
        >
          <path fillRule="evenodd" d="M6.22 4.22a.75.75 0 0 1 1.06 0l3.25 3.25a.75.75 0 0 1 0 1.06l-3.25 3.25a.75.75 0 0 1-1.06-1.06L8.94 8 6.22 5.28a.75.75 0 0 1 0-1.06Z" clipRule="evenodd" />
        </svg>
      </button>
    </article>
  )
})
