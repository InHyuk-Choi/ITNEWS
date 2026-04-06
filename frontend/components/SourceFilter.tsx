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
    <div className="relative">
      <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
        {SOURCE_FILTERS.map((filter) => {
          const isActive = currentSource === filter.key
          return (
            <button
              key={filter.key}
              onClick={() => handleSourceChange(filter.key)}
              className={`
                flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium
                transition-all duration-200 ease-in-out
                focus:outline-none focus:ring-2 focus:ring-orange-500/50
                ${
                  isActive
                    ? 'bg-gray-900 text-white dark:bg-white dark:text-gray-900 shadow-md'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-[#1a1a1a] dark:text-gray-400 dark:hover:bg-[#2a2a2a] dark:border dark:border-[#2a2a2a]'
                }
              `}
            >
              {filter.label}
            </button>
          )
        })}
      </div>

      {/* Fade gradient on right edge */}
      <div className="absolute right-0 top-0 bottom-0 w-8 bg-gradient-to-l from-white dark:from-[#0a0a0a] to-transparent pointer-events-none" />
    </div>
  )
}
