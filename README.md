# 📰 IT뉴스

> 개발자를 위한 IT 뉴스 애그리게이터 — 6개 소스에서 자동 수집하고 AI가 요약해주는 뉴스 플랫폼

![Next.js](https://img.shields.io/badge/Next.js-14-black?style=flat-square&logo=next.js)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=flat-square&logo=railway)

## ✨ 주요 기능

- **자동 크롤링** — 6개 소스에서 1시간마다 최신 뉴스 수집
- **AI 요약** — Gemini 2.5 Flash가 기사를 한국어로 요약
- **실시간 검색** — 제목/요약 기준 키워드 검색 (ILIKE)
- **주간 뉴스레터** — 매주 월요일 Groq AI가 선별한 TOP 10 기사를 이메일로 발송
- **뉴스레터 구독/해지** — 이메일 구독 신청 및 원클릭 수신거부
- **소스 필터** — 원하는 뉴스 소스만 선택
- **무한 스크롤** — 부드러운 페이지네이션
- **다크/라이트 모드** — 토글 지원

## 📡 뉴스 소스

| 소스 | 분류 |
|------|------|
| Hacker News | 글로벌 개발자 커뮤니티 |
| TechCrunch | 글로벌 IT 미디어 |
| VentureBeat | 글로벌 IT 미디어 |
| 전자신문 | 국내 IT 전문 |
| AI타임스 | 국내 AI 전문 |
| 네이버 뉴스 | 국내 IT 전반 |

## 🛠 기술 스택

### Frontend
- **Next.js 14** (App Router, ISR)
- **TypeScript**
- **Tailwind CSS**
- **SWR** (무한 스크롤)

### Backend
- **Spring Boot 3.5** (Java 25)
- **Spring Scheduler** (크롤링 자동화)
- **Jsoup + Rome** (HTML/RSS 파싱)
- **Groq API** (llama-3.1-8b-instant) — 뉴스레터 기사 선별
- **Gemini 2.5 Flash** — 기사 본문 요약
- **Resend** — 뉴스레터 이메일 발송
- **Bucket4j** (Rate Limiting)
- **Caffeine Cache**

### Infra
- **PostgreSQL 16**
- **Docker Compose** (로컬)
- **Railway** (배포)

## 🚀 로컬 실행

### 사전 준비

- Docker Desktop
- Java 25
- Node.js 20+

### API Key 발급

| 서비스 | 발급 경로 |
|--------|-----------|
| Gemini API | [aistudio.google.com](https://aistudio.google.com) |
| Groq API | [console.groq.com](https://console.groq.com) |
| Naver 검색 API | [developers.naver.com](https://developers.naver.com) |
| Resend | [resend.com](https://resend.com) |

### 실행

```bash
# 1. 레포 클론
git clone https://github.com/your-username/it-news-aggregator.git
cd it-news-aggregator

# 2. 환경변수 설정
cp .env.example .env
# .env 파일에 API 키 입력

# 3. 백엔드 빌드
cd backend && ./gradlew bootJar -x test && cd ..

# 4. 프론트엔드 빌드
cd frontend && npm install && npm run build && cd ..

# 5. 전체 실행
docker-compose up -d
```

브라우저에서 `http://localhost:3000` 접속

### 관리자 엔드포인트 (로컬 전용)

```bash
# 수동 크롤링
curl -X POST http://localhost:8080/api/admin/crawl

# 뉴스레터 즉시 발송
curl -X POST http://localhost:8080/api/admin/newsletter
```

Railway 등 외부에서 호출할 경우 `X-Admin-Secret` 헤더 필요:
```bash
curl -X POST https://your-backend.railway.app/api/admin/newsletter \
  -H "X-Admin-Secret: your-admin-secret"
```

## 📁 프로젝트 구조

```
├── backend/                  # Spring Boot
│   └── src/main/java/com/itnews/backend/
│       ├── crawler/          # 크롤러 (HackerNews, RSS, Naver)
│       ├── news/             # News Entity, Service, Controller
│       ├── subscriber/       # 구독자 관리, 뉴스레터 발송
│       ├── ai/               # Gemini 요약 서비스
│       └── config/           # Security, Cache, Rate Limit
│
├── frontend/                 # Next.js
│   ├── app/                  # App Router
│   │   └── unsubscribe/      # 수신거부 페이지
│   ├── components/           # UI 컴포넌트
│   └── lib/                  # API 클라이언트
│
└── docker-compose.yml
```

## 🔐 보안

- IP별 Rate Limiting (60 req/min)
- CORS 도메인 제한
- Security Headers (CSP, X-Frame-Options 등)
- Admin 엔드포인트: localhost 또는 `X-Admin-Secret` 헤더 인증

## 📄 환경변수

```env
# 백엔드
GEMINI_API_KEY=           # Google AI Studio
GROQ_API_KEY=             # Groq Cloud
NAVER_CLIENT_ID=          # Naver Developers
NAVER_CLIENT_SECRET=      # Naver Developers
RESEND_API_KEY=           # Resend 이메일 발송
ADMIN_SECRET=             # Admin 엔드포인트 인증 키
ALLOWED_ORIGIN=           # 프론트엔드 URL (CORS)

# 프론트엔드
NEXT_PUBLIC_API_URL=      # 백엔드 URL
```