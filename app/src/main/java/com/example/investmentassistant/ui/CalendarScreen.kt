package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.model.CalendarEvent
import com.example.investmentassistant.model.EventImportance
import com.example.investmentassistant.model.EventType
import com.example.investmentassistant.viewmodel.CalendarUiState
import com.example.investmentassistant.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    bottomPadding: PaddingValues,
    viewModel: CalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("경제 캘린더") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        }
    ) { innerPadding ->
        val combinedPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = bottomPadding.calculateBottomPadding(),
        )
        CalendarContent(uiState = uiState, padding = combinedPadding)
    }
}

@Composable
private fun CalendarContent(uiState: CalendarUiState, padding: PaddingValues) {
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error, color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
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
                    items(uiState.todayEvents, key = { it.id }) { CalendarEventCard(it) }
                }
                if (uiState.tomorrowEvents.isNotEmpty()) {
                    item { Spacer(Modifier.height(4.dp)); SectionHeader("내일") }
                    items(uiState.tomorrowEvents, key = { it.id }) { CalendarEventCard(it) }
                }
                if (uiState.thisWeekEvents.isNotEmpty()) {
                    item { Spacer(Modifier.height(4.dp)); SectionHeader("이번 주") }
                    items(uiState.thisWeekEvents, key = { it.id }) { CalendarEventCard(it) }
                }
            }
        }
    }
}

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
private fun CalendarEventCard(event: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        "발표됨",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val timeStr = formatTime(event.scheduledAt)
            val timeLabel = if (event.type == EventType.EARNINGS && event.earningsTime?.isNotEmpty() == true)
                "${event.earningsTime}  $timeStr"
            else
                timeStr
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
                event.epsEstimate?.let { ValueChip("예상", "${"%.2f".format(it)}") }
                event.epsActual?.let { ValueChip("실제", "${"%.2f".format(it)}", highlight = true) }
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
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color,
        modifier = Modifier.size(8.dp),
    ) {}
}

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
    val instant = Instant.ofEpochMilli(epochMs)
    val kst = ZoneId.of("Asia/Seoul")
    val ldt = instant.atZone(kst)
    return if (ldt.hour == 0 && ldt.minute == 0) {
        "시간 미정"
    } else {
        DateTimeFormatter.ofPattern("HH:mm KST").format(ldt)
    }
}

private fun formatRevenue(value: Long): String = when {
    value >= 1_000_000_000_000L -> "${"%.1f".format(value / 1_000_000_000_000.0)}T"
    value >= 1_000_000_000L -> "${"%.1f".format(value / 1_000_000_000.0)}B"
    value >= 1_000_000L -> "${"%.1f".format(value / 1_000_000.0)}M"
    else -> value.toString()
}
