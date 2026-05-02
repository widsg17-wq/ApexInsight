package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.investmentassistant.utils.TokenManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import com.example.investmentassistant.data.AppDatabase
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "menu"
    ) {
        // 1. 대문 화면
        composable("menu") {
            MenuScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToNews = { navController.navigate("news") },
                // ★ 에러 원인 해결: 보관함 이동 연결선 추가!
                onNavigateToSaved = { navController.navigate("saved") }
            )
        }
        // 2. 대시보드 화면
        composable("dashboard") {
            MacroDashboardScreen()
        }
        // 3. 뉴스 화면
        composable("news") {
            NewsSearchScreen()
        }
        // 4. ★ 신규 추가: 보관함 화면
        composable("saved") {
            SavedReportsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

// ▼ 대문(메뉴) 화면 UI ▼
@Composable
fun MenuScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToSaved: () -> Unit // ★ 파라미터 추가
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "ApexInsight",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "당신의 AI 투자 비서",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
            ) {
                Text("📊 매크로 대시보드", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToNews,
                modifier = Modifier.fillMaxWidth(0.7f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("📰 뉴스 검색 및 요약", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ★ 보관함 이동 버튼
            OutlinedButton(
                onClick = onNavigateToSaved,
                modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)
            ) {
                Text("🗂️ 나의 리포트 보관함", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.weight(1f))
            TokenUsageButton()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ▼ 토큰 팝업 컴포넌트 ▼
@Composable
fun TokenUsageButton() {
    var showDialog by remember { mutableStateOf(false) }

    // ★ DB에서 실시간으로 토큰 기록 가져오기
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val tokenRecords by db.tokenDao().getAllTokenRecords().collectAsState(initial = emptyList())

    TextButton(onClick = { showDialog = true }) {
        Text("📊 일별 AI 토큰 사용량 확인", style = MaterialTheme.typography.labelLarge)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("일별 토큰 사용 현황", fontWeight = FontWeight.Bold) },
            text = {
                if (tokenRecords.isEmpty()) {
                    Text("아직 사용 기록이 없습니다.")
                } else {
                    // 리스트 형태로 날짜 - 사용량 띄우기
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(tokenRecords) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(record.date, fontWeight = FontWeight.Medium)
                                Text("🔥 ${record.totalTokens}", color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("닫기") }
            }
        )
    }
}