export function SkeletonCard() {
  return (
    <div className="px-4 py-3.5 flex items-start gap-3">
      <div className="shrink-0 w-[3px] self-stretch rounded-full bg-gray-200 dark:bg-[#2a2a2a] animate-pulse" />
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2">
          <div className="h-2.5 w-14 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
          <div className="h-2.5 w-10 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        </div>
        <div className="h-4 w-full rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
        <div className="h-3 w-3/4 rounded bg-gray-100 dark:bg-[#2a2a2a] animate-pulse" />
      </div>
    </div>
  )
}
