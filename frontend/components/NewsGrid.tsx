'use client'

import { useEffect, useRef, useCallback, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import useSWR from 'swr'
import useSWRInfinite from 'swr/infinite'
import { motion, AnimatePresence } from 'framer-motion'
import { fetchNews, searchNews, NewsResponse, NewsItem } from '@/lib/api'
import { NewsCard } from './NewsCard'
import { NewsModal } from './NewsModal'
import { SkeletonCard } from './SkeletonCard'

interface NewsGridProps {
  initialData: NewsResponse
  source?: string
}

const PAGE_SIZE = 20

function getKey(
  pageIndex: number,
  previousPageData: NewsResponse | null,
  source: string
) {
  // Stop fetching if last page is reached
  if (previousPageData && pageIndex >= previousPageData.totalPages) return null

  return `news-${source}-${pageIndex}`
}

export function NewsGrid({ initialData }: NewsGridProps) {
  const searchParams = useSearchParams()
  const currentSource = searchParams.get('source') || 'all'
  const query = searchParams.get('q') || ''
  const loaderRef = useRef<HTMLDivElement>(null)
  const [selectedItem, setSelectedItem] = useState<NewsItem | null>(null)

  // 검색 모드
  const { data: searchData, isLoading: searchLoading } = useSWR(
    query ? `search-${query}` : null,
    () => searchNews(query),
    { revalidateOnFocus: false, dedupingInterval: 30000 }
  )

  // 일반 모드 (무한스크롤)
  const { data, size, setSize, isLoading, isValidating } = useSWRInfinite(
    (pageIndex, previousPageData) =>
      query ? null : getKey(pageIndex, previousPageData as NewsResponse | null, currentSource),
    async (key) => {
      const pageIndex = parseInt(key.split('-').pop() || '0', 10)
      const sourceParam = currentSource === 'all' ? undefined : currentSource
      return fetchNews(sourceParam, pageIndex, PAGE_SIZE)
    },
    {
      fallbackData: [initialData],
      revalidateFirstPage: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: true,
      dedupingInterval: 60000,
    }
  )

  // 검색 모드 vs 일반 모드
  const allItems = query
    ? (searchData?.content ?? [])
    : (data ? data.flatMap((page) => page.content) : [])
  const lastPage = data?.[data.length - 1]
  const hasMore = !query && (lastPage ? size < lastPage.totalPages : false)
  const isCurrentlyLoading = query ? searchLoading : isLoading
  const isEmpty = allItems.length === 0 && !isCurrentlyLoading

  // Intersection Observer for infinite scroll
  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const target = entries[0]
      if (target.isIntersecting && hasMore && !isValidating) {
        setSize((prev) => prev + 1)
      }
    },
    [hasMore, isValidating, setSize]
  )

  useEffect(() => {
    const observer = new IntersectionObserver(handleObserver, {
      root: null,
      rootMargin: '200px',
      threshold: 0,
    })
    const el = loaderRef.current
    if (el) observer.observe(el)
    return () => {
      if (el) observer.unobserve(el)
    }
  }, [handleObserver])

  if (isEmpty) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col items-center justify-center py-32 text-center"
      >
        <div className="w-16 h-16 rounded-2xl bg-gray-100 dark:bg-[#1a1a1a] flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            className="w-8 h-8 text-gray-400 dark:text-gray-600">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
              d={query
                ? "M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"
                : "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
              }
            />
          </svg>
        </div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
          {query ? `"${query}" 검색 결과 없음` : '뉴스가 없습니다'}
        </h3>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {query ? '다른 키워드로 검색해보세요.' : '현재 이 소스에서 가져올 뉴스가 없습니다.'}
        </p>
      </motion.div>
    )
  }

  return (
    <div>
      <NewsModal item={selectedItem} onClose={() => setSelectedItem(null)} />
      <AnimatePresence mode="wait">
        <motion.div
          key={currentSource}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
        >
          {/* Results count */}
          {query
            ? searchData && searchData.totalElements > 0 && (
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                <span className="font-medium text-gray-900 dark:text-white">&ldquo;{query}&rdquo;</span> 검색 결과{' '}
                <span className="font-medium text-gray-900 dark:text-white">{searchData.totalElements.toLocaleString()}</span>개
              </p>
            )
            : lastPage && lastPage.totalElements > 0 && (
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                총{' '}
                <span className="font-medium text-gray-900 dark:text-white">
                  {lastPage.totalElements.toLocaleString()}
                </span>
                개의 뉴스
              </p>
            )
          }

          {/* News list */}
          <div className="border border-gray-200 dark:border-[#1f1f1f] rounded-lg overflow-hidden divide-y divide-gray-100 dark:divide-[#1a1a1a]">
            {allItems.map((item, index) => (
              <NewsCard
                key={`${item.id}-${item.source}`}
                item={item}
                index={index % PAGE_SIZE}
                onClick={setSelectedItem}
              />
            ))}

            {/* Loading skeletons */}
            {(isCurrentlyLoading || isValidating) &&
              Array.from({ length: 6 }).map((_, i) => (
                <div key={`skeleton-${i}`} className="divide-y divide-gray-100 dark:divide-[#1a1a1a]">
                  <SkeletonCard />
                </div>
              ))}
          </div>
        </motion.div>
      </AnimatePresence>

      {/* Infinite scroll trigger */}
      <div ref={loaderRef} className="h-10 mt-6" />

      {/* End of feed */}
      {!hasMore && allItems.length > 0 && !isValidating && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-8"
        >
          <p className="text-sm text-gray-400 dark:text-gray-600">
            모든 뉴스를 불러왔습니다
          </p>
        </motion.div>
      )}
    </div>
  )
}
