# Development Plan: Investment Assistant App

## Objective
Build the Android application defined in `README.md` iteratively. The system relies heavily on external APIs (Finance, News, Brokerage) and LLM processing. Development must be phased to ensure data accuracy before building complex UI.

---

## Phase 1: Project Initialization & UI Skeleton
**Goal:** Set up the Android environment and create the navigation structure. Mock data will be used to build the UI components.
* **Step 1:** Initialize an Android Studio project using Kotlin and Jetpack Compose.
* **Step 2:** Set up basic navigation with a bottom bar or side drawer. Core screens:
  * `Dashboard` (Daily Briefing, Portfolio, Buy/Sell Signals)
  * `Search/Analysis` (Keyword search, Asset Valuation)
  * `Market Radar` (Hot Options, Volatility)
  * `Hypothesis Engine` (Idea input to Asset suggestions)
* **Step 3:** Build static UI components using mock data (e.g., hardcoded news strings, fake stock tickers).

## Phase 2: Data Aggregation & Backend Setup
**Goal:** Establish the pipelines to pull raw financial and news data. (This can be done via a lightweight Python backend or direct API calls from the Android app, depending on architecture choice).
* **Step 1:** Implement News API integration (e.g., Google News RSS, NewsAPI) based on keyword search.
* **Step 2:** Implement Financial Data API (e.g., Yahoo Finance, Alpha Vantage, or FRED for macro data like 미국금리).
  * Create functions to fetch: Current price, PE ratio, Sales growth (재무지표).
* **Step 3:** Implement Market Radar logic to query top daily gainers/losers.

## Phase 3: AI & LLM Integration (The Brain)
**Goal:** Connect an LLM (e.g., Gemini, Claude, or OpenAI) to process the raw data fetched in Phase 2.
* **Step 1:** Create prompt templates for **News Summarization** (input: array of articles -> output: bulleted summary with source links).
* **Step 2:** Create the **Valuation Engine** logic. Feed financial metrics and recent news to the LLM to generate an "Overvalued/Undervalued" assessment.
* **Step 3:** Build the **Macro Hypothesis Engine**. Create a system prompt that takes user natural language (e.g., "Economy is falling") and outputs specific asset classes and tickers.

## Phase 4: Brokerage Integration (Mirae Asset)
**Goal:** Connect the user's actual portfolio to the app.
* **Step 1:** Register for Mirae Asset Open API access and secure API keys.
* **Step 2:** Implement OAuth or secure login flow for the brokerage.
* **Step 3:** Fetch current holdings and map them to the Dashboard.
* **Step 4:** Cross-reference current holdings with the AI Valuation Engine (from Phase 3) to generate daily "Buy/Hold/Sell" alerts.

## Phase 5: Polish & Testing
**Goal:** Ensure the app is stable, secure, and ready for daily use.
* **Step 1:** Implement caching (e.g., Room Database) so the app doesn't hit API limits on every refresh.
* **Step 2:** Refine error handling (e.g., what happens if the Mirae Asset API is down during trading hours?).
* **Step 3:** Finalize UI/UX polish, ensuring charts and data tables are readable on mobile.