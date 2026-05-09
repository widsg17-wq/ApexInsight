package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.data.WatchedKeyword
import com.example.investmentassistant.viewmodel.WatchedKeywordsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val INTERVAL_OPTIONS = listOf(1, 6, 12, 24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedKeywordsScreen(
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: WatchedKeywordsViewModel = viewModel(),
) {
    val keywords by viewModel.keywords.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("자동 리포트 구독", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            if (keywords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(bottom = bottomPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "구독 중인 키워드가 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "우하단 + 버튼을 눌러 키워드를 추가하세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "설정한 주기마다 자동으로 AI 리포트를 생성합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(bottom = bottomPadding.calculateBottomPadding()),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(keywords, key = { it.id }) { kw ->
                        KeywordCard(kw = kw, onDelete = { viewModel.deleteKeyword(kw) })
                    }
                }
            }
        }

        // FAB을 Box로 직접 위치시켜 바텀 네비게이션 바 위에 올바르게 표시
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .padding(bottom = bottomPadding.calculateBottomPadding() + 16.dp)
                .zIndex(1f),
        ) {
            Icon(Icons.Default.Add, contentDescription = "키워드 추가")
        }
    }

    if (showAddDialog) {
        AddKeywordDialog(
            onConfirm = { keyword, interval ->
                viewModel.addKeyword(keyword, interval)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun KeywordCard(kw: WatchedKeyword, onDelete: () -> Unit) {
    val lastRunText = if (kw.lastRunAt == 0L) "아직 실행 안 됨"
    else SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(kw.lastRunAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kw.keyword,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "매 ${kw.intervalHours}시간마다 자동 수집",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "마지막 실행: $lastRunText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddKeywordDialog(onConfirm: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var selectedInterval by remember { mutableStateOf(24) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("키워드 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("키워드 (예: 금리 인하)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("수집 주기", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    INTERVAL_OPTIONS.forEach { hours ->
                        FilterChip(
                            selected = selectedInterval == hours,
                            onClick = { selectedInterval = hours },
                            label = { Text("${hours}시간") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(keyword, selectedInterval) },
                enabled = keyword.isNotBlank(),
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
