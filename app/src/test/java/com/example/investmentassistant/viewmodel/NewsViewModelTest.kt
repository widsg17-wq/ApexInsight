package com.example.investmentassistant.viewmodel

import com.example.investmentassistant.fake.FakeAiRepository
import com.example.investmentassistant.fake.FakeNewsRepository
import com.example.investmentassistant.fake.FakeReportRepository
import com.example.investmentassistant.model.DateRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var newsRepo: FakeNewsRepository
    private lateinit var aiRepo: FakeAiRepository
    private lateinit var reportRepo: FakeReportRepository
    private lateinit var viewModel: NewsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        newsRepo = FakeNewsRepository()
        aiRepo = FakeAiRepository()
        reportRepo = FakeReportRepository()
        viewModel = NewsViewModel(newsRepo, aiRepo, reportRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `초기 상태는 Idle`() {
        assertTrue(viewModel.uiState.value is NewsUiState.Idle)
    }

    @Test
    fun `빈 검색어로 searchNews 호출 시 상태 변화 없음`() = runTest {
        viewModel.updateSearchQuery("")
        viewModel.searchNews()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is NewsUiState.Idle)
    }

    @Test
    fun `검색 성공 시 Success 상태로 변경`() = runTest {
        viewModel.updateSearchQuery("테슬라")
        viewModel.searchNews()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NewsUiState.Success)
        val success = state as NewsUiState.Success
        assertEquals(2, success.articles.size)
        assertEquals("테스트 뉴스 리포트", success.report)
    }

    @Test
    fun `검색 완료 후 Idle 아닌 상태로 전환됨`() = runTest {
        viewModel.updateSearchQuery("삼성")
        viewModel.searchNews()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value !is NewsUiState.Idle)
    }

    @Test
    fun `뉴스 API 오류 시 Error 상태로 변경`() = runTest {
        newsRepo.shouldThrow = true
        newsRepo.throwMessage = "API 한도 초과"

        viewModel.updateSearchQuery("애플")
        viewModel.searchNews()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NewsUiState.Error)
        assertTrue((state as NewsUiState.Error).message.contains("API 한도 초과"))
    }

    @Test
    fun `뉴스 없을 때 빈 리포트 반환`() = runTest {
        newsRepo.articles = emptyList()

        viewModel.updateSearchQuery("존재하지않는키워드")
        viewModel.searchNews()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NewsUiState.Success
        assertTrue(state.articles.isEmpty())
        assertTrue(state.report.isEmpty())
    }

    @Test
    fun `검색 성공 시 리포트 저장됨`() = runTest {
        viewModel.updateSearchQuery("엔비디아")
        viewModel.searchNews()
        advanceUntilIdle()

        assertEquals(1, reportRepo.savedReports.size)
        assertEquals("NEWS", reportRepo.savedReports[0].type)
        assertEquals("엔비디아", reportRepo.savedReports[0].title)
    }

    @Test
    fun `토큰 사용량이 ReportRepository에 누적됨`() = runTest {
        viewModel.updateSearchQuery("마이크로소프트")
        viewModel.searchNews()
        advanceUntilIdle()

        assertEquals(100, reportRepo.totalTokensAdded)
    }

    @Test
    fun `날짜 범위 변경 반영`() = runTest {
        viewModel.updateDateRange(DateRange.PAST_WEEK)
        assertEquals(DateRange.PAST_WEEK, viewModel.selectedDateRange.value)

        viewModel.updateDateRange(DateRange.PAST_MONTH)
        assertEquals(DateRange.PAST_MONTH, viewModel.selectedDateRange.value)
    }

    @Test
    fun `커스텀 날짜 숫자만 허용`() {
        viewModel.updateCustomDays("7")
        assertEquals("7", viewModel.customDays.value)

        viewModel.updateCustomDays("abc")
        assertEquals("7", viewModel.customDays.value)

        viewModel.updateCustomDays("14")
        assertEquals("14", viewModel.customDays.value)
    }

    @Test
    fun `중복 리포트는 저장하지 않음`() = runTest {
        viewModel.updateSearchQuery("구글")
        viewModel.searchNews()
        advanceUntilIdle()
        assertEquals(1, reportRepo.savedReports.size)

        viewModel.searchNews()
        advanceUntilIdle()
        assertEquals(1, reportRepo.savedReports.size)
    }
}
