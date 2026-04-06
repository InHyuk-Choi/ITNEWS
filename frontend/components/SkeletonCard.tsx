export function SkeletonCard() {
  return (
    <div className="rounded-2xl overflow-hidden bg-white dark:bg-[#1a1a1a] border border-gray-100 dark:border-[#2a2a2a] shadow-sm">
      {/* Thumbnail skeleton */}
      <div className="h-48 bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />

      <div className="p-5 space-y-3">
        {/* Badge + time row */}
        <div className="flex items-center justify-between">
          <div className="h-5 w-20 rounded-full bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
          <div className="h-4 w-16 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        </div>

        {/* Title skeleton - 2 lines */}
        <div className="space-y-2">
          <div className="h-5 w-full rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
          <div className="h-5 w-4/5 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        </div>

        {/* Summary skeleton - 3 lines */}
        <div className="space-y-1.5 pt-1">
          <div className="h-3.5 w-full rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
          <div className="h-3.5 w-full rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
          <div className="h-3.5 w-3/4 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        </div>

        {/* Footer skeleton */}
        <div className="pt-2 border-t border-gray-100 dark:border-[#2a2a2a] flex justify-end">
          <div className="h-4 w-24 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        </div>
      </div>
    </div>
  )
}
