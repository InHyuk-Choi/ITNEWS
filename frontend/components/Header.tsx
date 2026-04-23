'use client'

import { useState } from 'react'
import { ThemeToggle } from './ThemeToggle'
import { SearchBar } from './SearchBar'
import { SubscribeModal } from './SubscribeModal'

export function Header() {
  const [showSubscribe, setShowSubscribe] = useState(false)

  return (
    <>
      <header className="sticky top-0 z-50 w-full border-b border-gray-200 dark:border-[#1f1f1f] bg-white/95 dark:bg-[#0a0a0a]/95 backdrop-blur-sm">
        <div className="max-w-8xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 gap-4">

            {/* Logo */}
            <div className="flex items-center gap-2 shrink-0">
              <div className="flex items-center justify-center w-7 h-7 rounded-md bg-gray-900 dark:bg-white">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"
                  className="w-4 h-4 text-white dark:text-gray-900" aria-hidden="true">
                  <path fillRule="evenodd"
                    d="M14.615 1.595a.75.75 0 01.359.852L12.982 9.75h7.268a.75.75 0 01.548 1.262l-10.5 11.25a.75.75 0 01-1.272-.71l1.992-7.302H3.75a.75.75 0 01-.548-1.262l10.5-11.25a.75.75 0 01.913-.143z"
                    clipRule="evenodd" />
                </svg>
              </div>
              <span className="font-mono text-sm font-semibold text-gray-900 dark:text-white">
                it-news
              </span>
            </div>

            {/* Search */}
            <div className="flex-1 flex justify-center max-w-xl">
              <SearchBar />
            </div>

            {/* Right side */}
            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={() => setShowSubscribe(true)}
                className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 text-xs font-mono rounded-md border border-gray-200 dark:border-[#2a2a2a] text-gray-500 dark:text-gray-400 hover:border-gray-400 dark:hover:border-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors"
              >
                newsletter
              </button>
              <ThemeToggle />
            </div>
          </div>
        </div>
      </header>

      {showSubscribe && <SubscribeModal onClose={() => setShowSubscribe(false)} />}
    </>
  )
}
