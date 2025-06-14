// PDFExportService.kt
package com.example.bachelor_frontend.service

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.bachelor_frontend.classes.ExpenseDto
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class PDFExportService(private val context: Context) {

    /**
     * Generate monthly spending PDF report
     */
    fun generateMonthlyPDFReport(
        expenses: List<ExpenseDto>,
        monthlyBudget: Double,
        categoryBudgets: Map<String, Double>,
        currency: String,
        month: String,
        year: Int,
        userName: String
    ): File {
        val fileName = "spending_report_${month}_${year}.pdf"
        val documentsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")

        // Create directory if it doesn't exist
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val pdfFile = File(documentsDir, fileName)

        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }

        try {
            val writer = PdfWriter(pdfFile)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // Header
            document.add(
                Paragraph("Finance Tracker")
                    .setFontSize(24f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )

            document.add(
                Paragraph("Monthly Spending Report")
                    .setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
            )

            document.add(
                Paragraph("$month $year")
                    .setFontSize(14f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10f)
            )

            document.add(
                Paragraph("Generated for: $userName")
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )

            // Summary Section
            val totalSpent = expenses.sumOf { it.amount }
            val remaining = monthlyBudget - totalSpent
            val percentageUsed = if (monthlyBudget > 0) (totalSpent / monthlyBudget * 100) else 0.0

            document.add(
                Paragraph("📊 Summary")
                    .setFontSize(16f)
                    .setBold()
                    .setMarginBottom(10f)
            )

            val summaryTable = Table(2)
            summaryTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))

            summaryTable.addCell(Cell().add(Paragraph("Monthly Budget:")).setBold())
            summaryTable.addCell(Cell().add(Paragraph(currencyFormat.format(monthlyBudget))))
            summaryTable.addCell(Cell().add(Paragraph("Total Spent:")).setBold())
            summaryTable.addCell(Cell().add(Paragraph(currencyFormat.format(totalSpent))))
            summaryTable.addCell(Cell().add(Paragraph("Remaining:")).setBold())
            summaryTable.addCell(Cell().add(Paragraph(currencyFormat.format(remaining))))
            summaryTable.addCell(Cell().add(Paragraph("Budget Used:")).setBold())
            summaryTable.addCell(Cell().add(Paragraph("${String.format("%.1f", percentageUsed)}%")))
            summaryTable.addCell(Cell().add(Paragraph("Total Transactions:")).setBold())
            summaryTable.addCell(Cell().add(Paragraph("${expenses.size}")))

            document.add(summaryTable.setMarginBottom(20f))

            // Category Breakdown
            document.add(
                Paragraph("📂 Category Breakdown")
                    .setFontSize(16f)
                    .setBold()
                    .setMarginBottom(10f)
            )

            val categorySpending = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { expense -> expense.amount } }

            val categoryTable = Table(5)
            categoryTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))

            categoryTable.addHeaderCell(Cell().add(Paragraph("Category").setBold()))
            categoryTable.addHeaderCell(Cell().add(Paragraph("Budget").setBold()))
            categoryTable.addHeaderCell(Cell().add(Paragraph("Spent").setBold()))
            categoryTable.addHeaderCell(Cell().add(Paragraph("Remaining").setBold()))
            categoryTable.addHeaderCell(Cell().add(Paragraph("% Used").setBold()))

            categoryBudgets.forEach { (category, budget) ->
                val spent = categorySpending[category] ?: 0.0
                val remaining = budget - spent
                val percentUsed = if (budget > 0) (spent / budget * 100) else 0.0

                categoryTable.addCell(category)
                categoryTable.addCell(currencyFormat.format(budget))
                categoryTable.addCell(currencyFormat.format(spent))
                categoryTable.addCell(currencyFormat.format(remaining))
                categoryTable.addCell("${String.format("%.1f", percentUsed)}%")
            }

            // Add categories with spending but no budget
            categorySpending.forEach { (category, spent) ->
                if (!categoryBudgets.containsKey(category)) {
                    categoryTable.addCell(category)
                    categoryTable.addCell("No budget set")
                    categoryTable.addCell(currencyFormat.format(spent))
                    categoryTable.addCell("-")
                    categoryTable.addCell("-")
                }
            }

            document.add(categoryTable.setMarginBottom(20f))

            // Top Expenses (if any)
            if (expenses.isNotEmpty()) {
                document.add(
                    Paragraph("💰 Top 10 Expenses")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginBottom(10f)
                )

                val topExpenses = expenses.sortedByDescending { it.amount }.take(10)
                val topExpensesTable = Table(4)
                topExpensesTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))

                topExpensesTable.addHeaderCell(Cell().add(Paragraph("Date").setBold()))
                topExpensesTable.addHeaderCell(Cell().add(Paragraph("Description").setBold()))
                topExpensesTable.addHeaderCell(Cell().add(Paragraph("Category").setBold()))
                topExpensesTable.addHeaderCell(Cell().add(Paragraph("Amount").setBold()))

                topExpenses.forEach { expense ->
                    topExpensesTable.addCell(expense.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    topExpensesTable.addCell(expense.description.take(30) + if (expense.description.length > 30) "..." else "")
                    topExpensesTable.addCell(expense.category)
                    topExpensesTable.addCell(currencyFormat.format(expense.amount))
                }

                document.add(topExpensesTable.setMarginBottom(20f))

                // Daily Spending Chart (text-based)
                document.add(
                    Paragraph("📈 Daily Spending Overview")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginBottom(10f)
                )

                val dailySpending = expenses.groupBy {
                    it.timestamp.format(DateTimeFormatter.ofPattern("dd/MM"))
                }.mapValues { it.value.sumOf { expense -> expense.amount } }
                    .toList().sortedBy { it.first }

                val dailyTable = Table(2)
                dailyTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f))

                dailyTable.addHeaderCell(Cell().add(Paragraph("Date").setBold()))
                dailyTable.addHeaderCell(Cell().add(Paragraph("Total Spent").setBold()))

                dailySpending.forEach { (date, amount) ->
                    dailyTable.addCell(date)
                    dailyTable.addCell(currencyFormat.format(amount))
                }

                document.add(dailyTable.setMarginBottom(20f))
            }

            // Insights Section
            document.add(
                Paragraph("💡 Insights")
                    .setFontSize(16f)
                    .setBold()
                    .setMarginBottom(10f)
            )

            val insights = generateInsights(expenses, monthlyBudget, categoryBudgets, categorySpending)
            insights.forEach { insight ->
                document.add(
                    Paragraph("• $insight")
                        .setMarginBottom(5f)
                )
            }

            // Footer
            document.add(
                Paragraph("Generated on ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30f)
            )

            document.add(
                Paragraph("Finance Tracker © 2025")
                    .setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )

            document.close()
            return pdfFile

        } catch (e: Exception) {
            throw Exception("Failed to generate PDF: ${e.message}")
        }
    }

    private fun generateInsights(
        expenses: List<ExpenseDto>,
        monthlyBudget: Double,
        categoryBudgets: Map<String, Double>,
        categorySpending: Map<String, Double>
    ): List<String> {
        val insights = mutableListOf<String>()

        val totalSpent = expenses.sumOf { it.amount }

        // Budget insights
        if (monthlyBudget > 0) {
            val percentageUsed = (totalSpent / monthlyBudget * 100)
            when {
                percentageUsed > 100 -> insights.add("You exceeded your monthly budget by ${String.format("%.1f", percentageUsed - 100)}%")
                percentageUsed > 90 -> insights.add("You used ${String.format("%.1f", percentageUsed)}% of your budget - getting close to the limit!")
                percentageUsed < 50 -> insights.add("Great job! You only used ${String.format("%.1f", percentageUsed)}% of your budget")
            }
        }

        // Category insights
        val topCategory = categorySpending.maxByOrNull { it.value }
        topCategory?.let {
            insights.add("Your highest spending category was ${it.key} with ${NumberFormat.getCurrencyInstance().format(it.value)}")
        }

        // Over budget categories
        val overBudgetCategories = categoryBudgets.filter { (category, budget) ->
            val spent = categorySpending[category] ?: 0.0
            spent > budget && budget > 0
        }

        if (overBudgetCategories.isNotEmpty()) {
            insights.add("${overBudgetCategories.size} categories went over budget: ${overBudgetCategories.keys.joinToString(", ")}")
        }

        // Transaction patterns
        if (expenses.isNotEmpty()) {
            val avgTransaction = totalSpent / expenses.size
            insights.add("Average transaction amount: ${NumberFormat.getCurrencyInstance().format(avgTransaction)}")

            val mostActiveDay = expenses.groupBy { it.timestamp.dayOfWeek }
                .maxByOrNull { it.value.size }?.key
            mostActiveDay?.let {
                insights.add("Most active spending day: ${it.getDisplayName(TextStyle.FULL, Locale.getDefault())}")
            }
        }

        return insights
    }

    /**
     * Send PDF report via email
     */
    fun sendPDFReportViaEmail(
        pdfFile: File,
        userEmail: String,
        userName: String,
        month: String,
        year: Int,
        isCSV: Boolean = false
    ): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val fileType = if (isCSV) "CSV" else "PDF"
            val fileName = pdfFile.name

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (isCSV) "text/csv" else "application/pdf"

                // Set recipient
                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))

                // Set subject
                putExtra(Intent.EXTRA_SUBJECT, "Your Finance Tracker Report - $month $year")

                // Set email body
                putExtra(Intent.EXTRA_TEXT, """
                Hi $userName,
                
                Please find attached your monthly spending report for $month $year.
                
                📊 This $fileType report includes:
                • Complete summary of your monthly budget vs actual spending
                • Detailed category-wise breakdown with budget analysis
                • Your top expenses for the month
                • Daily spending overview and patterns
                • Personalized insights and recommendations for better financial management
                
                📈 Key Benefits:
                • Track your spending habits
                • Identify areas where you can save money
                • Monitor your budget performance
                • Make informed financial decisions
                
                💡 Tip: Review this report regularly to stay on top of your finances!
                
                Thank you for using Finance Tracker to manage your personal finances.
                
                Best regards,
                Finance Tracker Team
                
                ---
                This report was automatically generated by Finance Tracker app.
                Report generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
            """.trimIndent())

                // Attach the file
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try to find Gmail first, then fall back to other email apps
            val emailApps = context.packageManager.queryIntentActivities(emailIntent, 0)
            val gmailIntent = emailApps.find {
                it.activityInfo.packageName.contains("gmail", ignoreCase = true)
            }

            if (gmailIntent != null) {
                // Use Gmail directly if available
                emailIntent.setPackage("com.google.android.gm")
                context.startActivity(emailIntent)
            } else {
                // Use chooser for other email apps
                val chooserIntent = Intent.createChooser(
                    emailIntent,
                    "Send $fileType Report via Email"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
            }

            true
        } catch (e: Exception) {
            Log.e("PDFExportService", "Failed to send email", e)
            throw Exception("Failed to send email: ${e.message}")
        }
    }


    /**
     * Share PDF file via various apps
     */
    fun sharePDFReport(pdfFile: File, isCSV: Boolean) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Monthly Spending Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Report")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            throw Exception("Failed to share file: ${e.message}")
        }
    }

    fun generateCSVExport(
        expenses: List<ExpenseDto>,
        month: String,
        year: Int
    ): File {
        val fileName = "expenses_${month}_${year}.csv"
        val documentsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")

        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val csvFile = File(documentsDir, fileName)

        try {
            csvFile.writeText("Date,Description,Category,Amount,Receipt\n")

            expenses.sortedBy { it.timestamp }.forEach { expense ->
                val line = "${expense.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}," +
                        "\"${expense.description.replace("\"", "\"\"")}\","  +
                        "${expense.category}," +
                        "${expense.amount}," +
                        "${if (expense.receiptImageUrl?.isNotEmpty() == true) "Yes" else "No"}\n"
                csvFile.appendText(line)
            }

            Log.d(TAG, "CSV generated successfully: ${csvFile.absolutePath}")
            return csvFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate CSV", e)
            throw Exception("Failed to generate CSV: ${e.message}")
        }
    }

    fun exportAndEmailReport(
        expenses: List<ExpenseDto>,
        monthlyBudget: Double,
        categoryBudgets: Map<String, Double>,
        currency: String,
        month: String,
        year: Int,
        userName: String,
        userEmail: String,
        exportType: ExportType = ExportType.PDF
    ): File {
        return try {
            val exportedFile = when (exportType) {
                ExportType.PDF -> {
                    generateMonthlyPDFReport(
                        expenses = expenses,
                        monthlyBudget = monthlyBudget,
                        categoryBudgets = categoryBudgets,
                        currency = currency,
                        month = month,
                        year = year,
                        userName = userName
                    )
                }
                ExportType.CSV -> {
                    generateCSVExport(expenses, month, year)
                }
            }

            // Automatically attempt to send via email
            sendPDFReportViaEmail(
                pdfFile = exportedFile,
                userEmail = userEmail,
                userName = userName,
                month = month,
                year = year,
                isCSV = exportType == ExportType.CSV
            )

            exportedFile
        } catch (e: Exception) {
            Log.e("PDFExportService", "Export and email failed", e)
            throw e
        }
    }

    enum class ExportType {
        PDF, CSV
    }

}