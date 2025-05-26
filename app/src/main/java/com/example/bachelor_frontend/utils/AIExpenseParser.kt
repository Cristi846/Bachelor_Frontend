package com.example.bachelor_frontend.utils

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI-powered expense parser using OpenAI API
 * This is an advanced version that can handle more complex natural language
 */
class AIExpenseParser {

    // You'll need to add your OpenAI API key here
    private val apiKey = "YOUR_OPENAI_API_KEY_HERE"
    private val apiUrl = "https://api.openai.com/v1/chat/completions"

    data class AIExpenseResult(
        val amount: Double?,
        val currency: String?,
        val merchant: String?,
        val category: String?,
        val description: String?,
        val confidence: Float,
        val rawResponse: String
    )

    suspend fun parseExpenseWithAI(message: String, userCurrency: String = "USD"): AIExpenseResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = callOpenAI(message, userCurrency)
                parseAIResponse(response)
            } catch (e: Exception) {
                AIExpenseResult(
                    amount = null,
                    currency = null,
                    merchant = null,
                    category = "Other",
                    description = null,
                    confidence = 0.0f,
                    rawResponse = "Error: ${e.message}"
                )
            }
        }
    }

    private fun callOpenAI(message: String, userCurrency: String): String {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true

        val prompt = """
            Parse this expense message and extract the following information in JSON format:
            - amount (number): The monetary amount spent
            - currency (string): The currency code (USD, EUR, RON, etc.) - default to $userCurrency if not specified
            - merchant (string): Where the money was spent (store name, restaurant, etc.)
            - category (string): One of: Food, Transportation, Shopping, Entertainment, Healthcare, Utilities, Housing, Other
            - description (string): A brief description of the expense
            - confidence (number): Confidence level from 0.0 to 1.0

            Categories guide:
            - Food: restaurants, groceries, cafes, food delivery
            - Transportation: gas, taxi, bus, train, parking
            - Shopping: clothes, electronics, general purchases
            - Entertainment: movies, games, concerts, events
            - Healthcare: pharmacy, doctor, medical expenses
            - Utilities: bills, internet, phone, subscriptions
            - Housing: rent, furniture, home maintenance

            User message: "$message"

            Respond only with valid JSON in this format:
            {
                "amount": 50.0,
                "currency": "USD",
                "merchant": "McDonald's",
                "category": "Food",
                "description": "Lunch at McDonald's",
                "confidence": 0.9
            }
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 200)
            put("temperature", 0.1)
        }

        // Send request
        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(requestBody.toString())
        writer.flush()
        writer.close()

        // Read response
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()

        return response
    }

    private fun parseAIResponse(apiResponse: String): AIExpenseResult {
        try {
            val jsonResponse = JSONObject(apiResponse)
            val choices = jsonResponse.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")

            // Parse the JSON content
            val expenseJson = JSONObject(content.trim())

            return AIExpenseResult(
                amount = expenseJson.optDouble("amount").takeIf { !it.isNaN() },
                currency = expenseJson.optString("currency").takeIf { it.isNotEmpty() },
                merchant = expenseJson.optString("merchant").takeIf { it.isNotEmpty() },
                category = expenseJson.optString("category", "Other"),
                description = expenseJson.optString("description").takeIf { it.isNotEmpty() },
                confidence = expenseJson.optDouble("confidence", 0.0).toFloat(),
                rawResponse = content
            )
        } catch (e: Exception) {
            return AIExpenseResult(
                amount = null,
                currency = null,
                merchant = null,
                category = "Other",
                description = null,
                confidence = 0.0f,
                rawResponse = "Parse error: ${e.message}"
            )
        }
    }
}

/**
 * Hybrid parser that tries rule-based first, then falls back to AI
 */
class HybridExpenseParser {
    private val ruleBasedParser = ExpenseChatParser()
    private val aiParser = AIExpenseParser()

    suspend fun parseExpense(message: String, userCurrency: String = "USD"): ExpenseChatParser.ParsedExpense {
        // Try rule-based parsing first
        val ruleBasedResult = ruleBasedParser.parseExpenseMessage(message, userCurrency)

        // If confidence is high enough, use rule-based result
        if (ruleBasedResult.confidence >= 0.7f) {
            return ruleBasedResult
        }

        // Otherwise, try AI parsing
        try {
            val aiResult = aiParser.parseExpenseWithAI(message, userCurrency)

            // Convert AI result to ParsedExpense format
            return ExpenseChatParser.ParsedExpense(
                amount = aiResult.amount,
                currency = aiResult.currency,
                merchant = aiResult.merchant,
                category = aiResult.category,
                description = aiResult.description,
                confidence = aiResult.confidence
            )
        } catch (e: Exception) {
            // Fall back to rule-based result if AI fails
            return ruleBasedResult
        }
    }
}