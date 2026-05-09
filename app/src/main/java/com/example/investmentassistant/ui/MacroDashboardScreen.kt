package com.example.investmentassistant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.model.MacroData
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.TimeRange
import com.example.investmentassistant.viewmodel.MacroViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText

private enum class MacroTab(val title: String) {
    PULSE("Market Pulse"),
    LIQUIDITY("Liquidity"),
    RATES("Rates & Bonds"),
    RISK("Risk & Sentiment"),
    ASSETS("Assets"),
    COMMODITY("Commodities"),
    KOREA("Korea"),
    INSIGHT("⭐ Insights"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroDashboardScreen(
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: MacroViewModel = viewModel(),
) {
    var selectedTab by remember { mutableStateOf(MacroTab.PULSE) }
    val selectedRange by viewModel.selectedRange.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isLoading) "데이터 불러오는 중..." else "ApexInsight Macro Panel",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.fetchMacroData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                ) {
                    MacroTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { viewModel.updateRange(range) },
                        label = { Text(range.label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (selectedTab) {
                MacroTab.PULSE -> MarketPulseContent(indicators)
                MacroTab.LIQUIDITY -> LiquidityContent(indicators)
                MacroTab.RATES -> RatesContent(indicators)
                MacroTab.RISK -> RiskContent(indicators)
                MacroTab.ASSETS -> AssetsContent(indicators)
                MacroTab.COMMODITY -> CommodityContent(indicators)
                MacroTab.KOREA -> KoreaContent(indicators)
                MacroTab.INSIGHT -> InsightContent(viewModel)
            }
        }
    }
}

// ==========================================
// Tab content composables
// ==========================================

@Composable
private fun MarketPulseContent(ind: MacroIndicators) {
    val isSp500Up = ind.sp500.isRising()
    val isNasdaqUp = ind.nasdaq.isRising()
    val isYieldDown = ind.us10y.isFalling()
    val isDollarDown = ind.dollarIndex.isFalling()

    val bullScore = listOf(isSp500Up, isNasdaqUp, isYieldDown, isDollarDown).count { it }
    val (statusText, statusColor) = when {
        bullScore >= 3 -> "강력한 Risk ON (위험 선호)" to Color(0xFF4CAF50)
        bullScore == 2 -> "혼조세 (방향성 탐색 중)" to Color(0xFFFFA500)
        else -> "Risk OFF (안전 자산 선호)" to Color(0xFFFF4444)
    }

    StatusCard(statusText, statusColor, "주식, 금리, 달러 동향 종합 심리")
    GuideSection("미 국채 10년물 금리가 오르면 기술주/성장주에 악재로 작용합니다. 또한 달러 가치가 오르면(강달러) 외국인 자금이 미국으로 빠져나가 신흥국 증시와 가상자산에 부정적입니다.")
    InfoRow("S&P 500", ind.sp500.latestValue, "실시간", isSp500Up, ind.sp500.history)
    InfoRow("NASDAQ", ind.nasdaq.latestValue, "실시간", isNasdaqUp, ind.nasdaq.history)
    InfoRow("미 국채 10Y", ind.us10y.latestValue, "실시간", isYieldDown, ind.us10y.history)
    InfoRow("달러 인덱스", ind.dollarIndex.latestValue, "실시간 (DXY)", !isDollarDown, ind.dollarIndex.history)
}

@Composable
private fun LiquidityContent(ind: MacroIndicators) {
    val isFedUp = ind.fedBalance.isRising()
    val isM2Up = ind.m2.isRising()
    val isRealRateDown = ind.realRate.isFalling()

    val score = listOf(isFedUp, isM2Up, isRealRateDown).count { it }
    val (statusText, statusColor) = when {
        score >= 2 -> "유동성 확장 국면 (증시 호재)" to Color(0xFF4CAF50)
        score == 1 -> "유동성 중립 (관망세)" to Color(0xFFFFA500)
        else -> "유동성 축소 국면 (긴축)" to Color.Gray
    }

    StatusCard(statusText, statusColor, "연준 대차대조표 및 실질금리 종합")
    GuideSection("연준 자산과 M2는 '시중에 풀린 돈의 양'입니다. 줄어들면(긴축) 자산 시장의 상승 동력이 떨어집니다. 체감 금리인 '실질 금리'가 상승하면 자금이 주식에서 채권으로 이동합니다.")
    InfoRow("Fed 연준 자산", ind.fedBalance.latestValue, "실시간 데이터", isFedUp, ind.fedBalance.history)
    InfoRow("M2 통화량", ind.m2.latestValue, "실시간 데이터", isM2Up, ind.m2.history)
    InfoRow("실질 금리 (10Y)", ind.realRate.latestValue, "TIPS 기준 실시간", !isRealRateDown, ind.realRate.history)
}

@Composable
private fun RatesContent(ind: MacroIndicators) {
    val spreadVal = try {
        val y10 = ind.us10y.latestValue.replace("%", "").toFloat()
        val y2 = ind.us2y.latestValue.replace("%", "").toFloat()
        y10 - y2
    } catch (e: Exception) { 0f }

    val isReverted = spreadVal < 0
    val is10yRising = ind.us10y.isRising()
    val (statusText, statusColor) = when {
        isReverted -> "침체 경고 (장단기 금리 역전)" to Color(0xFFFF4444)
        is10yRising -> "장기 금리 상승기 (성장주 부담)" to Color(0xFFFFA500)
        else -> "정상적인 수익률 곡선 (안정적)" to Color(0xFF4CAF50)
    }

    StatusCard(statusText, statusColor, "수익률 곡선 및 통화정책 지표")
    GuideSection("단기 금리(2Y)가 장기 금리(10Y)보다 높아지는 '역전 현상(마이너스)'이 발생하면, 보통 1~2년 내에 경제에 큰 위기나 경기 침체가 온다는 강력한 역사적 시그널입니다.")
    InfoRow("미 국채 2Y", ind.us2y.latestValue, "실시간 데이터", !is10yRising, ind.us2y.history)
    InfoRow("미 국채 10Y", ind.us10y.latestValue, "실시간 데이터", !is10yRising, ind.us10y.history)
    val spreadHistory = if (ind.us10y.history.isNotEmpty() && ind.us2y.history.isNotEmpty()) {
        ind.us10y.history.zip(ind.us2y.history) { y10, y2 -> y10 - y2 }
    } else emptyList()
    InfoRow("10Y-2Y 스프레드", String.format("%.2fp", spreadVal), if (isReverted) "역전 상태" else "정상", !isReverted, spreadHistory)
}

@Composable
private fun RiskContent(ind: MacroIndicators) {
    val isVixHigh = (ind.vix.latestValue.toFloatOrNull() ?: 0f) > 20f
    val isHyRising = ind.hySpread.isRising()
    val isFear = (ind.fearGreed.latestValue.toFloatOrNull() ?: 50f) < 45f

    val riskScore = listOf(isVixHigh, isHyRising, isFear).count { it }
    val (statusText, statusColor) = when {
        riskScore >= 2 -> "시장 공포 및 변동성 확대" to Color(0xFFFF4444)
        riskScore == 1 -> "리스크 요인 부분 발생 (주의)" to Color(0xFFFFA500)
        else -> "안정적인 시장 심리 (위험 선호)" to Color(0xFF4CAF50)
    }

    StatusCard(statusText, statusColor, "VIX 및 기업 부도 위험 종합")
    GuideSection("VIX는 시장의 '공포'를 나타냅니다. 20이 넘으면 경계해야 합니다. 하이일드 스프레드가 치솟으면 한계 기업들의 연쇄 부도 위험이 커졌다는 뜻입니다.")
    InfoRow("VIX (공포지수)", ind.vix.latestValue, "실시간 데이터", !isVixHigh, ind.vix.history)
    InfoRow("하이일드 스프레드", ind.hySpread.latestValue, "실시간 (FRED)", !isHyRising, ind.hySpread.history)
    InfoRow("Fear & Greed", ind.fearGreed.latestValue, if (isFear) "공포 구간" else "중립/탐욕 구간", ind.fearGreed.isRising(), ind.fearGreed.history)
}

@Composable
private fun AssetsContent(ind: MacroIndicators) {
    val isBtcUp = ind.btc.isRising()
    val isGoldUp = ind.gold.isRising()
    val rsHistory = if (ind.sp500.history.isNotEmpty() && ind.nasdaq.history.isNotEmpty()) {
        ind.sp500.history.zip(ind.nasdaq.history) { sp, ndq -> if (sp != 0f) ndq / sp else 0f }
    } else emptyList()
    val isNasdaqOutperforming = rsHistory.size > 1 && rsHistory.last() > rsHistory.first()

    val (statusText, statusColor) = when {
        isNasdaqOutperforming && isBtcUp -> "위험 자산(기술/코인) 랠리" to Color(0xFF4CAF50)
        isGoldUp && !isBtcUp -> "안전 자산(금) 선호 심리" to Color(0xFFFFA500)
        else -> "자산별 차별화 장세" to Color.Gray
    }

    StatusCard(statusText, statusColor, "디지털 vs 전통 자산 흐름 종합")
    GuideSection("금은 인플레이션이나 위기 발생 시 오르는 전통적 방어 자산입니다. 나스닥 상대강도(Outperform)가 뜬다면, 증시 자금이 '빅테크' 위주로만 쏠리고 있다는 뜻입니다.")
    InfoRow("비트코인", "$${ind.btc.latestValue}", "실시간 (BTC-USD)", isBtcUp, ind.btc.history)
    InfoRow("금 (Gold)", "$${ind.gold.latestValue}", "실시간 선물 (GC=F)", isGoldUp, ind.gold.history)
    InfoRow("나스닥 상대강도", if (isNasdaqOutperforming) "Outperform" else "Underperform", if (isNasdaqOutperforming) "강세 (빅테크 주도)" else "약세", isNasdaqOutperforming, rsHistory)
}

@Composable
private fun CommodityContent(ind: MacroIndicators) {
    val isWtiUp = ind.wti.isRising()
    val isCopperUp = ind.copper.isRising()
    val (statusText, statusColor) = when {
        isWtiUp && isCopperUp -> "경기 회복 기대 및 인플레 압력" to Color(0xFFFF4444)
        !isWtiUp && !isCopperUp -> "물가 압력 완화 (안정화)" to Color(0xFF4CAF50)
        else -> "에너지/산업금속 혼조세" to Color(0xFFFFA500)
    }

    StatusCard(statusText, statusColor, "에너지 및 산업금속 동향 종합")
    GuideSection("WTI 유가가 오르면 물가가 다시 뛰어올라 연준이 금리를 내리기 힘들어집니다. 구리는 '닥터 코퍼'로 불리며, 가격 상승은 공장이 돌아가고 실물 경기가 살아나고 있다는 청신호입니다.")
    InfoRow("WTI 원유", "$${ind.wti.latestValue}", "실시간 선물 (CL=F)", isWtiUp, ind.wti.history)
    InfoRow("구리 (Copper)", "$${ind.copper.latestValue}", "실시간 선물 (HG=F)", isCopperUp, ind.copper.history)
}

@Composable
private fun KoreaContent(ind: MacroIndicators) {
    val isFxHigh = (ind.usdkrw.latestValue.replace(",", "").toFloatOrNull() ?: 0f) > 1350f
    val isKospiUp = ind.kospi.isRising()
    val (statusText, statusColor) = when {
        isFxHigh && !isKospiUp -> "국장 투자 매력도 하락 (환율/증시 이중고)" to Color(0xFFFF4444)
        !isFxHigh && isKospiUp -> "안정적 환율 및 증시 상승 기대" to Color(0xFF4CAF50)
        else -> "환율/증시 힘겨루기 중" to Color(0xFFFFA500)
    }

    StatusCard(statusText, statusColor, "코스피 및 환율 추이 종합")
    GuideSection("환율이 크게 오르면(원화 가치 하락), 외국인 투자자들은 가만히 있어도 손해(환차손)를 보기 때문에 한국 주식을 팔고 달러로 바꿔서 떠나는 핵심 원인이 됩니다.")
    InfoRow("KOSPI", ind.kospi.latestValue, "실시간 (^KS11)", isKospiUp, ind.kospi.history)
    InfoRow("원/달러 환율", "₩${ind.usdkrw.latestValue}", "실시간 (KRW=X)", !isFxHigh, ind.usdkrw.history)
}

@Composable
private fun InsightContent(viewModel: MacroViewModel = viewModel()) {
    val insightText by viewModel.aiInsight.collectAsState()
    val isInsightLoading by viewModel.isInsightLoading.collectAsState()
    val tokenUsage by viewModel.tokenUsage.collectAsState()

    val isInitialState = insightText.contains("시작하세요")
    val isAnalyzing = insightText.contains("분석 중")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧠", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("AI 매크로 리포트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (isInsightLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            if (isAnalyzing || isInitialState) {
                Text(text = insightText, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
            } else {
                RichText { Markdown(insightText) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isInitialState && !isAnalyzing) {
                Text(
                    text = tokenUsage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.generateAiInsight() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInsightLoading,
            ) {
                Text(if (isInitialState) "실시간 지표 분석하기" else "최신 데이터로 재분석")
            }
        }
    }
}

// ==========================================
// Shared UI components
// ==========================================

@Composable
fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    Canvas(modifier = modifier) {
        val max = data.max()
        val min = data.min()
        val range = if (max == min) 1f else max - min
        val stepX = size.width / (data.size - 1)
        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value - min) / range) * size.height
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun StatusCard(status: String, color: Color, summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = status, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    subValue: String,
    isPositive: Boolean,
    history: List<Float> = emptyList(),
) {
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF4444)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, modifier = Modifier.width(100.dp), fontSize = 13.sp)
        if (history.isNotEmpty()) {
            Sparkline(
                data = history,
                color = trendColor,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 8.dp),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(80.dp)) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = subValue, color = trendColor, fontSize = 11.sp)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun GuideSection(guideText: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text("💡", modifier = Modifier.padding(end = 8.dp))
            Text(
                text = guideText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

private fun MacroData.isRising(): Boolean = history.size > 1 && history.last() > history[history.size - 2]
private fun MacroData.isFalling(): Boolean = history.size > 1 && history.last() < history[history.size - 2]
