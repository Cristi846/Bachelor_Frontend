package com.example.bachelor_frontend.utils

import com.example.bachelor_frontend.classes.ExpenseDto
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern


class ExpenseChatParser {

    private val currencyPatterns = mapOf(
        "RON" to listOf("ron", "lei", "leu"),
        "EUR" to listOf("eur", "euro", "euros", "€"),
        "USD" to listOf("usd", "dollar", "dollars", "$"),
        "GBP" to listOf("gbp", "pound", "pounds", "£")
    )

    private val amountPatterns = listOf(
        Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(${currencyPatterns.values.flatten().joinToString("|")})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:value|cost|price|worth)\\s*(?:of|is)?\\s*(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:spent|paid|buy|bought)\\s*(?:for)?\\s*(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    )

    private val merchantPatterns = listOf(
        Pattern.compile("(?:from|at|in)\\s+([A-Za-z][A-Za-z0-9\\s&'.-]{1,30}?)(?:\\s+(?:for|in|on|with|value|cost|price|\\d))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([A-Za-z][A-Za-z0-9\\s&'.-]{1,30})\\s+(?:store|shop|restaurant|market|mall|station)", Pattern.CASE_INSENSITIVE)
    )

    private val categoryKeywords = mapOf(
        "Food" to listOf(
            "food", "eat", "restaurant", "cafe", "coffee", "lunch", "dinner", "breakfast",
            "grocery", "groceries", "supermarket", "market", "auchan", "carrefour", "lidl",
            "mcdonalds", "kfc", "pizza", "burger", "meal", "snack", "drink", "beverages"
        ),
        "Transportation" to listOf(
            "gas", "fuel", "petrol", "diesel", "transport", "bus", "taxi", "uber", "lyft",
            "train", "metro", "parking", "car", "vehicle", "station", "ticket", "fare"
        ),
        "Shopping" to listOf(
            "shopping", "clothes", "clothing", "shoes", "electronics", "phone", "laptop",
            "amazon", "online", "store", "mall", "purchase", "buy", "bought"
        ),
        "Entertainment" to listOf(
            "movie", "cinema", "theater", "concert", "game", "entertainment", "fun",
            "park", "museum", "ticket", "show", "event"
        ),
        "Healthcare" to listOf(
            "pharmacy", "medicine", "doctor", "hospital", "health", "medical", "clinic",
            "prescription", "drugs", "treatment"
        ),
        "Utilities" to listOf(
            "bill", "electricity", "water", "internet", "phone", "utility", "subscription",
            "netflix", "spotify", "service"
        ),
        "Housing" to listOf(
            "rent", "mortgage", "housing", "home", "apartment", "furniture", "repair",
            "maintenance", "cleaning"
        )
    )

    data class ParsedExpense(
        val amount: Double?,
        val currency: String?,
        val merchant: String?,
        val category: String?,
        val description: String?,
        val confidence: Float
    )

    fun parseExpenseMessage(message: String, defaultCurrency: String = "USD"): ParsedExpense {
        val cleanMessage = message.trim().toLowerCase()
        var confidence = 0.0f

        val (amount, currency) = extractAmountAndCurrency(message, defaultCurrency)
        if (amount != null) confidence += 0.4f

        val merchant = extractMerchant(message)
        if (merchant != null) confidence += 0.3f

        val category = categorizeExpense(cleanMessage, merchant)
        if (category != "Other") confidence += 0.2f

        val description = generateDescription(message, merchant, amount, currency)
        if (description.isNotEmpty()) confidence += 0.1f

        return ParsedExpense(
            amount = amount,
            currency = currency,
            merchant = merchant,
            category = category,
            description = description,
            confidence = confidence
        )
    }

    fun createExpenseFromParsed(
        parsed: ParsedExpense,
        userId: String,
        userCurrency: String = "USD"
    ): ExpenseDto? {
        if (parsed.amount == null) return null

        val finalAmount = if (parsed.currency != null && parsed.currency != userCurrency) {
            parsed.amount
        } else {
            parsed.amount
        }

        return ExpenseDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            amount = finalAmount,
            category = parsed.category ?: "Other",
            description = parsed.description ?: "Chat expense",
            timestamp = LocalDateTime.now(),
            receiptImageUrl = null
        )
    }

    private fun extractAmountAndCurrency(message: String, defaultCurrency: String): Pair<Double?, String?> {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)
                val amount = amountStr?.toDoubleOrNull()

                val currency = if (matcher.groupCount() > 1) {
                    findCurrencyFromText(matcher.group(2))
                } else {
                    findCurrencyFromText(message)
                } ?: defaultCurrency

                return Pair(amount, currency)
            }
        }
        return Pair(null, null)
    }

    private fun findCurrencyFromText(text: String?): String? {
        if (text == null) return null

        val lowerText = text.toLowerCase()
        for ((currency, keywords) in currencyPatterns) {
            if (keywords.any { lowerText.contains(it) }) {
                return currency
            }
        }
        return null
    }

    private fun extractMerchant(message: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()?.capitalize()
            }
        }
        return null
    }

    private fun categorizeExpense(message: String, merchant: String?): String {
        val textToAnalyze = "$message ${merchant ?: ""}".toLowerCase()

        var bestCategory = "Other"
        var maxMatches = 0

        for ((category, keywords) in categoryKeywords) {
            val matches = keywords.count { keyword ->
                textToAnalyze.contains(keyword)
            }
            if (matches > maxMatches) {
                maxMatches = matches
                bestCategory = category
            }
        }

        return bestCategory
    }

    private fun generateDescription(
        originalMessage: String,
        merchant: String?,
        amount: Double?,
        currency: String?
    ): String {
        return when {
            merchant != null && amount != null -> "Purchase at $merchant"
            merchant != null -> "Expense at $merchant"
            amount != null -> "Expense via chat"
            else -> originalMessage.take(50)
        }
    }

    fun getSuggestions(message: String): List<String> {
        val suggestions = mutableListOf<String>()
        val parsed = parseExpenseMessage(message)

        if (parsed.amount == null) {
            suggestions.add("💡 Try including the amount: \"I spent 50 dollars on...\"")
        }

        if (parsed.merchant == null) {
            suggestions.add("💡 Try mentioning where: \"I bought from Auchan...\"")
        }

        if (parsed.confidence < 0.5f) {
            suggestions.add("💡 Example: \"I bought groceries from Auchan for 200 lei\"")
            suggestions.add("💡 Example: \"Spent 50 euros at McDonald's for lunch\"")
        }

        return suggestions
    }
}