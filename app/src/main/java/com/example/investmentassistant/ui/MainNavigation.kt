package com.example.investmentassistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainAppScreen() {
    // 화면 이동을 통제하는 조종기(NavController)
    val navController = rememberNavController()

    // NavHost: 실제로 화면이 교체되는 무대
    NavHost(
        navController = navController,
        startDestination = "menu" // ★ 앱을 켜면 무조건 'menu' 화면이 먼저 나옵니다!
    ) {
        // 1. 대문 (메뉴) 화면
        composable("menu") {
            MenuScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToNews = { navController.navigate("news") }
            )
        }

        // 2. 대시보드 화면 (우리가 만든 파일)
        composable("dashboard") {
            MacroDashboardScreen()
        }

        // 3. 뉴스 검색 화면 (기존에 만드신 파일)
        composable("news") {
            NewsSearchScreen() // 주의: 기존 뉴스 화면의 함수 이름과 동일해야 합니다.
        }
    }
}

// ▼ 대문(메뉴) 화면의 UI를 그리는 함수 ▼
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
        }
    }
}