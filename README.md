# Project: [Insert Project Name]

## Overview
A comprehensive, AI-powered Android application designed to serve as a personal investment assistant. The app aggregates financial news, evaluates asset valuations across different asset classes, provides daily portfolio-specific briefings, and identifies rapidly shifting market trends.

## Core Features

### 1. Intelligent News & Opinion Aggregator
* **Keyword Search:** Users can input specific keywords to pull recent, relevant news.
* **Opinion Summarization:** Synthesizes various viewpoints from multiple sources into a single, cohesive summary.
* **Source Tracking:** Displays all referenced sources with clickable links for deep diving.

### 2. Deep Asset Valuation & Analysis
Evaluates whether a selected asset (Company, Gold, Crypto, etc.) is overvalued or undervalued based on specific criteria.
* **Equities (Companies):** * Collects and analyzes core financial indicators (재무지표).
  * Tracks sales revenue, profit margins, and year-over-year growth.
* **Commodities (e.g., Gold) & Macro:**
  * Tracks geopolitical events impacting the asset.
  * Monitors recent price actions.
  * Integrates key macroeconomic indices such as US Interest Rates (미국금리) and the Consumer Price Index (소비자물가지표).

### 3. Daily Portfolio Dashboard & Brokerage Integration
* **MTS Integration:** Connects with Mirae Asset (미래에셋) to track the user's current holdings.
* **Daily Briefing:** Upon app launch, presents a curated feed of news specifically related to the user's invested assets.
* **Actionable Signals:** Provides explicit "Buy" or "Sell" recommendations based on the aggregated data and AI analysis of the portfolio.

### 4. Market Trend Radar ("Hot Options")
* **Volatility Tracking:** Identifies assets that are rising or falling rapidly in the current market session.
* **"Why is this happening?":** Automatically generates an AI explanation for the sudden price movement based on breaking news or unusual volume.

### 5. Macro Hypothesis Engine (Scenario-Based Investing)
* **Natural Language Theses:** Users can input broad macroeconomic or sector-specific insights (e.g., "The US housing market is going to cool down" or "AI infrastructure will require massive energy grid upgrades").
* **Financial Translation Logic:** The AI processes the natural language insight and maps it to specific financial mechanics:
  * Identifies sectors that benefit from or are harmed by the scenario.
  * Suggests long positions (Buy) for beneficiaries and short/inverse positions (or "Avoid" warnings) for negatively impacted sectors.
* **Actionable Asset Discovery:** Outputs a curated list of specific, tradable instruments (Individual Stocks, ETFs, Inverse ETFs, Commodities) that align with the thesis, ready to be viewed alongside the existing Mirae Asset portfolio data.
* **Risk Context:** Briefly explains *why* the suggested assets correlate with the user's thesis (e.g., "Suggesting $TLT because long-term treasuries typically rise when economic growth slows").

## Proposed Tech Stack (Draft)
* **Frontend:** Android (Kotlin / Jetpack Compose)
* **Backend/Data Aggregation:** Python (BeautifulSoup/Scrapy for news, Financial APIs for stock/macro data)
* **AI/NLP:** LLM API (for summarization, opinion synthesis, and buy/sell reasoning)
* **Brokerage API:** Mirae Asset Open API (for portfolio syncing)

## Setup & Execution (For AI Agent)
1. Initialize the Android Studio project structure.
2. Set up the basic UI navigation (Dashboard, Search/News, Asset Analysis, Market Trends).
3. Draft the API service interfaces for data fetching before implementing the complex scraping logic.