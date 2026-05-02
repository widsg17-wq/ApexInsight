package com.example.investmentassistant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.investmentassistant.model.NewsArticle
import com.example.investmentassistant.viewmodel.DateRange
import com.example.investmentassistant.viewmodel.NewsViewModel
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val newsList by viewModel.newsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val generatedReport by viewModel.generatedReport.collectAsState()
    val context = LocalContext.current
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val customDays by viewModel.customDays.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("투자 키워드 검색 (예: 금리 인하)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchNews(context) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateRange.values().forEach { range ->
                FilterChip(
                    selected = selectedDateRange == range,
                    onClick = { viewModel.updateDateRange(range) },
                    label = { Text(range.label) }
                )
            }

            if (selectedDateRange == DateRange.CUSTOM) {
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { viewModel.updateCustomDays(it) },
                    label = { Text("일수") },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("일 전")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            // ★ onClick에 context 전달
            onClick = { viewModel.searchNews(context) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && searchQuery.isNotBlank()
        ) {
            Text("AI 리포트 생성 및 검색")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI 애널리스트가 리포트를 작성 중입니다...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (generatedReport.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI 투자 리포트",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RichText {
                                    Markdown(generatedReport)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "참고 기사 출처",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                items(newsList) { article ->
                    NewsArticleCard(article = article)
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(article: NewsArticle) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (article.url.isNotBlank()) {
                    try {
                        uriHandler.openUri(article.url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = article.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = article.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}