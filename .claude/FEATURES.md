# ApexInsight — Feature Catalog

Last updated: 2026-05-09 (기능 백로그 추가)

## Screens

| Screen | Route | Description |
|--------|-------|-------------|
| MacroDashboardScreen | `dashboard` | 8-tab macro panel + AI insights |
| NewsSearchScreen | `news` | Keyword search → AI investment report |
| SavedReportsScreen | `archive` | Report archive + daily token usage |

Navigation: **Bottom Navigation Bar** (Dashboard / 뉴스 / 보관함)

---

## MacroDashboard Tabs

| Tab | Indicators | Sources |
|-----|-----------|---------|
| Market Pulse | S&P 500, NASDAQ, US 10Y Yield, DXY | Yahoo Finance + FRED |
| Liquidity | Fed Balance Sheet, M2, Real Rate | FRED |
| Rates & Bonds | 2Y/10Y yields, yield curve spread | FRED |
| Risk & Sentiment | VIX, HY Spread, Fear & Greed Index | Yahoo + Alternative.me |
| Assets | BTC, Gold, Nasdaq Relative Strength | Yahoo Finance |
| Commodities | WTI Oil, Copper | Yahoo Finance |
| Korea | KOSPI, USD/KRW | Yahoo Finance |
| ⭐ Insights | AI macro report (Gemini 2.5 Flash) | AI + all indicators |

Time range selector: 1W / 1M / 3M / 6M / 1Y (changes FRED limit + Yahoo range/interval)

---

## AI Report Generation

- **News Report**: User enters keyword → NewsAPI articles → Gemini synthesizes 3-section report
  - 📊 핵심 동향 (key trends)
  - 💰 경제/산업 파급 효과 (economic impact)
  - 🚀 향후 향방 및 투자 시사점 (investment outlook)
  - Source citations as markdown hyperlinks `[[1]](url)`

- **Macro Insight**: User taps analyze button on Insights tab → Gemini analyzes 13 live indicators
  - 📊 글로벌 매크로 상황 요약
  - 🎯 자산 시장 단기 방향성 전망
  - ⚠️ 핵심 리스크 요인

- **Auto-save**: All generated reports saved to Room DB (duplicate check on content)
- **Token tracking**: Daily Gemini token usage cumulated in `token_records` table

---

## External APIs

| API | Base URL | Auth | Notes |
|-----|----------|------|-------|
| NewsAPI | newsapi.org/v2 | `NEWS_API_KEY` in local.properties | 100 req/day (free plan) |
| FRED | api.stlouisfed.org/fred | `FRED_API_KEY` in local.properties | Macro time series |
| Yahoo Finance | query1.finance.yahoo.com/v8/finance | None | Stock/asset prices |
| Fear & Greed | api.alternative.me/fng | None | Crypto sentiment 0–100 |
| Google Gemini | via SDK | `GEMINI_API_KEY` in local.properties | Model: gemini-2.5-flash |

---

## Room Database

| Table | PK | Key Fields |
|-------|----|-----------|
| `saved_reports` | `id` (autoincrement) | `type` (NEWS/MACRO), `title`, `content`, `savedAt` (ms) |
| `token_records` | `date` (yyyy-MM-dd) | `totalTokens` — daily cumulative |

---

## Architecture

- **Single-module** Android app (Kotlin + Jetpack Compose)
- **Layers**: UI → ViewModel (AndroidViewModel) → Repository → (Network / Room)
- **Repositories**: `MacroRepository`, `NewsRepository`, `AiRepository`, `ReportRepository`
- **No Context in ViewModel public API** — Application context injected at construction
- **Reactive**: Kotlin Flow + StateFlow for all live data
- **DI**: Manual singleton pattern (no Hilt yet)

---

## Planned / Not Yet Built

- Brokerage integration (Mirae Asset portfolio sync)
- Portfolio-specific daily briefings
- Market radar (hot options / volatility tracking)
- Individual company valuation analysis
- Buy / Sell / Hold recommendation engine
- Claude API option instead of Gemini

---

## Ideas Backlog

<!-- Add new feature ideas here -->

---

## 기능 개발 백로그 (우선순위 순)

> 상태: ⬜ 미착수 / 🔄 진행중 / ✅ 완료

### 1. 뉴스 자동 리포트 (주기적 생성)
**상태:** ⬜ 미착수

사용자가 관심 키워드를 등록하면, 앱이 일정 주기(예: 매일 아침 8시)마다 자동으로 뉴스를 수집하고 AI 리포트를 생성하여 보관함에 저장.

- 키워드 등록/관리 화면
- WorkManager로 백그라운드 주기 실행
- 생성된 리포트 보관함 자동 저장

#### 1-2. 리포트 급변 감지 알림
**상태:** ⬜ 미착수 (1번 완료 후 진행)

새로 생성된 리포트의 내용이 직전 리포트 대비 크게 달라진 경우 (예: AI가 리스크 심각도를 높게 판단) 푸시 알림 발송.

- 직전 리포트와 신규 리포트를 AI로 비교
- 변화량이 임계값 이상이면 알림

---

### 2. 지표 급변 감지 알림
**상태:** ⬜ 미착수

주요 거시지표(VIX, Fear & Greed, 환율 등)를 주기적으로 체크하여, 단기간에 급격히 변동할 경우 푸시 알림.

- WorkManager로 지표 주기 수집
- 직전값 대비 변화율 임계값 설정 (예: VIX +20% 이상)
- 알림에 어떤 지표가 얼마나 변했는지 표시

---

### 3. 투자 포인트 발생 알림
**상태:** ⬜ 미착수 (2번 완료 후 진행)

AI가 지표 데이터를 분석하여 "매수/매도 고려 시점"이라고 판단할 경우 알림.

- 지표 수집 후 Gemini에 투자 포인트 여부 질의
- AI 응답이 특정 신호를 포함할 경우 알림 발송
- 알림 탭 → 해당 AI 리포트 바로 열기
