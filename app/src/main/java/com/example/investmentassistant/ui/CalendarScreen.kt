package com.example.investmentassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.data.repository.CalendarRepository
import com.example.investmentassistant.model.CalendarEvent
import com.example.investmentassistant.model.EventImportance
import com.example.investmentassistant.model.EventType
import com.example.investmentassistant.viewmodel.CalendarUiState
import com.example.investmentassistant.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    bottomPadding: PaddingValues,
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("경제 캘린더") },
                    actions = {
                        IconButton(onClick = { viewModel.load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("이번 주") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("달력") },
                    )
                }
            }
        }
    ) { innerPadding ->
        val combinedPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = bottomPadding.calculateBottomPadding(),
        )
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(combinedPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(combinedPadding), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            selectedTab == 0 -> WeeklyContent(
                uiState = uiState,
                padding = combinedPadding,
                onEventClick = { selectedEvent = it },
            )
            else -> MonthCalendarView(
                uiState = uiState,
                onSelectDate = viewModel::selectDate,
                onChangeMonth = viewModel::changeMonth,
                padding = combinedPadding,
                onEventClick = { selectedEvent = it },
            )
        }
    }

    selectedEvent?.let { event ->
        EventDetailBottomSheet(
            event = event,
            onDismiss = { selectedEvent = null },
        )
    }
}

// ── 이번 주 뷰 ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyContent(
    uiState: CalendarUiState,
    padding: PaddingValues,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val hasAnyEvent = uiState.todayEvents.isNotEmpty() ||
            uiState.tomorrowEvents.isNotEmpty() ||
            uiState.thisWeekEvents.isNotEmpty()

    if (!hasAnyEvent) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("이번 주 주요 이벤트가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.todayEvents.isNotEmpty()) {
            item { SectionHeader("오늘") }
            items(uiState.todayEvents, key = { it.id }) { CalendarEventCard(it, onClick = { onEventClick(it) }) }
        }
        if (uiState.tomorrowEvents.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); SectionHeader("내일") }
            items(uiState.tomorrowEvents, key = { it.id }) { CalendarEventCard(it, onClick = { onEventClick(it) }) }
        }
        if (uiState.thisWeekEvents.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); SectionHeader("이번 주") }
            items(uiState.thisWeekEvents, key = { it.id }) { CalendarEventCard(it, onClick = { onEventClick(it) }) }
        }
    }
}

// ── 달력 뷰 ─────────────────────────────────────────────────────────────────

@Composable
private fun MonthCalendarView(
    uiState: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    padding: PaddingValues,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            MonthNavigationHeader(
                yearMonth = uiState.currentMonth,
                onPrev = { onChangeMonth(uiState.currentMonth.minusMonths(1)) },
                onNext = { onChangeMonth(uiState.currentMonth.plusMonths(1)) },
            )
        }
        item { DayOfWeekHeader() }
        item {
            CalendarGrid(
                yearMonth = uiState.currentMonth,
                monthlyEvents = uiState.monthlyEvents,
                selectedDate = uiState.selectedDate,
                today = today,
                onSelectDate = onSelectDate,
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        if (uiState.selectedDate != null) {
            item {
                Text(
                    text = "${uiState.selectedDate.monthValue}월 ${uiState.selectedDate.dayOfMonth}일 이벤트",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (uiState.selectedDateEvents.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("이 날의 주요 이벤트가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.selectedDateEvents, key = { it.id }) { event ->
                    CalendarEventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        } else {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("날짜를 선택하면 이벤트를 볼 수 있습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── 이벤트 상세 바텀시트 ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailBottomSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 제목
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImportanceDot(event.importance)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${countryFlag(event.country)} ${event.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 시간 + 발표됨 뱃지
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val timeStr = formatTime(event.scheduledAt)
                val timeLabel = if (event.type == EventType.EARNINGS && event.earningsTime?.isNotEmpty() == true)
                    "${event.earningsTime}  $timeStr" else timeStr
                Text(timeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.isAnnounced) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "발표됨",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            HorizontalDivider()

            // 설명
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = getEventDescription(event),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }

            // 수치 데이터
            when (event.type) {
                EventType.ECONOMIC -> {
                    if (event.previous != null || event.forecast != null || event.actual != null) {
                        HorizontalDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            event.previous?.let { ValueChip("이전", it) }
                            event.forecast?.let { ValueChip("예상", it) }
                            event.actual?.let { ValueChip("실제", it, highlight = true) }
                        }
                    }
                }
                EventType.EARNINGS -> {
                    if (event.epsEstimate != null || event.epsActual != null ||
                        event.revenueEstimate != null || event.revenueActual != null) {
                        HorizontalDivider()
                        EarningsValues(event)
                    }
                }
            }
        }
    }
}

// ── 달력 그리드 ──────────────────────────────────────────────────────────────

@Composable
private fun MonthNavigationHeader(yearMonth: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달") }
        Text(
            text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달") }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = when (index) {
                    0 -> Color(0xFFE53935)
                    6 -> Color(0xFF1565C0)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    monthlyEvents: Map<LocalDate, List<CalendarEvent>>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    val startOffset = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalRows = (startOffset + daysInMonth + 6) / 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        repeat(totalRows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayNum = row * 7 + col - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = yearMonth.atDay(dayNum)
                        DayCell(
                            day = dayNum,
                            events = monthlyEvents[date] ?: emptyList(),
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            isSunday = col == 0,
                            isSaturday = col == 6,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    events: List<CalendarEvent>,
    isSelected: Boolean,
    isToday: Boolean,
    isSunday: Boolean,
    isSaturday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    isSunday -> Color(0xFFE53935)
                    isSaturday -> Color(0xFF1565C0)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (events.any { it.importance == EventImportance.HIGH }) EventDot(Color(0xFFE53935))
            if (events.any { it.importance == EventImportance.MEDIUM }) EventDot(Color(0xFFFB8C00))
            if (events.isNotEmpty() && events.all { it.importance == EventImportance.LOW }) EventDot(Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun EventDot(color: Color) {
    Box(Modifier.size(4.dp).clip(CircleShape).background(color))
}

// ── 공통 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun CalendarEventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isAnnounced)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImportanceDot(event.importance)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${countryFlag(event.country)} ${event.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (event.isAnnounced) {
                    Text("발표됨", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            val timeStr = formatTime(event.scheduledAt)
            val timeLabel = if (event.type == EventType.EARNINGS && event.earningsTime?.isNotEmpty() == true)
                "${event.earningsTime}  $timeStr" else timeStr
            Text(timeLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            when (event.type) {
                EventType.ECONOMIC -> EconomicValues(event)
                EventType.EARNINGS -> EarningsValues(event)
            }
        }
    }
}

@Composable
private fun EconomicValues(event: CalendarEvent) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        event.previous?.let { ValueChip("이전", it) }
        event.forecast?.let { ValueChip("예상", it) }
        event.actual?.let { ValueChip("실제", it, highlight = true) }
    }
}

@Composable
private fun EarningsValues(event: CalendarEvent) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (event.epsEstimate != null || event.epsActual != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EPS", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                event.epsEstimate?.let { ValueChip("예상", "%.2f".format(it)) }
                event.epsActual?.let { ValueChip("실제", "%.2f".format(it), highlight = true) }
            }
        }
        if (event.revenueEstimate != null || event.revenueActual != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("매출", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                event.revenueEstimate?.let { ValueChip("예상", formatRevenue(it)) }
                event.revenueActual?.let { ValueChip("실제", formatRevenue(it), highlight = true) }
            }
        }
    }
}

@Composable
private fun ValueChip(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ImportanceDot(importance: EventImportance) {
    val color = when (importance) {
        EventImportance.HIGH -> Color(0xFFE53935)
        EventImportance.MEDIUM -> Color(0xFFFB8C00)
        EventImportance.LOW -> Color(0xFF9E9E9E)
    }
    Surface(shape = MaterialTheme.shapes.small, color = color, modifier = Modifier.size(8.dp)) {}
}

// ── 설명 헬퍼 ───────────────────────────────────────────────────────────────

private fun getEventDescription(event: CalendarEvent): String = when (event.type) {
    EventType.ECONOMIC -> getEconomicDescription(event.title)
    EventType.EARNINGS -> getEarningsDescription(event)
}

private fun getEconomicDescription(title: String): String {
    val lower = title.lowercase()
    return when {
        "cpi" in lower || "소비자물가" in lower ->
            "소비자물가지수(CPI)는 소비자가 구입하는 상품·서비스의 가격 변동을 측정합니다. 인플레이션의 핵심 지표로 연준(Fed)의 금리 결정에 직접적인 영향을 미칩니다. 예상치를 웃돌면 금리 인상 가능성이 높아져 주식·채권에 부담이 됩니다."
        "ppi" in lower || "생산자물가" in lower ->
            "생산자물가지수(PPI)는 생산자가 받는 상품·서비스의 평균 가격 변동을 측정합니다. CPI의 선행 지표로 여겨지며, 기업 이익률과 향후 소비자 물가 방향을 가늠하는 데 활용됩니다."
        "fomc" in lower || "기준금리" in lower ->
            "연방공개시장위원회(FOMC)에서 미국 기준금리를 결정합니다. 금리 인상은 달러 강세·채권 수익률 상승으로 이어지며 주식 시장에 부담이 될 수 있습니다. 반대로 금리 인하는 위험자산 선호 심리를 높입니다."
        "pce" in lower ->
            "개인소비지출(PCE) 물가지수는 연준이 인플레이션 목표치(2%) 달성 여부를 판단할 때 가장 중시하는 지표입니다. CPI보다 광범위한 소비 항목을 포함하며 변동성이 낮습니다."
        "gdp" in lower ->
            "국내총생산(GDP)은 일정 기간 국내에서 생산된 재화·서비스의 총 가치입니다. 경기 건강을 보여주는 핵심 지표로, 예상치 대비 결과가 경기 전망과 시장 심리에 영향을 미칩니다."
        "nonfarm" in lower || "비농업" in lower || "payroll" in lower || "고용 보고서" in lower ->
            "비농업 고용(NFP)은 농업을 제외한 분야의 신규 일자리 수를 나타냅니다. 고용 시장의 건강을 직접 보여주는 지표로 연준의 통화정책 결정에 큰 영향을 미칩니다. 예상치보다 높으면 경기 과열 우려로 금리 인상 압박이 커질 수 있습니다."
        "실업률" in lower ->
            "실업률은 구직 중인 인구 대비 미취업자의 비율입니다. 낮은 실업률은 경기 호황을 나타내지만, 인플레이션 압력을 높여 연준의 긴축 강화로 이어질 수 있습니다."
        "ism" in lower ->
            "ISM 지수는 구매 담당자 설문 기반의 경기 선행 지표입니다. 50 이상이면 경기 확장, 이하면 수축을 의미합니다. 제조업과 서비스업 경기 방향을 빠르게 파악하는 데 활용됩니다."
        "소매판매" in lower || "retail" in lower ->
            "소매판매는 소비자 지출 동향을 직접 측정하는 지표입니다. 미국 GDP의 약 70%가 소비에서 나오기 때문에 경기 판단에 매우 중요합니다. 강한 수치는 경기 회복 신호이지만 동시에 인플레이션 압력을 높일 수 있습니다."
        "pmi" in lower ->
            "구매관리자지수(PMI)는 기업 구매 담당자들의 경기 인식을 조사한 선행 지표입니다. 50 기준으로 경기 확장·수축을 구분하며, 실제 경제 데이터보다 한 발 앞서 경기 방향을 제시합니다."
        else ->
            "주요 경제 지표 발표입니다. 실제 수치와 시장 예상치를 비교해 시장 영향을 판단하세요."
    }
}

private fun getEarningsDescription(event: CalendarEvent): String {
    val ticker = event.ticker ?: return "기업 실적 발표입니다."
    val company = CalendarRepository.getCompanyName(ticker)
    val timing = when (event.earningsTime) {
        "장 시작 전" -> "장 시작 전(BMO) 발표로 당일 시초가에 즉시 반영됩니다."
        "장 마감 후" -> "장 마감 후(AMC) 발표로 다음 거래일 시초가에 반영됩니다."
        "장중" -> "장중 발표로 발표 직후 즉시 주가에 영향을 미칩니다."
        else -> "발표 시간은 미정입니다."
    }
    return "${company}($ticker)의 분기 실적을 발표합니다. $timing EPS(주당순이익)와 매출이 시장 예상치(컨센서스)를 상회하면 어닝 서프라이즈로 주가 상승 요인이 됩니다."
}

// ── 유틸 ────────────────────────────────────────────────────────────────────

private fun countryFlag(country: String) = when (country.uppercase()) {
    "US" -> "🇺🇸"
    "KR" -> "🇰🇷"
    "EU", "EUR" -> "🇪🇺"
    "JP" -> "🇯🇵"
    "CN" -> "🇨🇳"
    "GB", "UK" -> "🇬🇧"
    else -> "🌐"
}

private fun formatTime(epochMs: Long): String {
    val ldt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Asia/Seoul"))
    return if (ldt.hour == 0 && ldt.minute == 0) "시간 미정"
    else DateTimeFormatter.ofPattern("HH:mm 'KST'").format(ldt)
}

private fun formatRevenue(value: Long): String = when {
    value >= 1_000_000_000_000L -> "%.1fT".format(value / 1_000_000_000_000.0)
    value >= 1_000_000_000L -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    else -> value.toString()
}
