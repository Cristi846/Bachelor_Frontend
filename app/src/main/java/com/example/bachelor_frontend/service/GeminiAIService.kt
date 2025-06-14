// Fixed GeminiAIService.kt with updated model name and error handling
package com.example.bachelor_frontend.service

import android.util.Log
import com.example.bachelor_frontend.classes.ExpenseDto
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.*
import com.example.bachelor_frontend.BuildConfig


class GeminiAIService {
    private val apiKey = BuildConfig.GEMINI_API_KEY // Replace with your actual API key

    // FIXED: Updated model name to the current one
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", // Changed from "gemini-pro"
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.3f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        }
    )

    companion object {
        private const val TAG = "GeminiAIService"
    }

    data class ConversationContext(
        val userId: String,
        val userCurrency: String,
        val recentExpenses: List<ExpenseDto> = emptyList(),
        val monthlyBudget: Double = 0.0,
        val currentSpending: Double = 0.0,
        val categories: List<String> = emptyList()
    )

    data class AIResponse(
        val message: String,
        val expenseData: ExpenseData? = null,
        val suggestions: List<String> = emptyList(),
        val actionType: ActionType = ActionType.CONVERSATION,
        val confidence: Float = 0.0f
    )

    data class ExpenseData(
        val amount: Double,
        val category: String,
        val description: String,
        val merchant: String? = null,
        val currency: String
    )

    enum class ActionType {
        CONVERSATION,
        CREATE_EXPENSE,
        BUDGET_ANALYSIS,
        SPENDING_ADVICE,
        CATEGORY_SUGGESTION
    }

    suspend fun processComplexMessage(
        message: String,
        context: ConversationContext,
        conversationHistory: List<String> = emptyList()
    ): AIResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing message with Gemini: $message")

                // Check if API key is properly set
                if (apiKey.isBlank()) {
                    Log.e(TAG, "Gemini API key not configured")
                    throw Exception("API key not configured")
                }

                val prompt = buildAdvancedPrompt(message, context, conversationHistory)
                Log.d(TAG, "Sending prompt to Gemini (first 200 chars): ${prompt.take(200)}...")

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""

                Log.d(TAG, "Received response from Gemini: $responseText")

                if (responseText.isBlank()) {
                    throw Exception("Empty response from Gemini")
                }

                parseAIResponse(responseText, context)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message with Gemini: ${e.message}", e)
                // Return a more specific error message
                AIResponse(
                    message = when {
                        e.message?.contains("API key") == true ->
                            "API configuration issue. Please check the setup."
                        e.message?.contains("not found") == true ->
                            "Service temporarily unavailable. Please try again."
                        e.message?.contains("quota") == true ->
                            "Service limit reached. Please try again later."
                        else -> "I'm having trouble understanding right now. Could you try rephrasing that?"
                    },
                    actionType = ActionType.CONVERSATION
                )
            }
        }
    }

    private fun buildAdvancedPrompt(
        message: String,
        context: ConversationContext,
        conversationHistory: List<String>
    ): String {
        val recentExpensesText = if (context.recentExpenses.isNotEmpty()) {
            context.recentExpenses.take(5).joinToString("\n") {
                "- ${it.category}: ${it.amount} ${context.userCurrency} (${it.description})"
            }
        } else "No recent expenses"

        val budgetInfo = if (context.monthlyBudget > 0) {
            "Monthly Budget: ${context.monthlyBudget} ${context.userCurrency}\n" +
                    "Current Spending: ${context.currentSpending} ${context.userCurrency}\n" +
                    "Remaining: ${context.monthlyBudget - context.currentSpending} ${context.userCurrency}"
        } else "No budget set"

        val historyText = if (conversationHistory.isNotEmpty()) {
            "Recent conversation:\n" + conversationHistory.takeLast(3).joinToString("\n")
        } else ""

        return """
You are an intelligent financial assistant for an expense tracking app. Analyze the user's message and respond appropriately.

User Context:
- Currency: ${context.userCurrency}
- Available Categories: ${context.categories.joinToString(", ")}
- $budgetInfo

Recent Expenses:
$recentExpensesText

$historyText

User Message: "$message"

Instructions:
1. If the user is describing an expense, extract the details and respond with JSON format:
{
  "type": "expense",
  "response": "Your conversational response",
  "expense": {
    "amount": number,
    "category": "category_name",
    "description": "description",
    "merchant": "merchant_name_if_mentioned",
    "currency": "${context.userCurrency}"
  },
  "confidence": 0.0-1.0
}

2. If the user is asking for budget advice or analysis, respond with:
{
  "type": "advice",
  "response": "Your advice and analysis",
  "suggestions": ["suggestion1", "suggestion2", "suggestion3"]
}

3. For general conversation about finances:
{
  "type": "conversation",
  "response": "Your helpful response"
}

Category Guidelines:
- Food: restaurants, groceries, delivery, cafes
- Transportation: gas, taxi, bus, parking, car maintenance
- Shopping: clothes, electronics, general purchases
- Entertainment: movies, games, events, subscriptions
- Healthcare: pharmacy, doctor, medical
- Utilities: bills, internet, phone
- Housing: rent, furniture, home maintenance
- Other: anything that doesn't fit above

Currency Recognition:
- "lei", "ron" → RON
- "euro", "euros", "€" → EUR  
- "dollar", "dollars", "$" → USD
- "pound", "pounds", "£" → GBP

Respond in a friendly, helpful manner. Always provide practical advice when appropriate.
        """.trimIndent()
    }

    private fun parseAIResponse(responseText: String, context: ConversationContext): AIResponse {
        return try {
            Log.d(TAG, "Parsing AI response: $responseText")

            // Try to find JSON within the response
            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}") + 1

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                // No JSON found, treat as plain text
                Log.d(TAG, "No JSON found in response, treating as plain text")
                return AIResponse(
                    message = responseText,
                    actionType = ActionType.CONVERSATION
                )
            }

            val jsonText = responseText.substring(jsonStart, jsonEnd)
            Log.d(TAG, "Extracted JSON: $jsonText")

            val jsonResponse = JSONObject(jsonText)
            val type = jsonResponse.optString("type", "conversation")
            val message = jsonResponse.optString("response", responseText)

            when (type) {
                "expense" -> {
                    val expenseJson = jsonResponse.optJSONObject("expense")
                    if (expenseJson != null) {
                        val expenseData = ExpenseData(
                            amount = expenseJson.optDouble("amount", 0.0),
                            category = expenseJson.optString("category", "Other"),
                            description = expenseJson.optString("description", "AI Expense"),
                            merchant = expenseJson.optString("merchant").takeIf { it.isNotEmpty() },
                            currency = expenseJson.optString("currency", context.userCurrency)
                        )

                        AIResponse(
                            message = message,
                            expenseData = expenseData,
                            actionType = ActionType.CREATE_EXPENSE,
                            confidence = jsonResponse.optDouble("confidence", 0.8).toFloat()
                        )
                    } else {
                        AIResponse(message = message, actionType = ActionType.CONVERSATION)
                    }
                }

                "advice" -> {
                    val suggestionsArray = jsonResponse.optJSONArray("suggestions")
                    val suggestions = mutableListOf<String>()

                    suggestionsArray?.let {
                        for (i in 0 until it.length()) {
                            suggestions.add(it.optString(i))
                        }
                    }

                    AIResponse(
                        message = message,
                        suggestions = suggestions,
                        actionType = ActionType.SPENDING_ADVICE
                    )
                }

                else -> {
                    AIResponse(message = message, actionType = ActionType.CONVERSATION)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON response, treating as plain text: ${e.message}")
            AIResponse(
                message = responseText,
                actionType = ActionType.CONVERSATION
            )
        }
    }

    suspend fun generateBudgetInsights(context: ConversationContext): AIResponse {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                Analyze this user's financial situation and provide insights:
                
                Monthly Budget: ${context.monthlyBudget} ${context.userCurrency}
                Current Spending: ${context.currentSpending} ${context.userCurrency}
                Remaining Budget: ${context.monthlyBudget - context.currentSpending} ${context.userCurrency}
                
                Recent Expenses:
                ${context.recentExpenses.take(10).joinToString("\n") {
                    "${it.category}: ${it.amount} ${context.userCurrency} - ${it.description}"
                }}
                
                Provide a comprehensive analysis including:
                1. Spending patterns
                2. Budget performance
                3. Areas for improvement
                4. Specific actionable advice
                
                Respond in JSON format:
                {
                  "type": "advice",
                  "response": "Your detailed analysis",
                  "suggestions": ["actionable_suggestion_1", "actionable_suggestion_2", "actionable_suggestion_3"]
                }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                parseAIResponse(response.text ?: "", context)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating budget insights", e)
                AIResponse(
                    message = "I'm having trouble analyzing your budget right now. Please try again later.",
                    actionType = ActionType.CONVERSATION
                )
            }
        }
    }

    suspend fun generateSpendingForecast(context: ConversationContext): AIResponse {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                Based on this user's spending patterns, provide a forecast and recommendations:
                
                Monthly Budget: ${context.monthlyBudget} ${context.userCurrency}
                Current Monthly Spending: ${context.currentSpending} ${context.userCurrency}
                
                Recent Spending by Category:
                ${context.recentExpenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                    .entries.joinToString("\n") { "${it.key}: ${it.value} ${context.userCurrency}" }}
                
                Provide:
                1. End-of-month spending forecast
                2. Risk assessment (will they exceed budget?)
                3. Specific recommendations to stay on track
                4. Suggested budget adjustments if needed
                
                Respond in JSON format with actionable insights.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                parseAIResponse(response.text ?: "", context)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating spending forecast", e)
                AIResponse(
                    message = "I couldn't generate a spending forecast right now. Please try again.",
                    actionType = ActionType.CONVERSATION
                )
            }
        }
    }
}

// Alternative: Completely remove Gemini dependency and use only fallback
class SimplifiedAIService {
    private val fallbackService = FallbackAIService()

    suspend fun processComplexMessage(
        message: String,
        context: GeminiAIService.ConversationContext,
        conversationHistory: List<String> = emptyList()
    ): GeminiAIService.AIResponse {
        // Convert context to fallback format and process
        return fallbackService.processMessage(message, context)
    }

    suspend fun generateBudgetInsights(context: GeminiAIService.ConversationContext): GeminiAIService.AIResponse {
        return fallbackService.processMessage("analyze my budget", context)
    }

    suspend fun generateSpendingForecast(context: GeminiAIService.ConversationContext): GeminiAIService.AIResponse {
        return fallbackService.processMessage("give me spending advice", context)
    }
}