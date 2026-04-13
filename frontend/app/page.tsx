import { Suspense } from 'react'
import { fetchNews } from '@/lib/api'
import { Header } from '@/components/Header'
import { SourceFilter } from '@/components/SourceFilter'
import { NewsGrid } from '@/components/NewsGrid'
import { SubscribeForm } from '@/components/SubscribeForm'

export const revalidate = 3600

interface PageProps {
  searchParams: { source?: string }
}

export default async function Home({ searchParams }: PageProps) {
  const source = searchParams.source || 'all'
  const initialData = await fetchNews(source === 'all' ? undefined : source, 0, 20)

  return (
    <div className="min-h-screen bg-white dark:bg-[#0a0a0a]">
      <Header />

      <main className="max-w-8xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 pb-16">
        <div className="mb-6">
          <Suspense fallback={null}>
            <SourceFilter />
          </Suspense>
        </div>

        <Suspense
          fallback={
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <div
                  key={i}
                  className="h-64 rounded-2xl bg-gray-100 dark:bg-[#1a1a1a] animate-pulse"
                />
              ))}
            </div>
          }
        >
          <NewsGrid initialData={initialData} source={source} />
        </Suspense>
        <SubscribeForm />
      </main>
    </div>
  )
}
