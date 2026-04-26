package com.example.investmentassistant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.api.MacroData
import com.example.investmentassistant.viewmodel.MacroViewModel
import com.example.investmentassistant.viewmodel.TimeRange

enum class MacroTab(val title: String) {
    PULSE("Market Pulse"),
    LIQUIDITY("Liquidity"),
    RATES("Rates & Bonds"),
    RISK("Risk & Sentiment"),
    ASSETS("Assets"),
    COMMODITY("Commodities"),
    KOREA("Korea"),
    INSIGHT("⭐ Insights")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroDashboardScreen(
    viewModel: MacroViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(MacroTab.PULSE) }
    val selectedRange by viewModel.selectedRange.collectAsState()

    // ★ String 대신 최신값과 히스토리가 포함된 데이터를 관찰합니다 ★
    val us10yData by viewModel.us10yData.collectAsState()
    val us2yData by viewModel.us2yData.collectAsState()
    val fedBalanceData by viewModel.fedBalanceData.collectAsState()
    val m2Data by viewModel.m2Data.collectAsState()
    val realRateData by viewModel.realRateData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sp500Data by viewModel.sp500Data.collectAsState()
    val nasdaqData by viewModel.nasdaqData.collectAsState()
    val dollarIndexData by viewModel.dollarIndexData.collectAsState()
    val vixData by viewModel.vixData.collectAsState()
    val hySpreadData by viewModel.hySpreadData.collectAsState()
    val fearGreedData by viewModel.fearGreedData.collectAsState()
    val btcData by viewModel.btcData.collectAsState()
    val goldData by viewModel.goldData.collectAsState()
    val wtiData by viewModel.wtiData.collectAsState()
    val copperData by viewModel.copperData.collectAsState()
    val kospiData by viewModel.kospiData.collectAsState()
    val usdkrwData by viewModel.usdkrwData.collectAsState()
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isLoading) "데이터 불러오는 중..." else "ApexInsight Macro Panel",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    MacroTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeRange.values().forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { viewModel.updateRange(range) },
                        label = { Text(range.label, fontSize = 12.sp) }
                    )
                }
            }
            when (selectedTab) {
                MacroTab.PULSE -> MarketPulseContent(us10yData, sp500Data, nasdaqData, dollarIndexData)
                MacroTab.LIQUIDITY -> LiquidityContent(fedBalanceData, m2Data, realRateData)
                MacroTab.RATES -> RatesContent(us10yData, us2yData)
                MacroTab.RISK -> RiskContent(vixData, hySpreadData, fearGreedData)
                MacroTab.ASSETS -> AssetsContent(btcData, goldData, sp500Data, nasdaqData)
                MacroTab.COMMODITY -> CommodityContent(wtiData, copperData)
                MacroTab.KOREA -> KoreaContent(kospiData, usdkrwData)
                MacroTab.INSIGHT -> InsightContent()
            }
        }
    }
}

// --- 각 탭별 UI 구현부 ---

// ==========================================
// 1. Market Pulse 탭
// ==========================================
// ==========================================
// 1. Market Pulse 탭 (주식, 금리, 달러 종합)
// ==========================================
@Composable
fun MarketPulseContent(
    us10yData: com.example.investmentassistant.api.MacroData,
    sp500Data: com.example.investmentassistant.api.MacroData,
    nasdaqData: com.example.investmentassistant.api.MacroData,
    dollarIndexData: com.example.investmentassistant.api.MacroData
) {
    // 1. 각 지표별 상승/하락 여부 판단
    val isSp500Up = sp500Data.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isNasdaqUp = nasdaqData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isYieldDown = us10yData.history.let { it.size > 1 && it.last() < it[it.size - 2] } // 금리 하락이 호재
    val isDollarDown = dollarIndexData.history.let { it.size > 1 && it.last() < it[it.size - 2] } // 달러 하락이 호재

    // 2. 호재(주식 상승, 금리/달러 하락) 개수 카운트
    val bullScore = listOf(isSp500Up, isNasdaqUp, isYieldDown, isDollarDown).count { it }

    // 3. 점수에 따른 종합 상태 결정
    val (statusText, statusColor) = when {
        bullScore >= 3 -> "강력한 Risk ON (위험 선호)" to Color(0xFF4CAF50) // 3개 이상 호재
        bullScore == 2 -> "혼조세 (방향성 탐색 중)" to Color(0xFFFFA500) // 주황색
        else -> "Risk OFF (안전 자산 선호)" to Color(0xFFFF4444) // 악재 우위
    }

    StatusCard(statusText, statusColor, "주식, 금리, 달러 동향 종합 심리")
    GuideSection("미 국채 10년물 금리가 오르면 기술주/성장주에 악재로 작용합니다. 또한 달러 가치가 오르면(강달러) 외국인 자금이 미국으로 빠져나가 신흥국 증시와 가상자산에 부정적입니다.")

    InfoRow("S&P 500", sp500Data.latestValue, "실시간", isSp500Up, sp500Data.history)
    InfoRow("NASDAQ", nasdaqData.latestValue, "실시간", isNasdaqUp, nasdaqData.history)
    InfoRow("미 국채 10Y", us10yData.latestValue, "실시간", isYieldDown, us10yData.history)
    InfoRow("달러 인덱스", dollarIndexData.latestValue, "실시간 (DXY)", !isDollarDown, dollarIndexData.history)
}

// ==========================================
// 2. Liquidity 탭 (연준 자산, 통화량, 실질 금리 종합)
// ==========================================
@Composable
fun LiquidityContent(
    fedData: com.example.investmentassistant.api.MacroData,
    m2Data: com.example.investmentassistant.api.MacroData,
    realRateData: com.example.investmentassistant.api.MacroData
) {
    val isFedUp = fedData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isM2Up = m2Data.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isRealRateDown = realRateData.history.let { it.size > 1 && it.last() < it[it.size - 2] } // 실질 금리 하락이 유동성에 호재

    val liquidityScore = listOf(isFedUp, isM2Up, isRealRateDown).count { it }

    val (statusText, statusColor) = when {
        liquidityScore >= 2 -> "유동성 확장 국면 (증시 호재)" to Color(0xFF4CAF50)
        liquidityScore == 1 -> "유동성 중립 (관망세)" to Color(0xFFFFA500)
        else -> "유동성 축소 국면 (긴축)" to Color.Gray
    }

    StatusCard(statusText, statusColor, "연준 대차대조표 및 실질금리 종합")
    GuideSection("연준 자산과 M2는 '시중에 풀린 돈의 양'입니다. 줄어들면(긴축) 자산 시장의 상승 동력이 떨어집니다. 체감 금리인 '실질 금리'가 상승하면 자금이 주식에서 채권으로 이동합니다.")

    InfoRow("Fed 연준 자산", fedData.latestValue, "실시간 데이터", isFedUp, fedData.history)
    InfoRow("M2 통화량", m2Data.latestValue, "실시간 데이터", isM2Up, m2Data.history)
    InfoRow("실질 금리 (10Y)", realRateData.latestValue, "TIPS 기준 실시간", !isRealRateDown, realRateData.history)
}

// ==========================================
// 3. Rates 탭 (장단기 역전 여부 및 금리 추세 종합)
// ==========================================
@Composable
fun RatesContent(
    data10y: com.example.investmentassistant.api.MacroData,
    data2y: com.example.investmentassistant.api.MacroData
) {
    val spreadVal = try {
        val y10 = data10y.latestValue.replace("%", "").toFloat()
        val y2 = data2y.latestValue.replace("%", "").toFloat()
        y10 - y2
    } catch (e: Exception) { 0f }

    val isReverted = spreadVal < 0
    val is10yRising = data10y.history.let { it.size > 1 && it.last() > it[it.size - 2] }

    val (statusText, statusColor) = when {
        isReverted -> "침체 경고 (장단기 금리 역전)" to Color(0xFFFF4444)
        is10yRising -> "장기 금리 상승기 (성장주 부담)" to Color(0xFFFFA500)
        else -> "정상적인 수익률 곡선 (안정적)" to Color(0xFF4CAF50)
    }

    StatusCard(statusText, statusColor, "수익률 곡선 및 통화정책 지표")
    GuideSection("단기 금리(2Y)가 장기 금리(10Y)보다 높아지는 '역전 현상(마이너스)'이 발생하면, 보통 1~2년 내에 경제에 큰 위기나 경기 침체가 온다는 강력한 역사적 시그널입니다.")

    InfoRow("미 국채 2Y", data2y.latestValue, "실시간 데이터", !is10yRising, data2y.history)
    InfoRow("미 국채 10Y", data10y.latestValue, "실시간 데이터", !is10yRising, data10y.history)

    val spreadHistory = if (data10y.history.isNotEmpty() && data2y.history.isNotEmpty()) {
        data10y.history.zip(data2y.history) { y10, y2 -> y10 - y2 }
    } else emptyList()
    InfoRow("10Y-2Y 스프레드", String.format("%.2fp", spreadVal), if (isReverted) "역전 상태" else "정상", !isReverted, spreadHistory)
}

// ==========================================
// 4. Risk 탭 (VIX, 하이일드, F&G 종합)
// ==========================================
@Composable
fun RiskContent(
    vixData: com.example.investmentassistant.api.MacroData,
    hySpreadData: com.example.investmentassistant.api.MacroData,
    fearGreedData: com.example.investmentassistant.api.MacroData
) {
    val isVixHigh = (vixData.latestValue.toFloatOrNull() ?: 0f) > 20f
    val isHyRising = hySpreadData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isFear = (fearGreedData.latestValue.toFloatOrNull() ?: 50f) < 45f // 45 이하면 공포

    // 리스크 요인(악재) 개수 카운트
    val riskScore = listOf(isVixHigh, isHyRising, isFear).count { it }

    val (statusText, statusColor) = when {
        riskScore >= 2 -> "시장 공포 및 변동성 확대" to Color(0xFFFF4444)
        riskScore == 1 -> "리스크 요인 부분 발생 (주의)" to Color(0xFFFFA500)
        else -> "안정적인 시장 심리 (위험 선호)" to Color(0xFF4CAF50)
    }

    StatusCard(statusText, statusColor, "VIX 및 기업 부도 위험 종합")
    GuideSection("VIX는 시장의 '공포'를 나타냅니다. 20이 넘으면 경계해야 합니다. 하이일드 스프레드가 치솟으면 한계 기업들의 연쇄 부도 위험이 커졌다는 뜻입니다.")

    InfoRow("VIX (공포지수)", vixData.latestValue, "실시간 데이터", !isVixHigh, vixData.history)
    InfoRow("하이일드 스프레드", hySpreadData.latestValue, "실시간 (FRED)", !isHyRising, hySpreadData.history)

    val fngStatus = if (isFear) "공포 구간" else "중립/탐욕 구간"
    val isFngUp = fearGreedData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("Fear & Greed", fearGreedData.latestValue, fngStatus, isFngUp, fearGreedData.history)
}

// ==========================================
// 5. Assets 탭 (비트코인, 금, 나스닥 RS 종합)
// ==========================================
@Composable
fun AssetsContent(
    btcData: com.example.investmentassistant.api.MacroData,
    goldData: com.example.investmentassistant.api.MacroData,
    sp500Data: com.example.investmentassistant.api.MacroData,
    nasdaqData: com.example.investmentassistant.api.MacroData
) {
    val isBtcUp = btcData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isGoldUp = goldData.history.let { it.size > 1 && it.last() > it[it.size - 2] }

    val rsHistory = if (sp500Data.history.isNotEmpty() && nasdaqData.history.isNotEmpty()) {
        sp500Data.history.zip(nasdaqData.history) { sp, ndq -> if (sp != 0f) ndq / sp else 0f }
    } else emptyList()
    val isNasdaqOutperforming = if (rsHistory.size > 1) rsHistory.last() > rsHistory.first() else false

    val (statusText, statusColor) = when {
        isNasdaqOutperforming && isBtcUp -> "위험 자산(기술/코인) 랠리" to Color(0xFF4CAF50)
        isGoldUp && !isBtcUp -> "안전 자산(금) 선호 심리" to Color(0xFFFFA500)
        else -> "자산별 차별화 장세" to Color.Gray
    }

    StatusCard(statusText, statusColor, "디지털 vs 전통 자산 흐름 종합")
    GuideSection("금은 인플레이션이나 위기 발생 시 오르는 전통적 방어 자산입니다. 나스닥 상대강도(Outperform)가 뜬다면, 증시 자금이 '빅테크' 위주로만 쏠리고 있다는 뜻입니다.")

    InfoRow("비트코인", "$${btcData.latestValue}", "실시간 (BTC-USD)", isBtcUp, btcData.history)
    InfoRow("금 (Gold)", "$${goldData.latestValue}", "실시간 선물 (GC=F)", isGoldUp, goldData.history)
    InfoRow("나스닥 상대강도", if (isNasdaqOutperforming) "Outperform" else "Underperform", if (isNasdaqOutperforming) "강세 (빅테크 주도)" else "약세", isNasdaqOutperforming, rsHistory)
}

// ==========================================
// 6. Commodity 탭 (유가, 구리 종합)
// ==========================================
@Composable
fun CommodityContent(
    wtiData: com.example.investmentassistant.api.MacroData,
    copperData: com.example.investmentassistant.api.MacroData
) {
    val isWtiUp = wtiData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    val isCopperUp = copperData.history.let { it.size > 1 && it.last() > it[it.size - 2] }

    val (statusText, statusColor) = when {
        isWtiUp && isCopperUp -> "경기 회복 기대 및 인플레 압력" to Color(0xFFFF4444) // 유가 동반 상승은 인플레 우려(빨강)
        !isWtiUp && !isCopperUp -> "물가 압력 완화 (안정화)" to Color(0xFF4CAF50)
        else -> "에너지/산업금속 혼조세" to Color(0xFFFFA500)
    }

    StatusCard(statusText, statusColor, "에너지 및 산업금속 동향 종합")
    GuideSection("WTI 유가가 오르면 물가가 다시 뛰어올라 연준이 금리를 내리기 힘들어집니다. 구리는 '닥터 코퍼'로 불리며, 가격 상승은 공장이 돌아가고 실물 경기가 살아나고 있다는 청신호입니다.")

    InfoRow("WTI 원유", "$${wtiData.latestValue}", "실시간 선물 (CL=F)", isWtiUp, wtiData.history)
    InfoRow("구리 (Copper)", "$${copperData.latestValue}", "실시간 선물 (HG=F)", isCopperUp, copperData.history)
}

// ==========================================
// 7. Korea 탭 (코스피, 환율 종합)
// ==========================================
@Composable
fun KoreaContent(
    kospiData: com.example.investmentassistant.api.MacroData,
    usdkrwData: com.example.investmentassistant.api.MacroData
) {
    val isFxHigh = (usdkrwData.latestValue.replace(",", "").toFloatOrNull() ?: 0f) > 1350f
    val isKospiUp = kospiData.history.let { it.size > 1 && it.last() > it[it.size - 2] }

    val (statusText, statusColor) = when {
        isFxHigh && !isKospiUp -> "국장 투자 매력도 하락 (환율/증시 이중고)" to Color(0xFFFF4444)
        !isFxHigh && isKospiUp -> "안정적 환율 및 증시 상승 기대" to Color(0xFF4CAF50)
        else -> "환율/증시 힘겨루기 중" to Color(0xFFFFA500)
    }

    StatusCard(statusText, statusColor, "코스피 및 환율 추이 종합")
    GuideSection("환율이 크게 오르면(원화 가치 하락), 외국인 투자자들은 가만히 있어도 손해(환차손)를 보기 때문에 한국 주식을 팔고 달러로 바꿔서 떠나는 핵심 원인이 됩니다.")

    InfoRow("KOSPI", kospiData.latestValue, "실시간 (^KS11)", isKospiUp, kospiData.history)
    InfoRow("원/달러 환율", "₩${usdkrwData.latestValue}", "실시간 (KRW=X)", !isFxHigh, usdkrwData.history) // 환율 상승은 악재
    InfoRow("외국인 수급", if (isKospiUp) "+3,240억" else "-4,150억", "모의 데이터 (미래에셋 대기)", isKospiUp)
}
// ==========================================
// ★ Insight (AI 분석) 탭
// ==========================================
@Composable
fun InsightContent(viewModel: MacroViewModel = viewModel()) {
    // 뷰모델에서 AI 상태 가져오기
    val insightText by viewModel.aiInsight.collectAsState()
    val isInsightLoading by viewModel.isInsightLoading.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 헤더 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧠", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "AI 매크로 리포트",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 로딩 중일 때 뺑뺑이 표시
                if (isInsightLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // AI 분석 텍스트 출력 영역
            Text(
                text = insightText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 분석 시작 버튼
            Button(
                onClick = { viewModel.generateAiInsight() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInsightLoading // 로딩 중에는 버튼 비활성화
            ) {
                Text(if (insightText.contains("시작하세요")) "실시간 지표 분석하기" else "최신 데이터로 재분석")
            }
        }
    }
}

// --- 공통 재사용 컴포넌트 ---

// ★ 미니 그래프를 그리는 핵심 컴포넌트 추가 ★
@Composable
fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return

    Canvas(modifier = modifier) {
        val max = data.maxOrNull() ?: 0f
        val min = data.minOrNull() ?: 0f
        val range = if (max == min) 1f else max - min

        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - min) / range) * height
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun StatusCard(status: String, color: Color, summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = status, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ★ InfoRow에 히스토리 데이터를 받아 Sparkline을 그리도록 수정 ★
@Composable
fun InfoRow(
    label: String,
    value: String,
    subValue: String,
    isPositive: Boolean,
    history: List<Float> = emptyList() // 그래프용 데이터 리스트 추가
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 이름
        Text(text = label, fontWeight = FontWeight.Medium, modifier = Modifier.width(100.dp))

        // 2. 가운데 미니 그래프 (데이터가 있을 때만 그림)
        if (history.isNotEmpty()) {
            Sparkline(
                data = history,
                color = if (isPositive) Color.Red else Color.Blue,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .padding(horizontal = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 3. 수치 및 등락
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(80.dp)) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = subValue,
                color = if (isPositive) Color.Red else Color.Blue,
                fontSize = 12.sp
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun InsightItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun GuideSection(guideText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text("💡", modifier = Modifier.padding(end = 8.dp))
            Text(
                text = guideText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}