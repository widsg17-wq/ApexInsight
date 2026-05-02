package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.SavedReport
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedReportsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val coroutineScope = rememberCoroutineScope()

    // ★ 핵심: 창고(DB)에 있는 모든 데이터를 실시간으로 가져옵니다.
    val reports by db.reportDao().getAllReports().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("나의 리포트 보관함", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        if (reports.isEmpty()) {
            // 저장된 리포트가 없을 때 보여줄 화면
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("아직 저장된 리포트가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // 저장된 리포트가 있을 때: 스크롤 가능한 리스트(LazyColumn)로 띄워줍니다.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reports) { report ->
                    ReportCard(report = report, onDelete = {
                        coroutineScope.launch { db.reportDao().deleteReport(report) }
                    })
                }
            }
        }
    }
}

// 리포트 1개를 예쁘게 보여주는 카드 디자인
@Composable
fun ReportCard(report: SavedReport, onDelete: () -> Unit) {
    // 카드가 펼쳐졌는지 여부를 기억하는 상태 변수
    var expanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
    val dateString = dateFormat.format(Date(report.savedAt))

    Card(
        // ★ 클릭할 때마다 expanded 상태를 반전시킴!
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 생성 날짜와 테마(제목)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title, // 테마 노출
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString, // 생성 날짜 노출
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 오른쪽: 삭제 버튼
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                }
            }

            // ★ 카드를 터치해서 expanded가 true가 되면 내용이 펼쳐짐!
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = report.content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}