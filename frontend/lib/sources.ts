export interface SourceMeta {
  label: string
  color: string
  bg: string
  textColor: string
}

export const SOURCES: Record<string, SourceMeta> = {
  hackernews: {
    label: 'Hacker News',
    color: 'orange',
    bg: '#ff6600',
    textColor: '#ffffff',
  },
  techcrunch: {
    label: 'TechCrunch',
    color: 'green',
    bg: '#0a7c3e',
    textColor: '#ffffff',
  },
  venturebeat: {
    label: 'VentureBeat',
    color: 'blue',
    bg: '#1a56db',
    textColor: '#ffffff',
  },
  etnews: {
    label: '전자신문',
    color: 'red',
    bg: '#cc2200',
    textColor: '#ffffff',
  },
  aitimes: {
    label: 'AI타임스',
    color: 'purple',
    bg: '#7c3aed',
    textColor: '#ffffff',
  },
  naver: {
    label: '네이버',
    color: 'green',
    bg: '#03c75a',
    textColor: '#ffffff',
  },
}

export const SOURCE_FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'hackernews', label: 'Hacker News' },
  { key: 'techcrunch', label: 'TechCrunch' },
  { key: 'venturebeat', label: 'VentureBeat' },
  { key: 'etnews', label: '전자신문' },
  { key: 'aitimes', label: 'AI타임스' },
  { key: 'naver', label: '네이버' },
]

export function getSourceMeta(source: string): SourceMeta {
  const normalized = source.toLowerCase().replace(/\s+/g, '-')
  return (
    SOURCES[normalized] ||
    SOURCES[source] || {
      label: source,
      color: 'gray',
      bg: '#666666',
      textColor: '#ffffff',
    }
  )
}
