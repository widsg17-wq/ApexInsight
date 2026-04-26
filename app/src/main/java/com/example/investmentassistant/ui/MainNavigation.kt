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
// ★ TokenManager 임포트 추가!
import com.example.investmentassistant.utils.TokenManager

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "menu"
    ) {
        composable("menu") {
            MenuScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToNews = { navController.navigate("news") }
            )
        }
        composable("dashboard") {
            MacroDashboardScreen()
        }
        composable("news") {
            NewsSearchScreen()
        }
    }
}

// ▼ 대문(메뉴) 화면 UI ▼
@Composable
fun MenuScreen(onNavigateToDashboard: () -> Unit, onNavigateToNews: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 위쪽 여백으로 중앙을 약간 아래로 밀어줌
            Spacer(modifier = Modifier.weight(1f))

            // 앱 타이틀
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

            // 대시보드 이동 버튼
            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp)
            ) {
                Text("📊 매크로 대시보드", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 뉴스 이동 버튼
            Button(
                onClick = onNavigateToNews,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📰 뉴스 검색 및 요약", style = MaterialTheme.typography.titleMedium)
            }

            // ★ 아래쪽 남은 공간을 밀어내서 버튼을 맨 밑으로 보냄
            Spacer(modifier = Modifier.weight(1f))

            // ★ 여기에 토큰 확인 팝업 버튼 추가!
            TokenUsageButton()

            // 화면 맨 밑바닥 여백
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ▼ 토큰 사용량을 보여주는 팝업 버튼 컴포넌트 ▼
@Composable
fun TokenUsageButton() {
    var showDialog by remember { mutableStateOf(false) }
    val totalTokens by TokenManager.totalTokens.collectAsState()

    TextButton(
        onClick = { showDialog = true }
    ) {
        Text("📊 AI 토큰 누적 사용량 확인", style = MaterialTheme.typography.labelLarge)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("AI 토큰 사용 현황", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "앱 실행 후 현재까지 뉴스 분석과 매크로 리포트에 사용된 총 토큰은\n\n" +
                            "🔥 $totalTokens Tokens\n\n" +
                            "입니다.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}