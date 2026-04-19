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
            when (selectedTab) {
                MacroTab.PULSE -> MarketPulseContent(us10yData, sp500Data, nasdaqData, dollarIndexData)
                MacroTab.LIQUIDITY -> LiquidityContent(fedBalanceData, m2Data, realRateData)
                MacroTab.RATES -> RatesContent(us10yData, us2yData)
                MacroTab.RISK -> RiskContent(vixData, hySpreadData, fearGreedData)
                MacroTab.ASSETS -> AssetsContent(btcData, goldData)
                MacroTab.COMMODITY -> CommodityContent(wtiData, copperData)
                MacroTab.KOREA -> KoreaContent(kospiData, usdkrwData)
                MacroTab.INSIGHT -> InsightContent()
            }
        }
    }
}

// --- 각 탭별 UI 구현부 ---

@Composable
fun MarketPulseContent(
    us10yData: com.example.investmentassistant.api.MacroData,
    sp500Data: com.example.investmentassistant.api.MacroData,
    nasdaqData: com.example.investmentassistant.api.MacroData,
    dollarIndexData: com.example.investmentassistant.api.MacroData // ★ 파라미터 추가
) {
    val isYieldRising = if (us10yData.history.size > 1) {
        (us10yData.history.lastOrNull() ?: 0f) > (us10yData.history.dropLast(1).lastOrNull() ?: 0f)
    } else true

    val statusText = if (isYieldRising) "Risk OFF (보수적)" else "Risk ON (긍정적)"
    val statusColor = if (isYieldRising) Color(0xFFFF4444) else Color(0xFF4CAF50)

    StatusCard(statusText, statusColor, "금리/달러 추이에 따른 현재 시장 심리 요약")

    val isSp500Up = sp500Data.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("S&P 500", sp500Data.latestValue, "실시간", isSp500Up, sp500Data.history)

    val isNasdaqUp = nasdaqData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("NASDAQ", nasdaqData.latestValue, "실시간", isNasdaqUp, nasdaqData.history)

    InfoRow("미 국채 10Y", us10yData.latestValue, "실시간", !isYieldRising, us10yData.history)

    // ★ 가짜 데이터 삭제하고 진짜 달러 인덱스 연결!
    val isDollarUp = dollarIndexData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    // (달러가 오르면 보통 시장에 악재이므로 색상을 파란색으로 하려면 !isDollarUp을 쓸 수도 있지만, 일단 상승=빨강 으로 통일합니다)
    InfoRow("달러 인덱스", dollarIndexData.latestValue, "실시간 (DXY)", isDollarUp, dollarIndexData.history)
}
@Composable
fun LiquidityContent(
    fedData: com.example.investmentassistant.api.MacroData,
    m2Data: com.example.investmentassistant.api.MacroData,
    realRateData: com.example.investmentassistant.api.MacroData // ★ 파라미터 추가
) {
    val isFedTightening = if (fedData.history.size > 1) {
        (fedData.history.lastOrNull() ?: 0f) < (fedData.history.dropLast(1).lastOrNull() ?: 0f)
    } else false

    val statusText = if (isFedTightening) "유동성 축소 중" else "유동성 횡보/확장"
    val statusColor = if (isFedTightening) Color.Gray else Color(0xFF4CAF50)

    StatusCard(statusText, statusColor, "연준 대차대조표 및 실질금리 실시간 추이")

    InfoRow(
        label = "Fed 연준 자산",
        value = fedData.latestValue,
        subValue = "실시간 데이터",
        isPositive = !isFedTightening,
        history = fedData.history
    )

    InfoRow(
        label = "M2 통화량",
        value = m2Data.latestValue,
        subValue = "실시간 데이터",
        isPositive = false,
        history = m2Data.history
    )

    // ★ 3. 가짜 데이터 삭제하고 진짜 실질 금리 데이터와 그래프 적용
    // (실질 금리 상승은 주식 시장에 악재이므로 파란색으로 표시하려면 isPositive를 조절할 수 있습니다)
    InfoRow(
        label = "실질 금리 (10Y)",
        value = realRateData.latestValue,
        subValue = "TIPS 기준 실시간",
        isPositive = true, // 상승을 빨간색으로 표시
        history = realRateData.history
    )
}
@Composable
fun RatesContent(
    data10y: com.example.investmentassistant.api.MacroData,
    data2y: com.example.investmentassistant.api.MacroData
) {
    StatusCard("금리 압박 심화", Color(0xFFFFA500), "연준 통화정책의 핵심 지표")

    InfoRow("미 국채 2Y", data2y.latestValue, "실시간 데이터", true, data2y.history)
    InfoRow("미 국채 10Y", data10y.latestValue, "실시간 데이터", true, data10y.history)

    // ★ 1. 최신 스프레드 값 계산
    val spread = try {
        val y10 = data10y.latestValue.replace("%", "").toFloat()
        val y2 = data2y.latestValue.replace("%", "").toFloat()
        String.format("%.2fp", y10 - y2)
    } catch (e: Exception) { "-" }

    val isReversed = spread.startsWith("-")

    // ★ 2. 과거 30일치 스프레드 추세 데이터(리스트) 생성
    val spreadHistory = if (data10y.history.isNotEmpty() && data2y.history.isNotEmpty()) {
        // 10년물 데이터와 2년물 데이터를 짝지어서 뺀 값을 새로운 리스트로 만듭니다.
        // zip 함수는 두 리스트의 같은 순서(인덱스)에 있는 값들을 묶어줍니다.
        data10y.history.zip(data2y.history) { y10, y2 ->
            y10 - y2
        }
    } else {
        emptyList()
    }

    // ★ 3. InfoRow에 스프레드 추세 데이터(spreadHistory) 전달
    InfoRow(
        label = "10Y-2Y 스프레드",
        value = spread,
        subValue = if (isReversed) "역전 상태 (침체 시그널)" else "정상",
        isPositive = !isReversed,
        history = spreadHistory // 여기에 우리가 방금 만든 리스트를 넣습니다!
    )
}
// (RiskContent, AssetsContent 등 나머지 탭은 기존과 동일하되 가짜 데이터 구조 유지)
@Composable
fun RiskContent(
    vixData: com.example.investmentassistant.api.MacroData,
    hySpreadData: com.example.investmentassistant.api.MacroData,
    // ★ 3. 세 번째 파라미터 추가!
    fearGreedData: com.example.investmentassistant.api.MacroData
) {
    val vixValue = vixData.latestValue.replace(",", "").toFloatOrNull() ?: 0f
    val isVixHigh = vixValue > 20f

    val statusText = if (isVixHigh) "공포 단계 (변동성 확대)" else "안정 단계 (위험 선호)"
    val statusColor = if (isVixHigh) Color(0xFFFF4444) else Color(0xFF4CAF50)

    StatusCard(statusText, statusColor, "VIX 지수 및 주요 리스크 지표 추이")

    val isVixUp = vixData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("VIX (공포지수)", vixData.latestValue, "실시간 데이터", !isVixUp, vixData.history)

    val isHyUp = hySpreadData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("하이일드 스프레드", hySpreadData.latestValue, "실시간 (FRED)", !isHyUp, hySpreadData.history)

    // ★ 4. 가짜 데이터 지우고 진짜 Fear & Greed 연결!
    val fngValue = fearGreedData.latestValue.toFloatOrNull() ?: 50f
    // 수치가 50 이상이면 탐욕(Greed), 미만이면 공포(Fear)
    val fngStatus = if (fngValue > 50) "탐욕 구간" else "공포 구간"
    // 수치가 오르는 것(탐욕으로 가는 것)을 초록색으로 표시할지, 빨간색으로 표시할지 설정
    // 보통 탐욕은 위험(빨강) 또는 긍정(초록)으로 해석되는데, 여기서는 상승을 파란색(과열 경고)으로 뒤집지 않고 기본 방향성(!isHyUp처럼 역상관이 아님)으로 두겠습니다.
    val isFngUp = fearGreedData.history.let { it.size > 1 && it.last() > it[it.size - 2] }

    InfoRow("Fear & Greed", fearGreedData.latestValue, fngStatus, isFngUp, fearGreedData.history)
}

@Composable
fun AssetsContent(
    btcData: com.example.investmentassistant.api.MacroData,
    goldData: com.example.investmentassistant.api.MacroData
) {
    StatusCard("디지털 vs 전통 안전 자산", Color(0xFF4CAF50), "비트코인과 금 가격 실시간 추이")

    val isBtcUp = btcData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("비트코인", "$${btcData.latestValue}", "실시간 (BTC-USD)", isBtcUp, btcData.history)

    val isGoldUp = goldData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("금 (Gold)", "$${goldData.latestValue}", "실시간 선물 (GC=F)", isGoldUp, goldData.history)

    // 나스닥 상대강도는 추가 연산이 필요하므로 틀만 남김
    InfoRow("나스닥 상대강도", "약세", "추후 연동 필요", false)
}

@Composable
fun CommodityContent(
    wtiData: com.example.investmentassistant.api.MacroData,
    copperData: com.example.investmentassistant.api.MacroData
) {
    StatusCard("인플레이션 & 경기 선행 지표", Color(0xFFFFA500), "에너지(유가) 및 산업금속(구리) 동향")

    val isWtiUp = wtiData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("WTI 원유", "$${wtiData.latestValue}", "실시간 선물 (CL=F)", isWtiUp, wtiData.history)

    val isCopperUp = copperData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("구리 (Copper)", "$${copperData.latestValue}", "실시간 선물 (HG=F)", isCopperUp, copperData.history)
}

@Composable
fun KoreaContent(
    kospiData: com.example.investmentassistant.api.MacroData,
    usdkrwData: com.example.investmentassistant.api.MacroData
) {
    // 환율이 1350원을 넘으면 경고(빨간색)로 표시
    val krwValue = usdkrwData.latestValue.replace(",", "").toFloatOrNull() ?: 0f
    val isFxHigh = krwValue > 1350f

    val statusText = if (isFxHigh) "국장 주의보 (환율 불안정)" else "안정적 환율 흐름"
    val statusColor = if (isFxHigh) Color(0xFFFF4444) else Color(0xFF4CAF50)

    StatusCard(statusText, statusColor, "코스피 지수 및 원/달러 환율 추이")

    val isKospiUp = kospiData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    InfoRow("KOSPI", kospiData.latestValue, "실시간 (^KS11)", isKospiUp, kospiData.history)

    val isUsdkrwUp = usdkrwData.history.let { it.size > 1 && it.last() > it[it.size - 2] }
    // 환율 상승은 국장에 악재이므로 빨간색으로 표시하기 위해 !isUsdkrwUp 사용
    InfoRow("원/달러 환율", "₩${usdkrwData.latestValue}", "실시간 (KRW=X)", !isUsdkrwUp, usdkrwData.history)

    // 외국인 수급은 증권사 API(미래에셋 등)가 필요하므로 일단 틀만 남김
    InfoRow("외국인 수급", "연동 대기중", "증권사 API 필요", false)
}

@Composable
fun InsightContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("💡 AI 투자 행동 지침", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            InsightItem("현재 현금 비중 40% 이상 확보 권고")
            InsightItem("고금리 지속 시 기술주 비중 축소, 방어주 검토")
            InsightItem("VIX 30 돌파 전까지는 공격적 매수 지양")
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