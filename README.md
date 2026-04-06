# 📰 IT 뉴스 애그리게이터

> 여러 IT 뉴스 소스를 자동으로 수집하고, AI가 요약해주는 뉴스 플랫폼

![Next.js](https://img.shields.io/badge/Next.js-14-black?style=flat-square&logo=next.js)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)

## ✨ 주요 기능

- **자동 크롤링** — 6개 소스에서 1시간마다 최신 뉴스 수집
- **AI 요약** — Gemini 2.5 Flash가 기사 본문을 읽고 한국어로 요약
- **다크/라이트 모드** — 토글 지원
- **무한 스크롤** — 부드러운 페이지네이션
- **소스 필터** — 원하는 뉴스 소스만 선택
- **카드 클릭 → 모달** — 요약 전체 보기 후 원문 이동

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
- **Framer Motion**
- **SWR** (무한 스크롤)

### Backend
- **Spring Boot 3.5** (Java 25)
- **Spring Scheduler** (크롤링 자동화)
- **Jsoup + Rome** (HTML/RSS 파싱)
- **Bucket4j** (Rate Limiting)
- **Caffeine Cache**

### Infra
- **PostgreSQL 16**
- **Docker Compose**
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
| Naver 검색 API | [developers.naver.com](https://developers.naver.com) |

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

### 수동 크롤링 트리거 (테스트용)

```bash
docker exec it-backend-1 curl -X POST http://localhost:8080/api/admin/crawl
```

## 📁 프로젝트 구조

```
├── backend/                  # Spring Boot
│   └── src/main/java/com/itnews/backend/
│       ├── crawler/          # 크롤러 (HackerNews, RSS, Naver)
│       ├── news/             # News Entity, Service, Controller
│       ├── ai/               # Gemini 요약 서비스
│       └── config/           # Security, Cache, Rate Limit
│
├── frontend/                 # Next.js
│   ├── app/                  # App Router
│   ├── components/           # UI 컴포넌트
│   └── lib/                  # API 클라이언트, 유틸
│
├── docker-compose.yml
└── .env.example
```

## 🔐 보안

- IP별 Rate Limiting (60 req/min)
- CORS 도메인 제한
- Security Headers (CSP, X-Frame-Options 등)
- Admin 엔드포인트 localhost 전용

## 📄 환경변수

```env
GEMINI_API_KEY=           # Google AI Studio API Key
NAVER_CLIENT_ID=          # Naver Developers Client ID
NAVER_CLIENT_SECRET=      # Naver Developers Client Secret
```
