package com.example.investmentassistant.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TutorialStepData(val title: String, val description: String)

private val STEPS = listOf(
    TutorialStepData(
        "거시경제 대시보드",
        "VIX, S&P500, 금리 등 핵심 지표를 한눈에 확인하고, AI 매수·매도·보유 추천을 받아보세요.",
    ),
    TutorialStepData(
        "뉴스 검색",
        "키워드로 국내외 최신 금융 뉴스를 검색하고 AI 요약을 확인하세요.",
    ),
    TutorialStepData(
        "경제 캘린더",
        "금리 결정, CPI 등 주요 경제 지표 발표 일정과 실제 결과를 확인하고, 발표 시 알림을 받으세요.",
    ),
    TutorialStepData(
        "종목 모니터링",
        "관심 종목을 등록하고 실시간 가격 변동을 추적하세요. 급변 시 즉시 알림을 보내드립니다.",
    ),
    TutorialStepData(
        "리포트 보관함",
        "AI가 생성한 분석 리포트를 저장하고 언제든지 다시 불러볼 수 있습니다.",
    ),
    TutorialStepData(
        "키워드 구독",
        "관심 키워드를 등록하면 관련 최신 뉴스 알림을 자동으로 받을 수 있습니다.",
    ),
)

@Composable
fun TutorialOverlay(
    currentStep: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalSteps = STEPS.size
    val density = LocalDensity.current
    val systemNavBottomPx = WindowInsets.navigationBars.getBottom(density)
    val systemNavHeight = with(density) { systemNavBottomPx.toDp() }
    val navBarHeight = 80.dp
    val totalBottomHeight = navBarHeight + systemNavHeight

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val navBarHeightPx = with(density) { navBarHeight.toPx() }
        val systemNavHeightPx = with(density) { systemNavHeight.toPx() }
        val itemWidthPx = screenWidthPx / totalSteps

        val spotlightCx = itemWidthPx * currentStep + itemWidthPx / 2f
        val spotlightCy = screenHeightPx - systemNavHeightPx - navBarHeightPx / 2f
        val spotlightRadius = itemWidthPx * 0.44f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color(0xBB000000))
            drawCircle(
                color = Color.Transparent,
                radius = spotlightRadius,
                center = Offset(spotlightCx, spotlightCy),
                blendMode = BlendMode.Clear,
            )
        }

        val cardWidth = 288.dp
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val paddingPx = with(density) { 12.dp.toPx() }
        val idealLeftPx = spotlightCx - cardWidthPx / 2f
        val clampedLeftPx = idealLeftPx.coerceIn(paddingPx, screenWidthPx - cardWidthPx - paddingPx)
        val cardLeft = with(density) { clampedLeftPx.toDp() }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "tutorial_balloon",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = cardLeft, y = -(totalBottomHeight + 12.dp))
                .width(cardWidth),
        ) { stepIdx ->
            BalloonCard(
                step = STEPS[stepIdx],
                stepIdx = stepIdx,
                totalSteps = totalSteps,
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
private fun BalloonCard(
    step: TutorialStepData,
    stepIdx: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${stepIdx + 1} / $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text("건너뛰기", style = MaterialTheme.typography.labelMedium)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(totalSteps) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == stepIdx) 8.dp else 5.dp)
                                .background(
                                    color = if (i == stepIdx) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }

                FilledIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (stepIdx < totalSteps - 1) Icons.Default.NavigateNext else Icons.Default.Check,
                        contentDescription = if (stepIdx < totalSteps - 1) "다음" else "완료",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
