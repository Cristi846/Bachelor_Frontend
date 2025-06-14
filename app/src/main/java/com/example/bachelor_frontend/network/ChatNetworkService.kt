package com.example.bachelor_frontend.network

import android.util.Log
import com.example.bachelor_frontend.classes.ExpenseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChatNetworkService {

    companion object {
        private const val BASE_URL = "http://192.168.1.11:9090/api" // Update with your IP
        private const val PARSE_ENDPOINT = "$BASE_URL/chat/parse"
        private const val CONFIRM_ENDPOINT = "$BASE_URL/chat/confirm"
        private const val SUGGESTIONS_ENDPOINT = "$BASE_URL/chat/suggestions"
        private const val HEALTH_ENDPOINT = "$BASE_URL/chat/health"
        private const val TEST_ENDPOINT = "$BASE_URL/chat/test"
    }

    data class ParseExpenseRequest(
        val message: String,
        val userId: String,
        val userCurrency: String
    )

    data class ParseExpenseResponse(
        val success: Boolean,
        val message: String,
        val expenseDto: ExpenseDto?,
        val confidence: Double?,
        val extractedData: Map<String, Any>?
    )

    suspend fun testConnection(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatNetwork", "Testing connection to: $HEALTH_ENDPOINT")

                val url = URL(HEALTH_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                Log.d("ChatNetwork", "Response code: ${connection.responseCode}")

                val response = if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                }

                Log.d("ChatNetwork", "Response: $response")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    Result.success("✅ Connection successful: $response")
                } else {
                    Result.failure(Exception("❌ HTTP ${connection.responseCode}: $response"))
                }
            } catch (e: Exception) {
                Log.e("ChatNetwork", "Connection failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testPost(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatNetwork", "Testing POST to: $TEST_ENDPOINT")

                val testData = JSONObject().apply {
                    put("test", "Hello from Android")
                }

                val response = makePostRequest(TEST_ENDPOINT, testData.toString())
                Result.success("✅ POST test successful: $response")
            } catch (e: Exception) {
                Log.e("ChatNetwork", "POST test failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun parseExpenseMessage(
        message: String,
        userId: String,
        userCurrency: String
    ): ParseExpenseResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatNetwork", "Parsing message: '$message' for user: $userId")

                val request = ParseExpenseRequest(message, userId, userCurrency)
                val response = makePostRequest(PARSE_ENDPOINT, request.toJson())

                Log.d("ChatNetwork", "Parse response: $response")
                parseExpenseResponseFromJson(response)
            } catch (e: Exception) {
                Log.e("ChatNetwork", "Parse request failed", e)
                ParseExpenseResponse(
                    success = false,
                    message = "Network error: ${e.message}",
                    expenseDto = null,
                    confidence = null,
                    extractedData = null
                )
            }
        }
    }

    private fun makePostRequest(urlString: String, jsonBody: String): String {
        Log.d("ChatNetwork", "POST to: $urlString")
        Log.d("ChatNetwork", "Body: $jsonBody")

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(jsonBody)
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        Log.d("ChatNetwork", "Response code: $responseCode")

        val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = reader.readText()
        reader.close()

        Log.d("ChatNetwork", "Response body: $response")

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP $responseCode: $response")
        }

        return response
    }

    private fun parseExpenseResponseFromJson(jsonString: String): ParseExpenseResponse {
        val json = JSONObject(jsonString)

        val expenseDto = if (json.has("expenseDto") && !json.isNull("expenseDto")) {
            val expenseJson = json.getJSONObject("expenseDto")
            ExpenseDto(
                id = expenseJson.optString("id", ""),
                userId = expenseJson.optString("userId", ""),
                amount = expenseJson.optDouble("amount", 0.0),
                category = expenseJson.optString("category", "Other"),
                description = expenseJson.optString("description", ""),
                timestamp = java.time.LocalDateTime.now(),
                receiptImageUrl = expenseJson.optString("receiptImageUrl", null)
            )
        } else null

        val extractedData = if (json.has("extractedData") && !json.isNull("extractedData")) {
            val dataJson = json.getJSONObject("extractedData")
            mapOf(
                "amount" to dataJson.optDouble("amount"),
                "currency" to dataJson.optString("currency"),
                "merchant" to dataJson.optString("merchant"),
                "category" to dataJson.optString("category")
            ).filterValues { it != null && it != "" }
        } else null

        return ParseExpenseResponse(
            success = json.optBoolean("success", false),
            message = json.optString("message", ""),
            expenseDto = expenseDto,
            confidence = if (json.has("confidence")) json.optDouble("confidence") else null,
            extractedData = extractedData
        )
    }

    private fun ParseExpenseRequest.toJson(): String {
        return JSONObject().apply {
            put("message", message)
            put("userId", userId)
            put("userCurrency", userCurrency)
        }.toString()
    }
}