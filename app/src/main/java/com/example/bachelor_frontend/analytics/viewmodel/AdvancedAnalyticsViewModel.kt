// AdvancedAnalyticsViewModel.kt
package com.example.bachelor_frontend.analytics.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachelor_frontend.analytics.models.*
import com.example.bachelor_frontend.analytics.service.AdvancedAnalyticsService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

class AdvancedAnalyticsViewModel : ViewModel() {
    private val analyticsService = AdvancedAnalyticsService()
    private val auth = FirebaseAuth.getInstance()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Cash Flow Projection
    private val _cashFlowProjection = MutableStateFlow<CashFlowProjection?>(null)
    val cashFlowProjection: StateFlow<CashFlowProjection?> = _cashFlowProjection.asStateFlow()

    // Tax Optimization
    private val _taxOptimizationReport = MutableStateFlow<TaxOptimizationReport?>(null)
    val taxOptimizationReport: StateFlow<TaxOptimizationReport?> = _taxOptimizationReport.asStateFlow()

    // Retirement Planning
    private val _retirementPlan = MutableStateFlow<RetirementPlan?>(null)
    val retirementPlan: StateFlow<RetirementPlan?> = _retirementPlan.asStateFlow()

    // Custom Reports
    private val _customReports = MutableStateFlow<List<CustomReport>>(emptyList())
    val customReports: StateFlow<List<CustomReport>> = _customReports.asStateFlow()

    private val _currentReport = MutableStateFlow<CustomReport?>(null)
    val currentReport: StateFlow<CustomReport?> = _currentReport.asStateFlow()

    // Financial Metrics
    private val _financialMetrics = MutableStateFlow<FinancialMetrics?>(null)
    val financialMetrics: StateFlow<FinancialMetrics?> = _financialMetrics.asStateFlow()

    // Spending Patterns
    private val _spendingPatterns = MutableStateFlow<Map<String, Any>>(emptyMap())
    val spendingPatterns: StateFlow<Map<String, Any>> = _spendingPatterns.asStateFlow()

    companion object {
        private const val TAG = "AdvancedAnalyticsViewModel"
    }

    /**
     * Generate cash flow forecast
     */
    fun generateCashFlowForecast(months: Int = 12) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Generating cash flow forecast for $months months")
                val projection = analyticsService.generateCashFlowForecast(userId, months)
                _cashFlowProjection.value = projection

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, projection)

            } catch (e: Exception) {
                Log.e(TAG, "Error generating cash flow forecast", e)
                _error.value = "Failed to generate cash flow forecast: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Analyze tax optimization opportunities
     */
    fun analyzeTaxOptimization(taxYear: Int = java.time.Year.now().value) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Analyzing tax optimization for year $taxYear")
                val report = analyticsService.analyzeTaxOptimization(userId, taxYear)
                _taxOptimizationReport.value = report

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, report)

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing tax optimization", e)
                _error.value = "Failed to analyze tax optimization: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate retirement projections
     */
    fun calculateRetirementProjections(
        currentAge: Int,
        targetRetirementAge: Int = 65,
        currentSavings: Double = 0.0,
        monthlyContribution: Double = 0.0
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Calculating retirement projections")
                val plan = analyticsService.calculateRetirementProjections(
                    userId, currentAge, targetRetirementAge, currentSavings, monthlyContribution
                )
                _retirementPlan.value = plan

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, plan)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating retirement projections", e)
                _error.value = "Failed to calculate retirement projections: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate custom report
     */
    fun generateCustomReport(criteria: ReportCriteria) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Generating custom report: ${criteria.reportType}")
                val report = analyticsService.generateCustomReport(userId, criteria)
                _currentReport.value = report

                // Add to reports list
                val updatedReports = _customReports.value.toMutableList()
                updatedReports.add(0, report) // Add to beginning
                _customReports.value = updatedReports

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, report)

            } catch (e: Exception) {
                Log.e(TAG, "Error generating custom report", e)
                _error.value = "Failed to generate custom report: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate financial metrics for current period
     */
    fun calculateFinancialMetrics(period: YearMonth = YearMonth.now()) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Calculating financial metrics for $period")
                val metrics = analyticsService.calculateFinancialMetrics(userId, period)
                _financialMetrics.value = metrics

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, metrics)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating financial metrics", e)
                _error.value = "Failed to calculate financial metrics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Analyze spending patterns
     */
    fun analyzeSpendingPatterns() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Analyzing spending patterns")
                val patterns = analyticsService.analyzeSpendingPatterns(userId)
                _spendingPatterns.value = patterns

                // Save for historical tracking
                analyticsService.saveAnalyticsData(userId, patterns)

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing spending patterns", e)
                _error.value = "Failed to analyze spending patterns: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get quick spending analysis report
     */
    fun getQuickSpendingAnalysis() {
        val criteria = ReportCriteria(
            reportType = ReportType.SPENDING_ANALYSIS,
            dateRange = DateRange(
                startDate = java.time.LocalDateTime.now().minusMonths(3),
                endDate = java.time.LocalDateTime.now()
            ),
            groupBy = GroupBy.MONTHLY,
            includeComparisons = true
        )
        generateCustomReport(criteria)
    }

    /**
     * Get budget performance report
     */
    fun getBudgetPerformanceReport() {
        val criteria = ReportCriteria(
            reportType = ReportType.BUDGET_PERFORMANCE,
            dateRange = DateRange(
                startDate = java.time.LocalDateTime.now().minusMonths(6),
                endDate = java.time.LocalDateTime.now()
            ),
            groupBy = GroupBy.MONTHLY,
            includeComparisons = true,
            includeProjections = true
        )
        generateCustomReport(criteria)
    }

    /**
     * Get category trends report
     */
    fun getCategoryTrendsReport() {
        val criteria = ReportCriteria(
            reportType = ReportType.CATEGORY_TRENDS,
            dateRange = DateRange(
                startDate = java.time.LocalDateTime.now().minusYears(1),
                endDate = java.time.LocalDateTime.now()
            ),
            groupBy = GroupBy.MONTHLY,
            includeComparisons = true
        )
        generateCustomReport(criteria)
    }

    /**
     * Get tax summary report
     */
    fun getTaxSummaryReport(taxYear: Int = java.time.Year.now().value) {
        val criteria = ReportCriteria(
            reportType = ReportType.TAX_SUMMARY,
            dateRange = DateRange(
                startDate = java.time.LocalDateTime.of(taxYear, 1, 1, 0, 0),
                endDate = java.time.LocalDateTime.of(taxYear, 12, 31, 23, 59)
            ),
            groupBy = GroupBy.CATEGORY
        )
        generateCustomReport(criteria)
    }

    /**
     * Load all analytics data
     */
    fun loadAllAnalytics() {
        calculateFinancialMetrics()
        analyzeSpendingPatterns()
        getQuickSpendingAnalysis()
    }

    /**
     * Refresh current data
     */
    fun refreshAnalytics() {
        _cashFlowProjection.value?.let {
            generateCashFlowForecast(it.projectionPeriod)
        }

        _taxOptimizationReport.value?.let {
            analyzeTaxOptimization(it.taxYear)
        }

        _retirementPlan.value?.let { plan ->
            calculateRetirementProjections(
                plan.currentAge,
                plan.targetRetirementAge,
                plan.currentSavings,
                plan.monthlyContribution
            )
        }

        _financialMetrics.value?.let {
            calculateFinancialMetrics(it.period)
        }

        analyzeSpendingPatterns()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Select a specific report to view
     */
    fun selectReport(report: CustomReport) {
        _currentReport.value = report
    }

    /**
     * Delete a custom report
     */
    fun deleteReport(reportId: String) {
        val updatedReports = _customReports.value.filter { it.id != reportId }
        _customReports.value = updatedReports

        // If the deleted report was currently selected, clear selection
        if (_currentReport.value?.id == reportId) {
            _currentReport.value = null
        }
    }

    /**
     * Export report data
     */
    fun exportReport(report: CustomReport): String {
        // Simple CSV export format
        val sb = StringBuilder()

        // Header
        sb.appendLine("${report.title} - Generated on ${report.generatedAt}")
        sb.appendLine()

        // Summary data
        sb.appendLine("Summary:")
        report.data.summary.forEach { (key, value) ->
            sb.appendLine("$key: $value")
        }
        sb.appendLine()

        // Breakdown data
        report.data.breakdown.forEach { (category, data) ->
            sb.appendLine("$category:")
            data.forEach { (key, value) ->
                sb.appendLine("  $key: $value")
            }
            sb.appendLine()
        }

        // Insights
        if (report.insights.isNotEmpty()) {
            sb.appendLine("Key Insights:")
            report.insights.forEach { insight ->
                sb.appendLine("- ${insight.title}: ${insight.description}")
            }
        }

        return sb.toString()
    }

    /**
     * Get financial health score
     */
    fun getFinancialHealthScore(): Double {
        val metrics = _financialMetrics.value ?: return 0.0

        var score = 0.0
        var factors = 0

        // Savings rate (30% weight)
        if (metrics.savingsRate >= 20) score += 30
        else if (metrics.savingsRate >= 10) score += 20
        else if (metrics.savingsRate >= 5) score += 10
        factors++

        // Emergency fund (25% weight)
        if (metrics.emergencyFundMonths >= 6) score += 25
        else if (metrics.emergencyFundMonths >= 3) score += 15
        else if (metrics.emergencyFundMonths >= 1) score += 10
        factors++

        // Debt to income ratio (25% weight)
        if (metrics.debtToIncomeRatio <= 0.1) score += 25
        else if (metrics.debtToIncomeRatio <= 0.3) score += 15
        else if (metrics.debtToIncomeRatio <= 0.5) score += 10
        factors++

        // Risk management (20% weight)
        if (metrics.riskScore <= 30) score += 20
        else if (metrics.riskScore <= 50) score += 15
        else if (metrics.riskScore <= 70) score += 10
        factors++

        return if (factors > 0) score else 0.0
    }

    /**
     * Get personalized recommendations based on analytics
     */
    fun getPersonalizedRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _financialMetrics.value
        val patterns = _spendingPatterns.value

        metrics?.let { m ->
            // Savings rate recommendations
            if (m.savingsRate < 10) {
                recommendations.add("Try to increase your savings rate to at least 10% of your income")
            }

            // Emergency fund recommendations
            if (m.emergencyFundMonths < 3) {
                recommendations.add("Build an emergency fund covering at least 3 months of expenses")
            }

            // Risk recommendations
            if (m.riskScore > 70) {
                recommendations.add("Your spending pattern shows high risk - consider creating a more stable budget")
            }
        }

        // Spending pattern recommendations
        if (patterns.isNotEmpty()) {
            val volatility = patterns["spendingVolatility"] as? Double
            if (volatility != null && volatility > 0.3) {
                recommendations.add("Your spending varies significantly month to month - try to create more consistent spending habits")
            }
        }

        val projection = _cashFlowProjection.value
        projection?.let { p ->
            if (p.summary.averageMonthlySavings < 0) {
                recommendations.add("Your projected spending exceeds income - review your budget and reduce expenses")
            }
        }

        return recommendations
    }
}