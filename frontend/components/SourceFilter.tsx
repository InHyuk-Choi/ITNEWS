'use client'

import { useRouter, useSearchParams } from 'next/navigation'
import { SOURCE_FILTERS } from '@/lib/sources'

export function SourceFilter() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const currentSource = searchParams.get('source') || 'all'

  const handleSourceChange = (key: string) => {
    const params = new URLSearchParams(searchParams.toString())
    if (key === 'all') {
      params.delete('source')
    } else {
      params.set('source', key)
    }
    router.push(`/?${params.toString()}`, { scroll: false })
  }

  return (
    <div className="flex gap-0 overflow-x-auto scrollbar-hide border-b border-gray-200 dark:border-[#1f1f1f]">
      {SOURCE_FILTERS.map((filter) => {
        const isActive = currentSource === filter.key
        return (
          <button
            key={filter.key}
            onClick={() => handleSourceChange(filter.key)}
            className={`
              flex-shrink-0 px-3 py-2 -mb-px text-xs font-mono
              border-b-2 transition-colors duration-100
              focus:outline-none
              ${isActive
                ? 'border-gray-900 dark:border-white text-gray-900 dark:text-white'
                : 'border-transparent text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }
            `}
          >
            {filter.label}
          </button>
        )
      })}
    </div>
  )
}
