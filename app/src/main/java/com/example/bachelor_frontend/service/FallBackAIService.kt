// Complete FallbackAIService.kt - Works without any external dependencies
package com.example.bachelor_frontend.service

import android.util.Log
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.utils.ExpenseChatParser
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

class FallbackAIService {
    private val expenseParser = ExpenseChatParser()

    companion object {
        private const val TAG = "FallbackAIService"

        private val GREETING_RESPONSES = listOf(
            "👋 Hello! I'm your financial assistant. How can I help you manage your money today?",
            "Hi there! Ready to take control of your finances? What would you like to do?",
            "Welcome back! I'm here to help you track expenses and manage your budget. What's on your mind?"
        )

        private val EXPENSE_CONFIRMATIONS = listOf(
            "Perfect! I found an expense for {amount} {currency} in {category}. Should I add this to your records?",
            "Great! I detected a {category} expense of {amount} {currency}. Shall I save this for you?",
            "I see you spent {amount} {currency} on {category}. Would you like me to track this expense?"
        )

        private val ENCOURAGEMENT_PHRASES = listOf(
            "You're doing great with tracking your expenses! 🎉",
            "Keep up the good work managing your finances! 💪",
            "I'm impressed by your financial discipline! ⭐",
            "Your budget awareness is really improving! 📈"
        )
    }

    suspend fun processMessage(
        message: String,
        context: GeminiAIService.ConversationContext
    ): GeminiAIService.AIResponse {
        // Simulate AI thinking time
        delay(Random.nextLong(500, 1500))

        val cleanMessage = message.lowercase().trim()
        Log.d(TAG, "Processing message: $message")

        return when {
            isGreeting(cleanMessage) -> handleGreeting()
            isExpenseRelated(cleanMessage) -> handleExpenseTracking(message, context)
            isBudgetAnalysisRequest(cleanMessage) -> handleBudgetAnalysis(context)
            isAdviceRequest(cleanMessage) -> handleAdviceRequest(context)
            isCategoryRequest(cleanMessage) -> handleCategoryManagement(context)
            isGeneralFinanceQuestion(cleanMessage) -> handleGeneralFinance(cleanMessage)
            else -> handleFallbackResponse(cleanMessage)
        }
    }

    private fun isGreeting(message: String): Boolean {
        val greetings = listOf("hello", "hi", "hey", "good morning", "good afternoon", "good evening")
        return greetings.any { message.contains(it) }
    }

    private fun isExpenseRelated(message: String): Boolean {
        val expenseKeywords = listOf("spent", "paid", "bought", "cost", "expense", "purchase", "money")
        return expenseKeywords.any { message.contains(it) }
    }

    private fun isBudgetAnalysisRequest(message: String): Boolean {
        val budgetKeywords = listOf("budget", "analysis", "analyze", "spending", "overview", "summary")
        return budgetKeywords.any { message.contains(it) }
    }

    private fun isAdviceRequest(message: String): Boolean {
        val adviceKeywords = listOf("advice", "suggest", "recommend", "help", "improve", "save money", "tips")
        return adviceKeywords.any { message.contains(it) }
    }

    private fun isCategoryRequest(message: String): Boolean {
        val categoryKeywords = listOf("category", "categories", "organize", "classification")
        return categoryKeywords.any { message.contains(it) }
    }

    private fun isGeneralFinanceQuestion(message: String): Boolean {
        val financeKeywords = listOf("finance", "financial", "money", "investment", "saving", "debt")
        return financeKeywords.any { message.contains(it) }
    }

    private fun handleGreeting(): GeminiAIService.AIResponse {
        return GeminiAIService.AIResponse(
            message = GREETING_RESPONSES.random(),
            suggestions = listOf(
                "Track a new expense",
                "Analyze my budget",
                "Get spending advice",
                "Show my categories"
            ),
            actionType = GeminiAIService.ActionType.CONVERSATION
        )
    }

    private fun handleExpenseTracking(
        message: String,
        context: GeminiAIService.ConversationContext
    ): GeminiAIService.AIResponse {
        val parsed = expenseParser.parseExpenseMessage(message, context.userCurrency)

        return if (parsed.amount != null && parsed.confidence >= 0.5f) {
            val expenseData = GeminiAIService.ExpenseData(
                amount = parsed.amount,
                category = parsed.category ?: "Other",
                description = parsed.description ?: "Expense tracked via chat",
                merchant = parsed.merchant,
                currency = parsed.currency ?: context.userCurrency
            )

            val confirmationMessage = EXPENSE_CONFIRMATIONS.random()
                .replace("{amount}", parsed.amount.toString())
                .replace("{currency}", expenseData.currency)
                .replace("{category}", expenseData.category)

            GeminiAIService.AIResponse(
                message = confirmationMessage,
                expenseData = expenseData,
                actionType = GeminiAIService.ActionType.CREATE_EXPENSE,
                confidence = parsed.confidence
            )
        } else {
            GeminiAIService.AIResponse(
                message = "I couldn't quite understand that expense. Could you try something like:\n\n" +
                        "• 'I spent 50 euros at the grocery store'\n" +
                        "• 'Bought coffee for 5 dollars'\n" +
                        "• 'Paid 200 lei for gas'",
                suggestions = listOf(
                    "I spent 25 euros on groceries",
                    "Paid 40 dollars for gas",
                    "Bought lunch for 15 euros"
                ),
                actionType = GeminiAIService.ActionType.CONVERSATION
            )
        }
    }

    fun handleBudgetAnalysis(context: GeminiAIService.ConversationContext): GeminiAIService.AIResponse {
        val totalSpent = context.currentSpending
        val budget = context.monthlyBudget
        val remaining = budget - totalSpent

        val analysis = StringBuilder()
        analysis.append("📊 **Budget Analysis**\n\n")

        if (budget > 0) {
            val percentUsed = (totalSpent / budget) * 100
            analysis.append("💰 Monthly Budget: ${String.format("%.2f", budget)} ${context.userCurrency}\n")
            analysis.append("💸 Current Spending: ${String.format("%.2f", totalSpent)} ${context.userCurrency}\n")
            analysis.append("💵 Remaining: ${String.format("%.2f", remaining)} ${context.userCurrency}\n\n")

            when {
                percentUsed < 50 -> {
                    analysis.append("✅ Great job! You're using only ${String.format("%.1f", percentUsed)}% of your budget.")
                }
                percentUsed < 80 -> {
                    analysis.append("⚠️ You've used ${String.format("%.1f", percentUsed)}% of your budget. Keep an eye on spending.")
                }
                percentUsed < 100 -> {
                    analysis.append("🚨 Warning! You've used ${String.format("%.1f", percentUsed)}% of your budget.")
                }
                else -> {
                    analysis.append("🔴 You've exceeded your budget by ${String.format("%.2f", Math.abs(remaining))} ${context.userCurrency}!")
                }
            }
        } else {
            analysis.append("You haven't set a monthly budget yet. Would you like help setting one?")
        }

        val suggestions = mutableListOf<String>()
        if (budget <= 0) {
            suggestions.add("Help me set a budget")
        } else {
            if (totalSpent > budget * 0.8) {
                suggestions.add("How can I reduce spending?")
                suggestions.add("Show spending by category")
            }
            suggestions.add("Get saving tips")
            suggestions.add("Analyze my categories")
        }

        return GeminiAIService.AIResponse(
            message = analysis.toString(),
            suggestions = suggestions,
            actionType = GeminiAIService.ActionType.BUDGET_ANALYSIS
        )
    }

    fun handleAdviceRequest(context: GeminiAIService.ConversationContext): GeminiAIService.AIResponse {
        val advice = StringBuilder()
        advice.append("💡 **Financial Advice**\n\n")

        val suggestions = mutableListOf<String>()

        // Analyze spending patterns
        if (context.recentExpenses.isNotEmpty()) {
            val categorySpending = context.recentExpenses.groupBy { it.category }
                .mapValues { it.value.sumOf { expense -> expense.amount } }
                .toList()
                .sortedByDescending { it.second }

            advice.append("Based on your recent expenses:\n\n")

            if (categorySpending.isNotEmpty()) {
                val topCategory = categorySpending.first()
                advice.append("🔍 Your highest spending category is **${topCategory.first}** with ${String.format("%.2f", topCategory.second)} ${context.userCurrency}\n\n")

                when (topCategory.first.lowercase()) {
                    "food" -> {
                        advice.append("🍽️ Food Tips:\n")
                        advice.append("• Try meal planning and cooking at home\n")
                        advice.append("• Look for grocery store discounts\n")
                        advice.append("• Consider bulk buying for non-perishables\n")
                        suggestions.addAll(listOf("Meal planning tips", "Grocery budget strategies"))
                    }
                    "transportation" -> {
                        advice.append("🚗 Transportation Tips:\n")
                        advice.append("• Consider carpooling or public transport\n")
                        advice.append("• Plan efficient routes to save on fuel\n")
                        advice.append("• Look into transportation apps for deals\n")
                        suggestions.addAll(listOf("Public transport options", "Fuel saving tips"))
                    }
                    "entertainment" -> {
                        advice.append("🎬 Entertainment Tips:\n")
                        advice.append("• Look for free or low-cost activities\n")
                        advice.append("• Share streaming subscriptions with family\n")
                        advice.append("• Take advantage of happy hours and discounts\n")
                        suggestions.addAll(listOf("Free entertainment ideas", "Subscription management"))
                    }
                    else -> {
                        advice.append("💰 General Saving Tips:\n")
                        advice.append("• Set spending limits for this category\n")
                        advice.append("• Track your expenses more carefully\n")
                        advice.append("• Look for alternatives or better deals\n")
                        suggestions.addAll(listOf("Category budgeting", "Deal finding strategies"))
                    }
                }
            }
        } else {
            advice.append("Start tracking your expenses regularly to get personalized advice!\n\n")
            advice.append("📝 General Money Tips:\n")
            advice.append("• Set up a monthly budget\n")
            advice.append("• Track all your expenses\n")
            advice.append("• Review your spending weekly\n")
            advice.append("• Look for unnecessary subscriptions\n")

            suggestions.addAll(listOf(
                "Help me set a budget",
                "Track a new expense",
                "Show expense categories"
            ))
        }

        // Add encouragement
        if (Random.nextBoolean()) {
            advice.append("\n${ENCOURAGEMENT_PHRASES.random()}")
        }

        return GeminiAIService.AIResponse(
            message = advice.toString(),
            suggestions = suggestions,
            actionType = GeminiAIService.ActionType.SPENDING_ADVICE
        )
    }

    private fun handleCategoryManagement(context: GeminiAIService.ConversationContext): GeminiAIService.AIResponse {
        val response = StringBuilder()
        response.append("📂 **Your Expense Categories**\n\n")

        if (context.categories.isNotEmpty()) {
            response.append("Current categories:\n")
            context.categories.forEach { category ->
                response.append("• $category\n")
            }

            response.append("\nYou can organize your expenses using these categories. Would you like to see spending by category or need help with categorizing expenses?")
        } else {
            response.append("You don't have any custom categories set up yet. The default categories are:\n")
            response.append("• Food\n• Transportation\n• Housing\n• Entertainment\n• Utilities\n• Healthcare\n• Shopping\n• Other")
        }

        return GeminiAIService.AIResponse(
            message = response.toString(),
            suggestions = listOf(
                "Show spending by category",
                "Help me categorize an expense",
                "Add a new category"
            ),
            actionType = GeminiAIService.ActionType.CATEGORY_SUGGESTION
        )
    }

    private fun handleGeneralFinance(message: String): GeminiAIService.AIResponse {
        val responses = mapOf(
            "investment" to "💼 Investing is important for long-term wealth building. Start with emergency savings first, then consider low-cost index funds for beginners.",
            "saving" to "💰 Great question about saving! Try the 50/30/20 rule: 50% needs, 30% wants, 20% savings. Start small and increase gradually.",
            "debt" to "📉 Managing debt is crucial. Focus on high-interest debt first, consider the debt snowball method, and avoid taking on new debt.",
            "financial" to "📊 Financial planning involves budgeting, saving, investing, and protecting yourself with insurance. Start with tracking expenses and building an emergency fund.",
            "money" to "💵 Money management is a skill that improves with practice. The key is to spend less than you earn, track everything, and make informed decisions."
        )

        val relevantResponse = responses.entries.find { message.contains(it.key) }?.value
            ?: "That's a great financial question! I'm focused on helping you track expenses and manage your budget. For specific financial advice, consider consulting a financial advisor."

        return GeminiAIService.AIResponse(
            message = relevantResponse,
            suggestions = listOf(
                "Track my expenses",
                "Analyze my budget",
                "Get spending tips"
            ),
            actionType = GeminiAIService.ActionType.CONVERSATION
        )
    }

    private fun handleFallbackResponse(message: String): GeminiAIService.AIResponse {
        val fallbackResponses = listOf(
            "I'm here to help with your finances! You can ask me to track expenses, analyze your budget, or give spending advice.",
            "I didn't quite understand that. I can help you track expenses, manage your budget, or provide financial insights. What would you like to do?",
            "Let me help you with your finances! I can track expenses, analyze spending patterns, or give budget advice. What interests you most?",
            "I'm your financial assistant! I'm great at tracking expenses, budget analysis, and giving spending tips. How can I help you today?"
        )

        return GeminiAIService.AIResponse(
            message = fallbackResponses.random(),
            suggestions = listOf(
                "Track an expense",
                "Analyze my budget",
                "Get spending advice",
                "Show my categories"
            ),
            actionType = GeminiAIService.ActionType.CONVERSATION
        )
    }
}