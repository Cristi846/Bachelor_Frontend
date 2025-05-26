// AdvancedAnalyticsScreen.kt
package com.example.bachelor_frontend.analytics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.analytics.models.*
import com.example.bachelor_frontend.analytics.viewmodel.AdvancedAnalyticsViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalyticsScreen(
    viewModel: AdvancedAnalyticsViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val financialMetrics by viewModel.financialMetrics.collectAsState()
    val cashFlowProjection by viewModel.cashFlowProjection.collectAsState()
    val taxOptimizationReport by viewModel.taxOptimizationReport.collectAsState()
    val retirementPlan by viewModel.retirementPlan.collectAsState()
    val customReports by viewModel.customReports.collectAsState()
    val spendingPatterns by viewModel.spendingPatterns.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Projections", "Reports", "Tax Optimization", "Retirement")

    LaunchedEffect(Unit) {
        viewModel.loadAllAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Analytics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshAnalytics() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> OverviewTab(viewModel, financialMetrics, spendingPatterns)
                    1 -> ProjectionsTab(viewModel, cashFlowProjection)
                    2 -> ReportsTab(viewModel, customReports)
                    3 -> TaxOptimizationTab(viewModel, taxOptimizationReport)
                    4 -> RetirementTab(viewModel, retirementPlan)
                }

                // Loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Error display
                error?.let { errorMessage ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(errorMessage)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewTab(
    viewModel: AdvancedAnalyticsViewModel,
    financialMetrics: FinancialMetrics?,
    spendingPatterns: Map<String, Any>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FinancialHealthCard(viewModel, financialMetrics)
        }

        item {
            MetricsOverviewCard(financialMetrics)
        }

        item {
            SpendingPatternsCard(spendingPatterns)
        }

        item {
            RecommendationsCard(viewModel.getPersonalizedRecommendations())
        }

        item {
            QuickActionsCard(viewModel)
        }
    }
}

@Composable
fun FinancialHealthCard(
    viewModel: AdvancedAnalyticsViewModel,
    financialMetrics: FinancialMetrics?
) {
    val healthScore = viewModel.getFinancialHealthScore()
    val scoreColor = when {
        healthScore >= 80 -> Color.Green
        healthScore >= 60 -> Color(0xFFFFA000) // Amber
        else -> Color.Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Financial Health Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular progress indicator for health score
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = (healthScore / 100).toFloat(),
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 8.dp
                )

                Text(
                    text = "${healthScore.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    healthScore >= 80 -> "Excellent"
                    healthScore >= 60 -> "Good"
                    healthScore >= 40 -> "Fair"
                    else -> "Needs Improvement"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = scoreColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MetricsOverviewCard(financialMetrics: FinancialMetrics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            financialMetrics?.let { metrics ->
                MetricItem("Savings Rate", "${String.format("%.1f", metrics.savingsRate)}%")
                MetricItem("Emergency Fund", "${String.format("%.1f", metrics.emergencyFundMonths)} months")
                MetricItem("Monthly Burn Rate", "${String.format("%.0f", metrics.burnRate)}")
                MetricItem("Risk Score", "${metrics.riskScore.toInt()}/100")
                MetricItem("Spending Volatility", "${String.format("%.1f", metrics.volatilityIndex * 100)}%")
            } ?: run {
                Text(
                    text = "No metrics available. Please wait for data to load.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpendingPatternsCard(spendingPatterns: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Spending Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (spendingPatterns.isNotEmpty()) {
                val avgMonthlySpending = spendingPatterns["averageMonthlySpending"] as? Double
                val volatility = spendingPatterns["spendingVolatility"] as? Double

                avgMonthlySpending?.let {
                    MetricItem("Average Monthly Spending", "${String.format("%.0f", it)}")
                }

                volatility?.let {
                    MetricItem("Spending Volatility", "${String.format("%.1f", it * 100)}%")
                }

                @Suppress("UNCHECKED_CAST")
                val spikes = spendingPatterns["spendingSpikes"] as? List<Map<String, Any>>
                spikes?.let { spikeList ->
                    if (spikeList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Spending Spikes Detected: ${spikeList.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red
                        )
                    }
                }
            } else {
                Text(
                    text = "Analyzing spending patterns...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Recommendations",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personalized Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recommendations.isNotEmpty()) {
                recommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "Great job! No specific recommendations at this time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun QuickActionsCard(viewModel: AdvancedAnalyticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.generateCashFlowForecast() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cash Flow")
                }

                Button(
                    onClick = { viewModel.getBudgetPerformanceReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Budget Report")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.analyzeTaxOptimization() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tax Analysis")
                }

                OutlinedButton(
                    onClick = { viewModel.getCategoryTrendsReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trends")
                }
            }
        }
    }
}

@Composable
fun ProjectionsTab(
    viewModel: AdvancedAnalyticsViewModel,
    cashFlowProjection: CashFlowProjection?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CashFlowProjectionCard(viewModel, cashFlowProjection)
        }

        cashFlowProjection?.let { projection ->
            items(projection.monthlyProjections) { monthlyProjection ->
                MonthlyProjectionCard(monthlyProjection)
            }
        }
    }
}

@Composable
fun CashFlowProjectionCard(
    viewModel: AdvancedAnalyticsViewModel,
    cashFlowProjection: CashFlowProjection?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Flow Projection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.generateCashFlowForecast() }
                ) {
                    Text("Generate")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            cashFlowProjection?.let { projection ->
                val currencyFormat = NumberFormat.getCurrencyInstance()

                MetricItem("Projection Period", "${projection.projectionPeriod} months")
                MetricItem("Confidence Level", "${String.format("%.0f", projection.confidence * 100)}%")

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                MetricItem("Total Projected Income", currencyFormat.format(projection.summary.totalProjectedIncome))
                MetricItem("Total Projected Expenses", currencyFormat.format(projection.summary.totalProjectedExpenses))
                MetricItem("Total Projected Savings", currencyFormat.format(projection.summary.totalProjectedSavings))
                MetricItem("Average Monthly Savings", currencyFormat.format(projection.summary.averageMonthlySavings))

                Spacer(modifier = Modifier.height(8.dp))

                // Scenario analysis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScenarioChip("Best Case", projection.summary.bestCaseScenario, Color.Green)
                    ScenarioChip("Worst Case", projection.summary.worstCaseScenario, Color.Red)
                }
            } ?: run {
                Text(
                    text = "Click 'Generate' to create a cash flow projection based on your spending history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ScenarioChip(label: String, amount: Double, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = NumberFormat.getCurrencyInstance().format(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun MonthlyProjectionCard(monthlyProjection: MonthlyProjection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = monthlyProjection.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currencyFormat = NumberFormat.getCurrencyInstance()

            MetricItem("Projected Income", currencyFormat.format(monthlyProjection.projectedIncome))
            MetricItem("Projected Expenses", currencyFormat.format(monthlyProjection.projectedExpenses))
            MetricItem("Projected Savings", currencyFormat.format(monthlyProjection.projectedSavings))
            MetricItem("Confidence", "${String.format("%.0f", monthlyProjection.confidence * 100)}%")
        }
    }
}

@Composable
fun ReportsTab(
    viewModel: AdvancedAnalyticsViewModel,
    customReports: List<CustomReport>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ReportGenerationCard(viewModel)
        }

        items(customReports) { report ->
            CustomReportCard(report, viewModel)
        }

        if (customReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "No Reports",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No reports generated yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Use the options above to generate your first report",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportGenerationCard(viewModel: AdvancedAnalyticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Generate Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick report buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.getQuickSpendingAnalysis() },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Text("Spending", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Button(
                    onClick = { viewModel.getBudgetPerformanceReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Text("Budget", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.getCategoryTrendsReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null)
                        Text("Trends", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.getTaxSummaryReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Text("Tax", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomReportCard(report: CustomReport, viewModel: AdvancedAnalyticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generated: ${report.generatedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.selectReport(report) }) {
                        Icon(Icons.Default.Visibility, contentDescription = "View")
                    }
                    IconButton(onClick = { viewModel.deleteReport(report.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show summary data
            if (report.data.summary.isNotEmpty()) {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                report.data.summary.entries.take(3).forEach { (key, value) ->
                    MetricItem(
                        label = key.replace("([A-Z])".toRegex(), " $1").lowercase().replaceFirstChar { it.uppercase() },
                        value = if (key.contains("amount", true) || key.contains("income", true) || key.contains("expense", true)) {
                            NumberFormat.getCurrencyInstance().format(value)
                        } else {
                            value.toString()
                        }
                    )
                }
            }

            // Show insights
            if (report.insights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Key Insight: ${report.insights.first().title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TaxOptimizationTab(
    viewModel: AdvancedAnalyticsViewModel,
    taxOptimizationReport: TaxOptimizationReport?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TaxOptimizationCard(viewModel, taxOptimizationReport)
        }

        taxOptimizationReport?.let { report ->
            if (report.deductibleExpenses.isNotEmpty()) {
                item {
                    DeductibleExpensesCard(report.deductibleExpenses)
                }
            }

            if (report.recommendations.isNotEmpty()) {
                item {
                    TaxRecommendationsCard(report.recommendations)
                }
            }
        }
    }
}

@Composable
fun TaxOptimizationCard(
    viewModel: AdvancedAnalyticsViewModel,
    taxOptimizationReport: TaxOptimizationReport?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tax Optimization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.analyzeTaxOptimization() }
                ) {
                    Text("Analyze")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            taxOptimizationReport?.let { report ->
                val currencyFormat = NumberFormat.getCurrencyInstance()

                MetricItem("Tax Year", report.taxYear.toString())
                MetricItem("Deductible Expenses", "${report.deductibleExpenses.size}")
                MetricItem("Total Deductions", currencyFormat.format(report.categorizedDeductions.values.sum()))
                MetricItem("Estimated Tax Savings", currencyFormat.format(report.estimatedTaxSavings))

                Spacer(modifier = Modifier.height(16.dp))

                // Category breakdown
                if (report.categorizedDeductions.isNotEmpty()) {
                    Text(
                        text = "Deduction Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    report.categorizedDeductions.forEach { (category, amount) ->
                        MetricItem(
                            label = category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            value = currencyFormat.format(amount)
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "Click 'Analyze' to identify potential tax deductions and savings opportunities.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DeductibleExpensesCard(deductibleExpenses: List<DeductibleExpense>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Deductible Expenses (${deductibleExpenses.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            deductibleExpenses.take(5).forEach { expense ->
                DeductibleExpenseItem(expense)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (deductibleExpenses.size > 5) {
                Text(
                    text = "... and ${deductibleExpenses.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DeductibleExpenseItem(expense: DeductibleExpense) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.description.ifEmpty { expense.category },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${expense.deductionType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} • ${String.format("%.0f", expense.confidence * 100)}% confidence",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Text(
            text = NumberFormat.getCurrencyInstance().format(expense.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TaxRecommendationsCard(recommendations: List<TaxRecommendation>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Tax Recommendations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            recommendations.forEach { recommendation ->
                TaxRecommendationItem(recommendation)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TaxRecommendationItem(recommendation: TaxRecommendation) {
    val priorityColor = when (recommendation.priority) {
        RecommendationPriority.HIGH -> Color.Red
        RecommendationPriority.MEDIUM -> Color(0xFFFFA000)
        RecommendationPriority.LOW -> Color.Green
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (recommendation.priority) {
                    RecommendationPriority.HIGH -> Icons.Default.PriorityHigh
                    RecommendationPriority.MEDIUM -> Icons.Default.Warning
                    RecommendationPriority.LOW -> Icons.Default.Info
                },
                contentDescription = null,
                tint = priorityColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = recommendation.description,
            style = MaterialTheme.typography.bodySmall
        )

        if (recommendation.potentialSavings > 0) {
            Text(
                text = "Potential savings: ${NumberFormat.getCurrencyInstance().format(recommendation.potentialSavings)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Green,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "Action: ${recommendation.actionRequired}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun RetirementTab(
    viewModel: AdvancedAnalyticsViewModel,
    retirementPlan: RetirementPlan?
) {
    var showRetirementDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RetirementPlanningCard(viewModel, retirementPlan, onShowDialog = { showRetirementDialog = true })
        }

        retirementPlan?.let { plan ->
            item {
                RetirementProjectionCard(plan)
            }

            item {
                RetirementScenariosCard(plan.scenarios)
            }

            if (plan.recommendations.isNotEmpty()) {
                item {
                    RetirementRecommendationsCard(plan.recommendations)
                }
            }
        }
    }

    if (showRetirementDialog) {
        RetirementInputDialog(
            onDismiss = { showRetirementDialog = false },
            onCalculate = { age, retirementAge, savings, contribution ->
                viewModel.calculateRetirementProjections(age, retirementAge, savings, contribution)
                showRetirementDialog = false
            }
        )
    }
}

@Composable
fun RetirementPlanningCard(
    viewModel: AdvancedAnalyticsViewModel,
    retirementPlan: RetirementPlan?,
    onShowDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retirement Planning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(onClick = onShowDialog) {
                    Text("Calculate")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            retirementPlan?.let { plan ->
                val currencyFormat = NumberFormat.getCurrencyInstance()

                MetricItem("Current Age", "${plan.currentAge}")
                MetricItem("Target Retirement Age", "${plan.targetRetirementAge}")
                MetricItem("Years to Retirement", "${plan.targetRetirementAge - plan.currentAge}")

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                MetricItem("Current Savings", currencyFormat.format(plan.currentSavings))
                MetricItem("Monthly Contribution", currencyFormat.format(plan.monthlyContribution))
                MetricItem("Projected Retirement Fund", currencyFormat.format(plan.projectedRetirementFund))
                MetricItem("Monthly Retirement Income", currencyFormat.format(plan.monthlyRetirementIncome))
                MetricItem("Inflation-Adjusted Income", currencyFormat.format(plan.inflationAdjustedIncome))

            } ?: run {
                Text(
                    text = "Click 'Calculate' to create a personalized retirement plan based on your current financial situation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RetirementProjectionCard(plan: RetirementPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Retirement Readiness",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple retirement readiness assessment
            val recommendedFund = 1000000.0 // $1M target
            val readinessPercentage = (plan.projectedRetirementFund / recommendedFund * 100).coerceAtMost(100.0)

            val readinessColor = when {
                readinessPercentage >= 100 -> Color.Green
                readinessPercentage >= 75 -> Color(0xFFFFA000)
                else -> Color.Red
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Retirement Readiness",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${String.format("%.0f", readinessPercentage)}% of recommended target",
                        style = MaterialTheme.typography.bodySmall,
                        color = readinessColor
                    )
                }

                CircularProgressIndicator(
                    progress = (readinessPercentage / 100).toFloat(),
                    modifier = Modifier.size(60.dp),
                    color = readinessColor,
                    strokeWidth = 6.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4% rule assessment
            val annualWithdrawal = plan.projectedRetirementFund * 0.04
            val monthlyWithdrawal = annualWithdrawal / 12

            Text(
                text = "4% Rule Analysis",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Safe monthly withdrawal: ${NumberFormat.getCurrencyInstance().format(monthlyWithdrawal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RetirementScenariosCard(scenarios: List<RetirementScenario>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Retirement Scenarios",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            scenarios.forEach { scenario ->
                RetirementScenarioItem(scenario)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RetirementScenarioItem(scenario: RetirementScenario) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = scenario.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = scenario.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Monthly: ${NumberFormat.getCurrencyInstance().format(scenario.monthlyContribution)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Fund: ${NumberFormat.getCurrencyInstance().format(scenario.projectedFund)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RetirementRecommendationsCard(recommendations: List<RetirementRecommendation>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Retirement Recommendations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            recommendations.forEach { recommendation ->
                RetirementRecommendationItem(recommendation)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RetirementRecommendationItem(recommendation: RetirementRecommendation) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = recommendation.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = recommendation.description,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Impact: ${recommendation.impact}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Action: ${recommendation.requiredAction}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun RetirementInputDialog(
    onDismiss: () -> Unit,
    onCalculate: (Int, Int, Double, Double) -> Unit
) {
    var currentAge by remember { mutableStateOf("") }
    var retirementAge by remember { mutableStateOf("65") }
    var currentSavings by remember { mutableStateOf("") }
    var monthlyContribution by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirement Planning Calculator") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = currentAge,
                    onValueChange = { currentAge = it },
                    label = { Text("Current Age") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = retirementAge,
                    onValueChange = { retirementAge = it },
                    label = { Text("Target Retirement Age") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentSavings,
                    onValueChange = { currentSavings = it },
                    label = { Text("Current Retirement Savings ($)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = monthlyContribution,
                    onValueChange = { monthlyContribution = it },
                    label = { Text("Monthly Contribution ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val age = currentAge.toIntOrNull() ?: 30
                    val retAge = retirementAge.toIntOrNull() ?: 65
                    val savings = currentSavings.toDoubleOrNull() ?: 0.0
                    val contribution = monthlyContribution.toDoubleOrNull() ?: 0.0
                    onCalculate(age, retAge, savings, contribution)
                }
            ) {
                Text("Calculate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}