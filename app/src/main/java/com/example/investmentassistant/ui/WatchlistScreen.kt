package com.example.investmentassistant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.investmentassistant.api.FinnhubSymbolResult
import com.example.investmentassistant.data.repository.TradeSignal
import com.example.investmentassistant.model.WatchlistItem
import com.example.investmentassistant.viewmodel.TradeSignalState
import com.example.investmentassistant.viewmodel.WatchlistViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    bottomPadding: PaddingValues,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            showAddDialog = false
            viewModel.clearAddState()
        }
    }

    if (uiState.refreshError != null) {
        LaunchedEffect(uiState.refreshError) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearRefreshError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottomPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("종목 모니터링") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "새로고침")
                        }
                    }
                }
            )
            if (uiState.refreshError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = uiState.refreshError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("모니터링할 종목이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "+ 버튼으로 종목을 검색해 추가하세요",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        WatchlistCard(
                            item = item,
                            onDelete = { viewModel.removeItem(item.id) },
                            onAnalyze = { viewModel.analyzeItem(item) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Icon(Icons.Default.Add, "종목 추가")
        }
    }

    if (showAddDialog) {
        SearchAddDialog(
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                viewModel.clearAddState()
            },
        )
    }

    val tradeSignal by viewModel.tradeSignal.collectAsStateWithLifecycle()
    if (tradeSignal !is TradeSignalState.Idle) {
        TradeSignalDialog(
            state = tradeSignal,
            onDismiss = { viewModel.clearTradeSignal() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAddDialog(
    viewModel: WatchlistViewModel,
    onDismiss: () -> Unit,
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("종목 검색") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    label = { Text("종목명 또는 티커") },
                    placeholder = { Text("예: SK Hynix, Apple, NVDA, 005930") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (uiState.addError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.addError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (searchQuery.length < 2 && searchResults.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "2글자 이상 입력하면 자동으로 검색됩니다\n한국 주식: 000660.KS / 코스닥: .KQ\n지수: ^GSPC (S&P500), ^KS11 (코스피)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }

                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = { viewModel.addFromSearch(result) },
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}

@Composable
private fun SearchResultItem(result: FinnhubSymbolResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(result.displaySymbol.ifBlank { result.symbol }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(result.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (result.type.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    result.type,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun WatchlistCard(
    item: WatchlistItem,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit = {},
) {
    var showAnalysis by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(item.displayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.lastPrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${"%.2f".format(item.lastPrice)}", fontWeight = FontWeight.SemiBold)
                        val pct = item.lastChangePercent ?: 0.0
                        val color = if (pct >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Text(
                            "${if (pct >= 0) "▲" else "▼"}${"%.2f".format(Math.abs(pct))}%",
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Text(
                        if (item.lastCheckedAt > 0) "시세 없음" else "새로고침 필요",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onAnalyze,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("AI 진단", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (item.lastAlertMessage != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                val alertTime = DateTimeFormatter.ofPattern("MM/dd HH:mm")
                    .withZone(ZoneId.of("Asia/Seoul"))
                    .format(Instant.ofEpochMilli(item.lastAlertAt))
                Text(
                    "급변 감지 · $alertTime",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                if (showAnalysis) {
                    Text(item.lastAlertMessage, fontSize = 13.sp, lineHeight = 20.sp)
                    TextButton(onClick = { showAnalysis = false }, contentPadding = PaddingValues(0.dp)) {
                        Text("접기", fontSize = 12.sp)
                    }
                } else {
                    Text(
                        item.lastAlertMessage.take(80) + if (item.lastAlertMessage.length > 80) "..." else "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showAnalysis = true }, contentPadding = PaddingValues(0.dp)) {
                        Text("분석 전문 보기", fontSize = 12.sp)
                    }
                }
            }

            if (item.lastCheckedAt > 0) {
                Spacer(Modifier.height(4.dp))
                val checkedTime = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.of("Asia/Seoul"))
                    .format(Instant.ofEpochMilli(item.lastCheckedAt))
                Text("마지막 확인: $checkedTime", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TradeSignalDialog(
    state: TradeSignalState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 투자 진단") },
        text = {
            when (state) {
                TradeSignalState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("AI가 매크로 환경과 최근 뉴스를 분석 중입니다...", fontSize = 13.sp)
                    }
                }
                is TradeSignalState.Success -> {
                    val rec = state.recommendation
                    val (signalText, signalColor) = when (rec.signal) {
                        TradeSignal.BUY  -> "매수 ▲" to Color(0xFF1B5E20)
                        TradeSignal.SELL -> "매도 ▼" to Color(0xFFB71C1C)
                        TradeSignal.HOLD -> "보유 ─" to Color(0xFFE65100)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                color = signalColor,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    signalText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                            Text("신뢰도: ${rec.confidence}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(rec.summary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        HorizontalDivider()
                        Text(rec.reasoning, fontSize = 13.sp, lineHeight = 20.sp)
                        if (rec.targetPrice != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("목표가:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(rec.targetPrice, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚠️", fontSize = 11.sp)
                                Text(rec.riskNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                        Text(
                            "※ AI 분석은 투자 참고용이며 투자 결정의 책임은 본인에게 있습니다.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is TradeSignalState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                TradeSignalState.Idle -> {}
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}
