import type { MetadataRoute } from 'next'
import { SOURCE_FILTERS } from '@/lib/sources'

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || 'https://grand-youth-production-d2b9.up.railway.app'

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date()

  const sourceRoutes: MetadataRoute.Sitemap = SOURCE_FILTERS
    .filter((f) => f.key !== 'all')
    .map((f) => ({
      url: `${SITE_URL}/?source=${f.key}`,
      lastModified: now,
      changeFrequency: 'hourly' as const,
      priority: 0.7,
    }))

  return [
    {
      url: SITE_URL,
      lastModified: now,
      changeFrequency: 'hourly',
      priority: 1.0,
    },
    ...sourceRoutes,
  ]
}
