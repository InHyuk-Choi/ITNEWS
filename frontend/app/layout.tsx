import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { ThemeProvider } from 'next-themes'
import './globals.css'

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
})

export const metadata: Metadata = {
  title: 'IT뉴스 - 개발자를 위한 뉴스 애그리게이터',
  description:
    'Hacker News, GeekNews, ZDNet Korea, 블로터, TechCrunch, 네이버 등 주요 IT 뉴스를 한 곳에서',
  keywords: ['IT뉴스', '개발자', '기술', 'Hacker News', 'GeekNews', '테크뉴스'],
  openGraph: {
    title: 'IT뉴스 - 개발자를 위한 뉴스 애그리게이터',
    description: '주요 IT 뉴스를 한 곳에서',
    type: 'website',
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body className={`${inter.variable} font-sans antialiased`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem={false}
          disableTransitionOnChange={false}
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  )
}
