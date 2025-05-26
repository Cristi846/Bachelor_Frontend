// AdvancedAnalyticsModels.kt
package com.example.bachelor_frontend.analytics.models

import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Cash Flow Projection Model
 */
data class CashFlowProjection(
    val userId: String,
    val projectionPeriod: Int, // months
    val monthlyProjections: List<MonthlyProjection>,
    val summary: CashFlowSummary,
    val confidence: Double, // 0.0 to 1.0
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class MonthlyProjection(
    val month: YearMonth,
    val projectedIncome: Double,
    val projectedExpenses: Double,
    val projectedSavings: Double,
    val categoryBreakdown: Map<String, Double>,
    val confidence: Double
)

data class CashFlowSummary(
    val totalProjectedIncome: Double,
    val totalProjectedExpenses: Double,
    val totalProjectedSavings: Double,
    val averageMonthlySavings: Double,
    val worstCaseScenario: Double,
    val bestCaseScenario: Double
)

/**
 * Tax Optimization Report Model
 */
data class TaxOptimizationReport(
    val userId: String,
    val taxYear: Int,
    val deductibleExpenses: List<DeductibleExpense>,
    val categorizedDeductions: Map<String, Double>,
    val estimatedTaxSavings: Double,
    val recommendations: List<TaxRecommendation>,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class DeductibleExpense(
    val expenseId: String,
    val amount: Double,
    val category: String,
    val deductionType: TaxDeductionType,
    val description: String,
    val date: LocalDateTime,
    val confidence: Double // How confident we are this is deductible
)

enum class TaxDeductionType {
    BUSINESS_EXPENSE,
    MEDICAL_EXPENSE,
    CHARITABLE_DONATION,
    HOME_OFFICE,
    EDUCATION,
    TRAVEL,
    OTHER
}

data class TaxRecommendation(
    val title: String,
    val description: String,
    val potentialSavings: Double,
    val actionRequired: String,
    val priority: RecommendationPriority
)

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}

/**
 * Retirement Planning Model
 */
data class RetirementPlan(
    val userId: String,
    val currentAge: Int,
    val targetRetirementAge: Int,
    val currentSavings: Double,
    val monthlyContribution: Double,
    val projectedRetirementFund: Double,
    val monthlyRetirementIncome: Double,
    val inflationAdjustedIncome: Double,
    val recommendations: List<RetirementRecommendation>,
    val scenarios: List<RetirementScenario>,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class RetirementRecommendation(
    val title: String,
    val description: String,
    val impact: String,
    val requiredAction: String
)

data class RetirementScenario(
    val name: String,
    val monthlyContribution: Double,
    val projectedFund: Double,
    val monthlyIncome: Double,
    val description: String
)

/**
 * Custom Report Models
 */
data class ReportCriteria(
    val reportType: ReportType,
    val dateRange: DateRange,
    val categories: List<String>? = null,
    val groupBy: GroupBy,
    val includeComparisons: Boolean = false,
    val includeProjections: Boolean = false
)

enum class ReportType {
    SPENDING_ANALYSIS,
    BUDGET_PERFORMANCE,
    CATEGORY_TRENDS,
    SAVINGS_ANALYSIS,
    TAX_SUMMARY,
    INVESTMENT_PERFORMANCE,
    CUSTOM
}

enum class GroupBy {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CATEGORY
}

data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

data class CustomReport(
    val id: String,
    val userId: String,
    val title: String,
    val criteria: ReportCriteria,
    val data: ReportData,
    val visualizations: List<ChartData>,
    val insights: List<ReportInsight>,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class ReportData(
    val summary: Map<String, Double>,
    val breakdown: Map<String, Map<String, Double>>,
    val trends: List<TrendPoint>,
    val comparisons: Map<String, ComparisonData>?
)

data class TrendPoint(
    val period: String,
    val value: Double,
    val change: Double, // percentage change from previous period
    val metadata: Map<String, Any>
)

data class ComparisonData(
    val current: Double,
    val previous: Double,
    val change: Double,
    val changePercent: Double
)

data class ChartData(
    val type: ChartType,
    val title: String,
    val data: List<DataPoint>,
    val config: ChartConfig
)

enum class ChartType {
    LINE, BAR, PIE, AREA, SCATTER, HEATMAP
}

data class DataPoint(
    val label: String,
    val value: Double,
    val metadata: Map<String, Any> = emptyMap()
)

data class ChartConfig(
    val xAxisLabel: String,
    val yAxisLabel: String,
    val colors: List<String>,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true
)

data class ReportInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val impact: InsightImpact,
    val actionable: Boolean,
    val relatedData: Map<String, Any>
)

enum class InsightType {
    TREND, ANOMALY, OPPORTUNITY, WARNING, ACHIEVEMENT
}

enum class InsightImpact {
    HIGH, MEDIUM, LOW
}

/**
 * Advanced Metrics Models
 */
data class FinancialMetrics(
    val userId: String,
    val period: YearMonth,
    val savingsRate: Double, // percentage
    val burnRate: Double, // monthly spending rate
    val emergencyFundMonths: Double, // months of expenses covered
    val debtToIncomeRatio: Double,
    val investmentAllocation: Map<String, Double>,
    val riskScore: Double, // 0-100
    val volatilityIndex: Double,
    val diversificationScore: Double
)

data class SpendingVolatility(
    val userId: String,
    val category: String,
    val averageMonthlySpending: Double,
    val standardDeviation: Double,
    val volatilityPercentage: Double,
    val predictabilityScore: Double // 0-100
)

data class BudgetPerformanceAnalysis(
    val userId: String,
    val period: YearMonth,
    val overallPerformance: Double, // percentage of budget used
    val categoryPerformances: Map<String, CategoryPerformance>,
    val trends: Map<String, List<Double>>, // last 12 months performance
    val recommendations: List<BudgetRecommendation>
)

data class CategoryPerformance(
    val category: String,
    val budgetAmount: Double,
    val actualSpending: Double,
    val variance: Double,
    val variancePercentage: Double,
    val trend: PerformanceTrend
)

enum class PerformanceTrend {
    IMPROVING, STABLE, DETERIORATING
}

data class BudgetRecommendation(
    val category: String,
    val currentBudget: Double,
    val recommendedBudget: Double,
    val reason: String,
    val confidence: Double
)