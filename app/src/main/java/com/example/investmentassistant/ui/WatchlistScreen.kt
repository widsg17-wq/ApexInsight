package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.investmentassistant.model.WatchlistItem
import com.example.investmentassistant.viewmodel.WatchlistViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    bottomPadding: PaddingValues,
    viewModel: WatchlistViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputSymbol by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            inputSymbol = ""
            showAddDialog = false
            viewModel.clearAddState()
        }
    }

    Scaffold(
        topBar = {
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "종목 추가")
            }
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("모니터링할 종목이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "+ 버튼으로 티커를 추가하세요\n예: AAPL, NVDA, 005930.KS, ^GSPC",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    WatchlistCard(item = item, onDelete = { viewModel.removeItem(item.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; viewModel.clearAddState() },
            title = { Text("종목 추가") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputSymbol,
                        onValueChange = { inputSymbol = it.uppercase() },
                        label = { Text("티커 입력") },
                        placeholder = { Text("예: AAPL, 005930.KS, ^GSPC") },
                        singleLine = true,
                        isError = uiState.addError != null,
                        supportingText = uiState.addError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addSymbol(inputSymbol) }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; viewModel.clearAddState() }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun WatchlistCard(item: WatchlistItem, onDelete: () -> Unit) {
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
                    Text("로딩 중...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
