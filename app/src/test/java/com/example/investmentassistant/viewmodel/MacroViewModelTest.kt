package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.investmentassistant.fake.FakeAiRepository
import com.example.investmentassistant.fake.FakeMacroRepository
import com.example.investmentassistant.fake.FakeReportRepository
import com.example.investmentassistant.model.TimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MacroViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var macroRepo: FakeMacroRepository
    private lateinit var aiRepo: FakeAiRepository
    private lateinit var reportRepo: FakeReportRepository
    private lateinit var viewModel: MacroViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        macroRepo = FakeMacroRepository()
        aiRepo = FakeAiRepository()
        reportRepo = FakeReportRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MacroViewModel(app, macroRepo, aiRepo, reportRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `초기 상태에서 지표 데이터 로드 성공`() = runTest {
        advanceUntilIdle()

        val indicators = viewModel.indicators.value
        assertEquals("4.50%", indicators.us10y.latestValue)
        assertEquals("5100", indicators.sp500.latestValue)
        assertNull(viewModel.fetchError.value)
    }

    @Test
    fun `데이터 로드 완료 후 isLoading이 false이고 에러 없음`() = runTest {
        advanceUntilIdle()

        assertTrue(!viewModel.isLoading.value)
        assertNull(viewModel.fetchError.value)
    }

    @Test
    fun `네트워크 오류 시 fetchError에 메시지 설정`() = runTest {
        macroRepo.shouldThrow = true
        macroRepo.throwMessage = "연결 시간 초과"

        viewModel.fetchMacroData()
        advanceUntilIdle()

        assertNotNull(viewModel.fetchError.value)
        assertTrue(viewModel.fetchError.value!!.contains("연결 시간 초과"))
        assertTrue(!viewModel.isLoading.value)
    }

    @Test
    fun `clearFetchError 호출 시 에러 초기화`() = runTest {
        macroRepo.shouldThrow = true
        viewModel.fetchMacroData()
        advanceUntilIdle()
        assertNotNull(viewModel.fetchError.value)

        viewModel.clearFetchError()
        assertNull(viewModel.fetchError.value)
    }

    @Test
    fun `재시도 성공 시 에러 클리어 후 데이터 로드`() = runTest {
        macroRepo.shouldThrow = true
        viewModel.fetchMacroData()
        advanceUntilIdle()
        assertNotNull(viewModel.fetchError.value)

        macroRepo.shouldThrow = false
        viewModel.fetchMacroData()
        advanceUntilIdle()

        assertNull(viewModel.fetchError.value)
        assertEquals("4.50%", viewModel.indicators.value.us10y.latestValue)
    }

    @Test
    fun `시간 범위 변경 시 데이터 재로드`() = runTest {
        advanceUntilIdle()
        assertEquals(TimeRange.M1, viewModel.selectedRange.value)

        viewModel.updateRange(TimeRange.W1)
        advanceUntilIdle()

        assertEquals(TimeRange.W1, viewModel.selectedRange.value)
        assertNull(viewModel.fetchError.value)
    }

    @Test
    fun `AI 인사이트 생성 성공`() = runTest {
        advanceUntilIdle()

        viewModel.generateAiInsight()
        advanceUntilIdle()

        assertEquals("테스트 매크로 인사이트", viewModel.aiInsight.value)
        assertEquals(200, reportRepo.totalTokensAdded)
        assertTrue(!viewModel.isInsightLoading.value)
    }

    @Test
    fun `AI 인사이트 생성 실패 시 에러 메시지 표시`() = runTest {
        advanceUntilIdle()
        aiRepo.shouldThrow = true
        aiRepo.throwMessage = "AI 서버 불가"

        viewModel.generateAiInsight()
        advanceUntilIdle()

        assertTrue(viewModel.aiInsight.value.contains("오류"))
        assertTrue(!viewModel.isInsightLoading.value)
    }

    @Test
    fun `중복 리포트는 저장하지 않음`() = runTest {
        advanceUntilIdle()

        viewModel.generateAiInsight()
        advanceUntilIdle()
        val countAfterFirst = reportRepo.savedReports.size

        viewModel.generateAiInsight()
        advanceUntilIdle()

        assertEquals(countAfterFirst, reportRepo.savedReports.size)
    }
}
