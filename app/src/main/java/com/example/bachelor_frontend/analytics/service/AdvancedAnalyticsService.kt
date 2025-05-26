// AdvancedAnalyticsService.kt
package com.example.bachelor_frontend.analytics.service

import android.util.Log
import com.example.bachelor_frontend.analytics.models.*
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.*

class AdvancedAnalyticsService {
    private val db = FirebaseFirestore.getInstance()
    private val expenseRepository = ExpenseRepository()

    companion object {
        private const val TAG = "AdvancedAnalyticsService"
        private const val ANALYTICS_COLLECTION = "analytics"
        private const val USER_METRICS_COLLECTION = "user_metrics"
    }

    /**
     * Generate comprehensive cash flow forecast
     */
    suspend fun generateCashFlowForecast(
        userId: String,
        months: Int = 12
    ): CashFlowProjection = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating cash flow forecast for $userId, $months months")

            // Get historical expenses for analysis
            val expenses = expenseRepository.getExpensesByUser(userId)
            val historicalData = analyzeHistoricalSpending(expenses)

            // Get user's income data (if available)
            val incomeData = getUserIncomeData(userId)

            val monthlyProjections = mutableListOf<MonthlyProjection>()
            var totalIncome = 0.0
            var totalExpenses = 0.0

            for (i in 1..months) {
                val targetMonth = YearMonth.now().plusMonths(i.toLong())
                val projection = generateMonthlyProjection(
                    targetMonth,
                    historicalData,
                    incomeData
                )
                monthlyProjections.add(projection)
                totalIncome += projection.projectedIncome
                totalExpenses += projection.projectedExpenses
            }

            val totalSavings = totalIncome - totalExpenses
            val avgMonthlySavings = totalSavings / months
            val confidence = calculateForecastConfidence(historicalData, months)

            val summary = CashFlowSummary(
                totalProjectedIncome = totalIncome,
                totalProjectedExpenses = totalExpenses,
                totalProjectedSavings = totalSavings,
                averageMonthlySavings = avgMonthlySavings,
                worstCaseScenario = totalSavings * 0.7, // Conservative estimate
                bestCaseScenario = totalSavings * 1.3   // Optimistic estimate
            )

            CashFlowProjection(
                userId = userId,
                projectionPeriod = months,
                monthlyProjections = monthlyProjections,
                summary = summary,
                confidence = confidence
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating cash flow forecast", e)
            throw e
        }
    }

    /**
     * Analyze tax optimization opportunities
     */
    suspend fun analyzeTaxOptimization(
        userId: String,
        taxYear: Int = LocalDateTime.now().year
    ): TaxOptimizationReport = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Analyzing tax optimization for $userId, year $taxYear")

            val expenses = getExpensesForTaxYear(userId, taxYear)
            val deductibleExpenses = identifyDeductibleExpenses(expenses)
            val categorizedDeductions = categorizeDeductions(deductibleExpenses)
            val estimatedSavings = calculateEstimatedTaxSavings(categorizedDeductions)
            val recommendations =
                generateTaxRecommendations(deductibleExpenses, categorizedDeductions)

            TaxOptimizationReport(
                userId = userId,
                taxYear = taxYear,
                deductibleExpenses = deductibleExpenses,
                categorizedDeductions = categorizedDeductions,
                estimatedTaxSavings = estimatedSavings,
                recommendations = recommendations
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing tax optimization", e)
            throw e
        }
    }

    /**
     * Calculate retirement projections
     */
    suspend fun calculateRetirementProjections(
        userId: String,
        currentAge: Int,
        targetRetirementAge: Int = 65,
        currentSavings: Double = 0.0,
        monthlyContribution: Double = 0.0
    ): RetirementPlan = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Calculating retirement projections for $userId")

            val yearsToRetirement = targetRetirementAge - currentAge
            val monthsToRetirement = yearsToRetirement * 12

            // Calculate compound growth with realistic assumptions
            val annualReturn = 0.07 // 7% annual return assumption
            val monthlyReturn = annualReturn / 12
            val inflationRate = 0.03 // 3% annual inflation

            // Future value calculation
            val futureValueOfCurrent = currentSavings * (1 + annualReturn).pow(yearsToRetirement)
            val futureValueOfContributions = if (monthlyContribution > 0) {
                monthlyContribution * ((1 + monthlyReturn).pow(monthsToRetirement) - 1) / monthlyReturn
            } else 0.0

            val totalRetirementFund = futureValueOfCurrent + futureValueOfContributions

            // Safe withdrawal rate (4% rule)
            val monthlyRetirementIncome = (totalRetirementFund * 0.04) / 12
            val inflationAdjustedIncome =
                monthlyRetirementIncome / (1 + inflationRate).pow(yearsToRetirement)

            val recommendations = generateRetirementRecommendations(
                currentAge, targetRetirementAge, totalRetirementFund, monthlyContribution
            )

            val scenarios = generateRetirementScenarios(
                currentAge, targetRetirementAge, currentSavings, monthlyContribution
            )

            RetirementPlan(
                userId = userId,
                currentAge = currentAge,
                targetRetirementAge = targetRetirementAge,
                currentSavings = currentSavings,
                monthlyContribution = monthlyContribution,
                projectedRetirementFund = totalRetirementFund,
                monthlyRetirementIncome = monthlyRetirementIncome,
                inflationAdjustedIncome = inflationAdjustedIncome,
                recommendations = recommendations,
                scenarios = scenarios
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating retirement projections", e)
            throw e
        }
    }

    /**
     * Generate custom reports based on criteria
     */
    suspend fun generateCustomReport(
        userId: String,
        criteria: ReportCriteria
    ): CustomReport = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating custom report for $userId")

            val expenses = getExpensesForDateRange(userId, criteria.dateRange)
            val reportData = when (criteria.reportType) {
                ReportType.SPENDING_ANALYSIS -> generateSpendingAnalysis(expenses, criteria)
                ReportType.BUDGET_PERFORMANCE -> generateBudgetPerformanceReport(
                    userId,
                    expenses,
                    criteria
                )

                ReportType.CATEGORY_TRENDS -> generateCategoryTrendsReport(expenses, criteria)
                ReportType.SAVINGS_ANALYSIS -> generateSavingsAnalysisReport(
                    userId,
                    expenses,
                    criteria
                )

                ReportType.TAX_SUMMARY -> generateTaxSummaryReport(expenses, criteria)
                else -> generateGenericReport(expenses, criteria)
            }

            val visualizations = generateChartData(reportData, criteria)
            val insights = generateReportInsights(reportData, criteria)

            CustomReport(
                id = generateReportId(),
                userId = userId,
                title = generateReportTitle(criteria),
                criteria = criteria,
                data = reportData,
                visualizations = visualizations,
                insights = insights
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating custom report", e)
            throw e
        }
    }

    /**
     * Calculate advanced financial metrics
     */
    suspend fun calculateFinancialMetrics(
        userId: String,
        period: YearMonth = YearMonth.now()
    ): FinancialMetrics = withContext(Dispatchers.IO) {
        try {
            val startDate = period.atDay(1).atStartOfDay()
            val endDate = period.atEndOfMonth().atTime(23, 59, 59)

            val expenses = getExpensesForDateRange(userId, DateRange(startDate, endDate))
            val income = getUserIncomeForPeriod(userId, period)
            val totalExpenses = expenses.sumOf { it.amount }

            val savingsRate = if (income > 0) ((income - totalExpenses) / income) * 100 else 0.0
            val burnRate = totalExpenses
            val emergencyFund = getUserEmergencyFund(userId)
            val emergencyFundMonths = if (totalExpenses > 0) emergencyFund / totalExpenses else 0.0

            // Calculate other metrics
            val debtToIncomeRatio = calculateDebtToIncomeRatio(userId, income)
            val riskScore = calculateRiskScore(expenses, income)
            val volatilityIndex = calculateSpendingVolatility(expenses)

            FinancialMetrics(
                userId = userId,
                period = period,
                savingsRate = savingsRate,
                burnRate = burnRate,
                emergencyFundMonths = emergencyFundMonths,
                debtToIncomeRatio = debtToIncomeRatio,
                investmentAllocation = getUserInvestmentAllocation(userId),
                riskScore = riskScore,
                volatilityIndex = volatilityIndex,
                diversificationScore = calculateDiversificationScore(userId)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating financial metrics", e)
            throw e
        }
    }

    // Private helper methods

    private suspend fun analyzeHistoricalSpending(expenses: List<ExpenseDto>): Map<String, Double> {
        return expenses.groupBy { it.category }
            .mapValues { (_, categoryExpenses) ->
                categoryExpenses.sumOf { it.amount } / max(
                    1,
                    getUniqueMonthsCount(categoryExpenses)
                )
            }
    }

    private fun getUniqueMonthsCount(expenses: List<ExpenseDto>): Int {
        return expenses.map { YearMonth.from(it.timestamp) }.toSet().size
    }

    private suspend fun getUserIncomeData(userId: String): Double {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getDouble("monthlyIncome") ?: 3000.0 // Default assumption
        } catch (e: Exception) {
            3000.0 // Default fallback
        }
    }

    private fun generateMonthlyProjection(
        month: YearMonth,
        historicalData: Map<String, Double>,
        income: Double
    ): MonthlyProjection {
        val seasonalityFactor = getSeasonalityFactor(month)
        val categoryBreakdown = historicalData.mapValues { (_, avgSpending) ->
            avgSpending * seasonalityFactor * (0.9 + Math.random() * 0.2) // Add some variance
        }

        val totalExpenses = categoryBreakdown.values.sum()
        val savings = income - totalExpenses

        return MonthlyProjection(
            month = month,
            projectedIncome = income,
            projectedExpenses = totalExpenses,
            projectedSavings = savings,
            categoryBreakdown = categoryBreakdown,
            confidence = 0.75 // Base confidence level
        )
    }

    private fun getSeasonalityFactor(month: YearMonth): Double {
        return when (month.month.value) {
            12, 1 -> 1.2  // Holiday season - higher spending
            7, 8 -> 1.1   // Summer vacation - higher spending
            3, 9 -> 0.95  // Back to school/work - moderate spending
            else -> 1.0   // Normal spending
        }
    }

    private fun calculateForecastConfidence(
        historicalData: Map<String, Double>,
        forecastMonths: Int
    ): Double {
        val dataPoints = historicalData.size
        val baseConfidence = min(0.9, dataPoints / 12.0) // More data = higher confidence
        val timePenalty = max(0.1, 1.0 - (forecastMonths * 0.05)) // Further out = less confident
        return baseConfidence * timePenalty
    }

    private suspend fun getExpensesForTaxYear(userId: String, taxYear: Int): List<ExpenseDto> {
        val startDate = LocalDateTime.of(taxYear, 1, 1, 0, 0)
        val endDate = LocalDateTime.of(taxYear, 12, 31, 23, 59)
        return expenseRepository.getExpensesByDateRange(userId, startDate, endDate)
    }

    private fun identifyDeductibleExpenses(expenses: List<ExpenseDto>): List<DeductibleExpense> {
        return expenses.mapNotNull { expense ->
            val deductionType = when (expense.category.lowercase()) {
                "healthcare", "medical" -> TaxDeductionType.MEDICAL_EXPENSE
                "business", "office" -> TaxDeductionType.BUSINESS_EXPENSE
                "charity", "donation" -> TaxDeductionType.CHARITABLE_DONATION
                "education" -> TaxDeductionType.EDUCATION
                "travel" -> TaxDeductionType.TRAVEL
                else -> null
            }

            deductionType?.let {
                DeductibleExpense(
                    expenseId = expense.id,
                    amount = expense.amount,
                    category = expense.category,
                    deductionType = it,
                    description = expense.description,
                    date = expense.timestamp,
                    confidence = calculateDeductionConfidence(expense, it)
                )
            }
        }
    }

    private fun calculateDeductionConfidence(expense: ExpenseDto, type: TaxDeductionType): Double {
        // Simple confidence calculation based on amount and description
        val hasReceipt = !expense.receiptImageUrl.isNullOrEmpty()
        val hasDescription = expense.description.isNotEmpty()

        var confidence = 0.5
        if (hasReceipt) confidence += 0.3
        if (hasDescription) confidence += 0.2

        return min(1.0, confidence)
    }

    private fun categorizeDeductions(deductibleExpenses: List<DeductibleExpense>): Map<String, Double> {
        return deductibleExpenses.groupBy { it.deductionType.name }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }

    private fun calculateEstimatedTaxSavings(categorizedDeductions: Map<String, Double>): Double {
        val totalDeductions = categorizedDeductions.values.sum()
        val estimatedTaxRate = 0.22 // 22% tax bracket assumption
        return totalDeductions * estimatedTaxRate
    }

    private fun generateTaxRecommendations(
        deductibleExpenses: List<DeductibleExpense>,
        categorizedDeductions: Map<String, Double>
    ): List<TaxRecommendation> {
        val recommendations = mutableListOf<TaxRecommendation>()

        // Check for missing receipts
        val expensesWithoutReceipts = deductibleExpenses.filter { it.confidence < 0.8 }
        if (expensesWithoutReceipts.isNotEmpty()) {
            recommendations.add(
                TaxRecommendation(
                    title = "Improve Receipt Documentation",
                    description = "You have ${expensesWithoutReceipts.size} deductible expenses without proper documentation.",
                    potentialSavings = expensesWithoutReceipts.sumOf { it.amount } * 0.22,
                    actionRequired = "Gather missing receipts and documentation",
                    priority = RecommendationPriority.HIGH
                )
            )
        }

        // Check for optimization opportunities
        if (categorizedDeductions.getOrDefault("BUSINESS_EXPENSE", 0.0) < 5000) {
            recommendations.add(
                TaxRecommendation(
                    title = "Consider Home Office Deduction",
                    description = "You may be eligible for home office deductions",
                    potentialSavings = 1200.0,
                    actionRequired = "Track home office expenses and calculate deduction",
                    priority = RecommendationPriority.MEDIUM
                )
            )
        }

        return recommendations
    }

    private fun generateRetirementRecommendations(
        currentAge: Int,
        targetRetirementAge: Int,
        projectedFund: Double,
        currentContribution: Double
    ): List<RetirementRecommendation> {
        val recommendations = mutableListOf<RetirementRecommendation>()

        // Check if retirement fund is sufficient
        val recommendedFund = 1000000.0 // $1M target
        if (projectedFund < recommendedFund) {
            val shortfall = recommendedFund - projectedFund
            recommendations.add(
                RetirementRecommendation(
                    title = "Increase Retirement Savings",
                    description = "You're projected to have a shortfall of ${
                        String.format(
                            "%.0f",
                            shortfall
                        )
                    }",
                    impact = "Could affect retirement lifestyle goals",
                    requiredAction = "Consider increasing monthly contributions"
                )
            )
        }

        // Check contribution level
        if (currentContribution < 500) {
            recommendations.add(
                RetirementRecommendation(
                    title = "Maximize Employer Match",
                    description = "Ensure you're getting full employer 401(k) match",
                    impact = "Free money towards retirement",
                    requiredAction = "Review and adjust 401(k) contributions"
                )
            )
        }

        return recommendations
    }

    private fun generateRetirementScenarios(
        currentAge: Int,
        targetRetirementAge: Int,
        currentSavings: Double,
        baseContribution: Double
    ): List<RetirementScenario> {
        val scenarios = mutableListOf<RetirementScenario>()

        // Conservative scenario
        scenarios.add(
            RetirementScenario(
                name = "Conservative",
                monthlyContribution = baseContribution,
                projectedFund = calculateRetirementFund(
                    currentAge,
                    targetRetirementAge,
                    currentSavings,
                    baseContribution,
                    0.05
                ),
                monthlyIncome = 0.0, // Will be calculated
                description = "5% annual return, current contribution level"
            )
        )

        // Aggressive scenario
        scenarios.add(
            RetirementScenario(
                name = "Aggressive",
                monthlyContribution = baseContribution * 1.5,
                projectedFund = calculateRetirementFund(
                    currentAge,
                    targetRetirementAge,
                    currentSavings,
                    baseContribution * 1.5,
                    0.09
                ),
                monthlyIncome = 0.0, // Will be calculated
                description = "9% annual return, 50% higher contributions"
            )
        )

        return scenarios
    }

    private fun calculateRetirementFund(
        currentAge: Int,
        retirementAge: Int,
        currentSavings: Double,
        monthlyContribution: Double,
        annualReturn: Double
    ): Double {
        val years = retirementAge - currentAge
        val months = years * 12
        val monthlyReturn = annualReturn / 12

        val futureCurrentSavings = currentSavings * (1 + annualReturn).pow(years)
        val futureContributions = if (monthlyContribution > 0) {
            monthlyContribution * ((1 + monthlyReturn).pow(months) - 1) / monthlyReturn
        } else 0.0

        return futureCurrentSavings + futureContributions
    }

    // Additional helper methods for comprehensive analytics...

    private suspend fun getExpensesForDateRange(
        userId: String,
        dateRange: DateRange
    ): List<ExpenseDto> {
        return expenseRepository.getExpensesByDateRange(
            userId,
            dateRange.startDate,
            dateRange.endDate
        )
    }

    private fun generateSpendingAnalysis(
        expenses: List<ExpenseDto>,
        criteria: ReportCriteria
    ): ReportData {
        // Implementation for spending analysis
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        return ReportData(
            summary = mapOf(
                "totalSpending" to expenses.sumOf { it.amount },
                "transactionCount" to expenses.size.toDouble(),
                "averageTransaction" to (expenses.sumOf { it.amount } / expenses.size)
            ),
            breakdown = mapOf("byCategory" to categoryTotals),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateBudgetPerformanceReport(
        userId: String,
        expenses: List<ExpenseDto>,
        criteria: ReportCriteria
    ): ReportData {
        // Implementation for budget performance analysis
        return ReportData(
            summary = emptyMap(),
            breakdown = emptyMap(),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateCategoryTrendsReport(
        expenses: List<ExpenseDto>,
        criteria: ReportCriteria
    ): ReportData {
        // Implementation for category trends
        return ReportData(
            summary = emptyMap(),
            breakdown = emptyMap(),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateSavingsAnalysisReport(
        userId: String,
        expenses: List<ExpenseDto>,
        criteria: ReportCriteria
    ): ReportData {
        // Implementation for savings analysis
        return ReportData(
            summary = emptyMap(),
            breakdown = emptyMap(),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateTaxSummaryReport(expenses: List<ExpenseDto>, criteria: ReportCriteria): ReportData {
        val deductibleExpenses = identifyDeductibleExpenses(expenses)
        val categorizedDeductions = categorizeDeductions(deductibleExpenses)

        return ReportData(
            summary = mapOf(
                "totalDeductions" to categorizedDeductions.values.sum(),
                "estimatedSavings" to (categorizedDeductions.values.sum() * 0.22),
                "deductibleTransactions" to deductibleExpenses.size.toDouble()
            ),
            breakdown = mapOf("byDeductionType" to categorizedDeductions),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateGenericReport(expenses: List<ExpenseDto>, criteria: ReportCriteria): ReportData {
        return ReportData(
            summary = mapOf("totalAmount" to expenses.sumOf { it.amount }),
            breakdown = emptyMap(),
            trends = emptyList(),
            comparisons = null
        )
    }

    private fun generateChartData(reportData: ReportData, criteria: ReportCriteria): List<ChartData> {
        val charts = mutableListOf<ChartData>()

        // Generate pie chart for category breakdown
        if (reportData.breakdown.containsKey("byCategory")) {
            val categoryData = reportData.breakdown["byCategory"] ?: emptyMap()
            charts.add(
                ChartData(
                    type = ChartType.PIE,
                    title = "Spending by Category",
                    data = categoryData.map { (category, amount) ->
                        DataPoint(category, amount)
                    },
                    config = ChartConfig(
                        xAxisLabel = "Category",
                        yAxisLabel = "Amount",
                        colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECCA7", "#DDA0DD")
                    )
                )
            )
        }

        // Generate line chart for trends if available
        if (reportData.trends.isNotEmpty()) {
            charts.add(
                ChartData(
                    type = ChartType.LINE,
                    title = "Spending Trends",
                    data = reportData.trends.map { trendPoint ->
                        DataPoint(trendPoint.period, trendPoint.value)
                    },
                    config = ChartConfig(
                        xAxisLabel = "Period",
                        yAxisLabel = "Amount",
                        colors = listOf("#45B7D1")
                    )
                )
            )
        }

        return charts
    }

    private fun generateReportInsights(reportData: ReportData, criteria: ReportCriteria): List<ReportInsight> {
        val insights = mutableListOf<ReportInsight>()

        val totalSpending = reportData.summary["totalSpending"] ?: 0.0

        if (totalSpending > 0) {
            // Find highest spending category
            val categoryBreakdown = reportData.breakdown["byCategory"] ?: emptyMap()
            val topCategory = categoryBreakdown.maxByOrNull { it.value }

            topCategory?.let { (category, amount) ->
                val percentage = (amount / totalSpending) * 100
                insights.add(
                    ReportInsight(
                        title = "Highest Spending Category",
                        description = "$category represents ${String.format("%.1f", percentage)}% of your total spending",
                        type = InsightType.TREND,
                        impact = if (percentage > 40) InsightImpact.HIGH else InsightImpact.MEDIUM,
                        actionable = percentage > 30,
                        relatedData = mapOf("category" to category, "amount" to amount, "percentage" to percentage)
                    )
                )
            }
        }

        return insights
    }

    private fun generateReportId(): String = "report_${System.currentTimeMillis()}"

    private fun generateReportTitle(criteria: ReportCriteria): String {
        return when (criteria.reportType) {
            ReportType.SPENDING_ANALYSIS -> "Spending Analysis Report"
            ReportType.BUDGET_PERFORMANCE -> "Budget Performance Report"
            ReportType.CATEGORY_TRENDS -> "Category Trends Report"
            ReportType.SAVINGS_ANALYSIS -> "Savings Analysis Report"
            ReportType.TAX_SUMMARY -> "Tax Summary Report"
            ReportType.INVESTMENT_PERFORMANCE -> "Investment Performance Report"
            ReportType.CUSTOM -> "Custom Financial Report"
        }
    }

    private suspend fun getUserIncomeForPeriod(userId: String, period: YearMonth): Double {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getDouble("monthlyIncome") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun getUserEmergencyFund(userId: String): Double {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getDouble("emergencyFund") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun calculateDebtToIncomeRatio(userId: String, income: Double): Double {
        // This would typically fetch debt information from the database
        // For now, we'll return a placeholder value
        return 0.0
    }

    private fun calculateRiskScore(expenses: List<ExpenseDto>, income: Double): Double {
        val totalExpenses = expenses.sumOf { it.amount }
        val expenseToIncomeRatio = if (income > 0) totalExpenses / income else 1.0

        // Calculate risk score based on various factors
        var riskScore = 50.0 // Base score

        // High expense ratio increases risk
        if (expenseToIncomeRatio > 0.8) riskScore += 30
        else if (expenseToIncomeRatio > 0.6) riskScore += 15

        // Spending volatility increases risk
        val volatility = calculateSpendingVolatility(expenses)
        riskScore += volatility * 20

        return min(100.0, max(0.0, riskScore))
    }

    private fun calculateSpendingVolatility(expenses: List<ExpenseDto>): Double {
        if (expenses.size < 2) return 0.0

        val monthlyTotals = expenses.groupBy { YearMonth.from(it.timestamp) }
            .mapValues { it.value.sumOf { expense -> expense.amount } }
            .values.toList()

        if (monthlyTotals.size < 2) return 0.0

        val mean = monthlyTotals.average()
        val variance = monthlyTotals.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)

        return if (mean > 0) standardDeviation / mean else 0.0
    }

    private suspend fun getUserInvestmentAllocation(userId: String): Map<String, Double> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            userDoc.get("investmentAllocation") as? Map<String, Double> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun calculateDiversificationScore(userId: String): Double {
        // Placeholder implementation
        // This would analyze investment portfolio diversification
        return 75.0
    }

    /**
     * Save analytics data for historical tracking
     */
    suspend fun saveAnalyticsData(userId: String, data: Any) = withContext(Dispatchers.IO) {
        try {
            val analyticsDoc = mapOf(
                "userId" to userId,
                "data" to data,
                "timestamp" to LocalDateTime.now().toString(),
                "type" to data.javaClass.simpleName
            )

            db.collection(ANALYTICS_COLLECTION)
                .add(analyticsDoc)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error saving analytics data", e)
        }
    }

    /**
     * Get historical analytics data
     */
    suspend fun getHistoricalAnalytics(userId: String, type: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection(ANALYTICS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .orderBy("timestamp")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                doc.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting historical analytics", e)
            emptyList()
        }
    }

    /**
     * Calculate spending patterns and seasonality
     */
    suspend fun analyzeSpendingPatterns(userId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val expenses = expenseRepository.getExpensesByUser(userId)

            // Group by month to identify patterns
            val monthlySpending = expenses.groupBy { YearMonth.from(it.timestamp) }
                .mapValues { it.value.sumOf { expense -> expense.amount } }

            // Calculate seasonal trends
            val seasonalPattern = calculateSeasonalPattern(monthlySpending)

            // Identify spending spikes
            val spendingSpikes = identifySpendingSpikes(monthlySpending)

            mapOf(
                "monthlyTrends" to monthlySpending,
                "seasonalPattern" to seasonalPattern,
                "spendingSpikes" to spendingSpikes,
                "averageMonthlySpending" to monthlySpending.values.average(),
                "spendingVolatility" to calculateSpendingVolatility(expenses)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing spending patterns", e)
            emptyMap()
        }
    }

    private fun calculateSeasonalPattern(monthlySpending: Map<YearMonth, Double>): Map<Int, Double> {
        return monthlySpending.entries.groupBy { it.key.monthValue }
            .mapValues { (_, entries) -> entries.map { it.value }.average() }
    }

    private fun identifySpendingSpikes(monthlySpending: Map<YearMonth, Double>): List<Map<String, Any>> {
        val average = monthlySpending.values.average()
        val threshold = average * 1.5 // 50% above average is considered a spike

        return monthlySpending.filter { it.value > threshold }
            .map { (month, amount) ->
                mapOf(
                    "month" to month.toString(),
                    "amount" to amount,
                    "percentageAboveAverage" to ((amount - average) / average * 100)
                )
            }
    }

    private fun analyzeCategoryPattern(expenses: List<ExpenseDto>): Map<String, Any> {
        val monthlyAmounts = expenses.groupBy { YearMonth.from(it.timestamp) }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        return mapOf(
            "averageMonthly" to monthlyAmounts.values.average(),
            "volatility" to calculateSpendingVolatility(expenses),
            "trend" to calculateTrend(monthlyAmounts),
            "frequency" to expenses.size
        )
    }

    private fun calculateTrend(monthlyData: Map<YearMonth, Double>): String {
        if (monthlyData.size < 3) return "INSUFFICIENT_DATA"

        val sortedData = monthlyData.toList().sortedBy { it.first }
        val recent = sortedData.takeLast(3).map { it.second }.average()
        val earlier = sortedData.take(3).map { it.second }.average()

        val change = (recent - earlier) / earlier

        return when {
            change > 0.1 -> "INCREASING"
            change < -0.1 -> "DECREASING"
            else -> "STABLE"
        }
    }

}