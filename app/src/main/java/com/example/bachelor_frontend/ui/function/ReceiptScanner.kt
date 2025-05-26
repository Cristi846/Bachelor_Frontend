package com.example.bachelor_frontend.ui.function

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.bachelor_frontend.classes.ExpenseDto
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReceiptScanner(private val context: Context) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Romanian-specific patterns for merchant names ending in SA, SRL, S.R.L.
    private val romanianMerchantPatterns = listOf(
        // Pattern 1: Company name ending with SA (most common)
        Pattern.compile(
            "^\\s*([A-Z][A-Za-z\\s&'.-]{2,40})\\s+SA\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 2: Company name ending with SRL
        Pattern.compile(
            "^\\s*([A-Z][A-Za-z\\s&'.-]{2,40})\\s+SRL\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 3: Company name ending with S.R.L. (with dots)
        Pattern.compile(
            "^\\s*([A-Z][A-Za-z\\s&'.-]{2,40})\\s+S\\.R\\.L\\.\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 4: Full line with SA/SRL at the end
        Pattern.compile(
            "^\\s*([A-Z][A-Za-z\\s&'.-]{2,40}\\s+(?:SA|SRL|S\\.R\\.L\\.))\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 5: Store name at top (first few lines)
        Pattern.compile(
            "^\\s*([A-Z][A-Z\\s&'.-]{3,30})\\s*$",
            Pattern.MULTILINE
        )
    )

    // Romanian-specific TOTAL patterns - prioritizing "TOTAL Lei" format
    private val romanianTotalPatterns = listOf(
        // Pattern 1: TOTAL Lei with amount at end of line (HIGHEST PRIORITY)
        Pattern.compile(
            "(?:^|\\n)\\s*(?:TOTAL|Total)\\s+Lei.*?(\\d{1,5}[.,]\\d{2})\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 2: TOTAL Lei with amount anywhere on line
        Pattern.compile(
            "(?:TOTAL|Total)\\s+Lei[^\\n]*?(\\d{1,5}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        ),

        // Pattern 3: TOTAL with Lei and amount
        Pattern.compile(
            "(?:TOTAL|Total)[^\\n]*?(\\d{1,5}[.,]\\d{2})[^\\n]*Lei",
            Pattern.CASE_INSENSITIVE
        ),

        // Pattern 4: Traditional TOTAL line with amount at end
        Pattern.compile(
            "(?:^|\\n)\\s*(?:TOTAL|Total)[^\\n]*?(\\d{1,5}[.,]\\d{2})\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 5: TOTAL with colon and amount
        Pattern.compile(
            "(?:TOTAL|Total)\\s*:.*?(\\d{1,5}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        ),

        // Pattern 6: Simple TOTAL anywhere with amount
        Pattern.compile(
            "(?:TOTAL|Total).*?(\\d{1,5}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        )
    )

    suspend fun processReceiptImage(imageUri: Uri): ReceiptData {
        return withContext(Dispatchers.IO) {
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                val recognizedText = processImage(image)

                Log.d("ReceiptScanner", "Raw text from receipt:\n${recognizedText.text}")
                parseReceiptData(recognizedText)
            } catch (e: IOException) {
                Log.e("ReceiptScanner", "Error processing image", e)
                ReceiptData(
                    success = false,
                    error = "Failed to process image: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Error in receipt scanning", e)
                ReceiptData(
                    success = false,
                    error = "Error analyzing receipt: ${e.message}"
                )
            }
        }
    }

    private suspend fun processImage(image: InputImage): Text = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                continuation.resume(text)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    private fun parseReceiptData(text: Text): ReceiptData {
        val extractedText = text.text

        // PRIORITY 1: Try Romanian TOTAL Lei patterns first
        var amount = findRomanianTotalAmount(extractedText)

        // PRIORITY 2: If no TOTAL Lei found, try traditional TOTAL patterns
        if (amount <= 0) {
            amount = findTraditionalTotalAmount(extractedText)
        }

        // PRIORITY 3: Last resort - find largest reasonable amount (with better filtering)
        if (amount <= 0) {
            amount = findLargestReasonableAmount(extractedText)
        }

        val merchantName = findRomanianMerchantName(extractedText)
        val category = enhancedCategoryGuessing(extractedText, merchantName)

        Log.d("ReceiptScanner", "Extracted amount: $amount")
        Log.d("ReceiptScanner", "Extracted merchant: $merchantName")
        Log.d("ReceiptScanner", "Guessed category: $category")

        return if (amount > 0) {
            ReceiptData(
                success = true,
                amount = amount,
                merchantName = merchantName,
                category = category,
                rawText = extractedText
            )
        } else {
            Log.d("ReceiptScanner", "Could not find amount. Text blocks: ${text.textBlocks.size}")
            for (block in text.textBlocks) {
                Log.d("ReceiptScanner", "Block: ${block.text}")
            }

            ReceiptData(
                success = false,
                error = "Could not find a valid amount on the receipt",
                rawText = extractedText
            )
        }
    }

    private fun findRomanianTotalAmount(text: String): Double {
        Log.d("ReceiptScanner", "Searching for Romanian TOTAL Lei patterns...")

        for ((index, pattern) in romanianTotalPatterns.withIndex()) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                try {
                    val amountStr = matcher.group(1)?.trim() ?: continue
                    val normalizedAmount = amountStr.replace(",", ".")
                    val amount = normalizedAmount.toDouble()

                    if (amount > 0 && amount < 50000) { // Reasonable bounds for Romanian receipts
                        Log.d("ReceiptScanner", "Found TOTAL Lei amount $amount using pattern ${index + 1}")
                        Log.d("ReceiptScanner", "Matched text: ${matcher.group(0)}")
                        return amount
                    }
                } catch (e: NumberFormatException) {
                    Log.d("ReceiptScanner", "Failed to parse Romanian amount: ${matcher.group(1)}")
                    continue
                }
            }
        }

        return 0.0
    }

    private fun findTraditionalTotalAmount(text: String): Double {
        Log.d("ReceiptScanner", "Searching for traditional TOTAL patterns...")

        val lines = text.split("\n")
        for (i in lines.indices) {
            val line = lines[i].trim()

            // Look for lines starting with TOTAL or Total
            if (line.matches(Regex("(?i)^\\s*total.*"))) {
                Log.d("ReceiptScanner", "Found TOTAL line: $line")

                // Try to find amount at the END of this line first
                val endAmountPattern = Pattern.compile("(\\d{1,5}[.,]\\d{2})\\s*$")
                val endMatcher = endAmountPattern.matcher(line)
                if (endMatcher.find()) {
                    try {
                        val amountStr = endMatcher.group(1).trim()
                        val normalizedAmount = amountStr.replace(",", ".")
                        val amount = normalizedAmount.toDouble()
                        if (amount > 0) {
                            Log.d("ReceiptScanner", "Found amount at end of TOTAL line: $amount")
                            return amount
                        }
                    } catch (e: NumberFormatException) {
                        // Continue searching
                    }
                }

                // If no amount at end, look for any amount on the line
                val anyAmountPattern = Pattern.compile("(\\d{1,5}[.,]\\d{2})")
                val anyMatcher = anyAmountPattern.matcher(line)
                if (anyMatcher.find()) {
                    try {
                        val amountStr = anyMatcher.group(1).trim()
                        val normalizedAmount = amountStr.replace(",", ".")
                        val amount = normalizedAmount.toDouble()
                        if (amount > 0) {
                            Log.d("ReceiptScanner", "Found amount on TOTAL line: $amount")
                            return amount
                        }
                    } catch (e: NumberFormatException) {
                        // Continue to next line
                    }
                }

                // Check the next line if no amount found on current TOTAL line
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    val nextLineMatcher = anyAmountPattern.matcher(nextLine)
                    if (nextLineMatcher.find()) {
                        try {
                            val amountStr = nextLineMatcher.group(1).trim()
                            val normalizedAmount = amountStr.replace(",", ".")
                            val amount = normalizedAmount.toDouble()
                            if (amount > 0) {
                                Log.d("ReceiptScanner", "Found amount on line after TOTAL: $amount")
                                return amount
                            }
                        } catch (e: NumberFormatException) {
                            // Continue searching
                        }
                    }
                }
            }
        }

        return 0.0
    }

    private fun findLargestReasonableAmount(text: String): Double {
        Log.d("ReceiptScanner", "Searching for largest reasonable amount as fallback...")

        val amounts = mutableListOf<Double>()
        val amountPattern = Pattern.compile("\\b(\\d{1,5}[.,]\\d{2})\\b")
        val matcher = amountPattern.matcher(text)

        while (matcher.find()) {
            try {
                val amountStr = matcher.group(1)?.trim() ?: continue
                val normalizedAmount = amountStr.replace(",", ".")
                val amount = normalizedAmount.toDouble()

                // More restrictive bounds for fallback - avoid obviously wrong amounts
                if (amount >= 1.00 && amount <= 5000.0) {
                    amounts.add(amount)
                    Log.d("ReceiptScanner", "Found potential amount: $amount")
                }
            } catch (e: NumberFormatException) {
                // Skip invalid numbers
            }
        }

        // Return largest amount, but prefer amounts that are not too extreme
        val sortedAmounts = amounts.sortedDescending()

        // If we have multiple amounts, prefer one that's not the absolute largest
        // (sometimes VAT or other large numbers appear)
        return when {
            sortedAmounts.size >= 3 -> {
                // If largest is more than 3x the second largest, prefer second largest
                val largest = sortedAmounts[0]
                val secondLargest = sortedAmounts[1]
                if (largest > secondLargest * 3) {
                    Log.d("ReceiptScanner", "Largest amount seems too big, using second largest: $secondLargest")
                    secondLargest
                } else {
                    largest
                }
            }
            sortedAmounts.isNotEmpty() -> sortedAmounts[0]
            else -> 0.0
        }
    }

    private fun findRomanianMerchantName(text: String): String {
        Log.d("ReceiptScanner", "Searching for Romanian merchant name (SA/SRL)...")

        // Try Romanian-specific patterns first
        for ((index, pattern) in romanianMerchantPatterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim() ?: continue
                if (merchant.length > 2) {
                    val cleanedMerchant = cleanMerchantName(merchant)
                    Log.d("ReceiptScanner", "Found Romanian merchant using pattern ${index + 1}: $cleanedMerchant")
                    return cleanedMerchant
                }
            }
        }

        // Fallback: look in first 3 lines for company name
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        for ((index, line) in lines.take(3).withIndex()) {
            if (line.length > 3 &&
                !line.contains(Regex("\\d{2}[.,]\\d{2}")) && // No prices
                !line.matches(Regex("^\\d+.*")) && // Not starting with numbers
                !line.contains(Regex("(?i)(receipt|bon|fiscal|nr|data|ora)")) // Not receipt metadata
            ) {
                val cleanedLine = cleanMerchantName(line)
                if (cleanedLine.length > 3) {
                    Log.d("ReceiptScanner", "Found merchant in line ${index + 1}: $cleanedLine")
                    return cleanedLine
                }
            }
        }

        return ""
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("[^A-Za-z\\s&'.-]"), "") // Remove special chars except business ones
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
            .take(50) // Reasonable length limit
    }

    private fun enhancedCategoryGuessing(text: String, merchantName: String): String {
        val lowerText = text.lowercase()
        val lowerMerchant = merchantName.lowercase()

        // Romanian-aware category patterns
        val categoryPatterns = mapOf(
            "Food" to listOf(
                // Romanian stores
                "kaufland", "carrefour", "auchan", "mega image", "profi", "penny", "lidl",
                "selgros", "metro", "cora", "real", "hypermarket", "supermarket",
                // Food keywords
                "grocery", "alimentara", "restaurant", "cafe", "cofetarie", "brutarie",
                "pizza", "burger", "kfc", "mcdonald", "subway", "doner", "shaorma",
                "food", "mancare", "bautura", "lapte", "paine", "carne", "legume"
            ),
            "Transportation" to listOf(
                "petrom", "rompetrol", "omv", "mol", "lukoil", "shell", "bp",
                "benzina", "motorina", "combustibil", "parcare", "parking",
                "uber", "bolt", "taxi", "transport", "autobuz", "metrou",
                "bilet", "abonament", "cfr", "tarom"
            ),
            "Shopping" to listOf(
                "emag", "altex", "flanco", "media galaxy", "dedeman", "leroy merlin",
                "ikea", "jysk", "praktiker", "baumax", "bricostore",
                "mall", "afi", "baneasa", "plaza", "shopping", "magazine",
                "haine", "imbracaminte", "pantofi", "electronice", "mobila"
            ),
            "Healthcare" to listOf(
                "farmacie", "catena", "help net", "dona", "sensiblu", "farmacia tei",
                "spital", "clinica", "medic", "doctor", "dentist", "medicament",
                "reteta", "consultatii", "analize", "radiografie"
            ),
            "Utilities" to listOf(
                "enel", "electrica", "e-on", "gdf suez", "engie", "distrigaz",
                "apa", "nova", "rcs", "rds", "orange", "vodafone", "telekom",
                "digi", "upc", "internet", "telefon", "curent", "gaz", "apa"
            ),
            "Entertainment" to listOf(
                "cinema", "cinematograf", "bilet", "concert", "teatru", "opera",
                "muzeu", "parc", "distractii", "jocuri", "bowling", "karaoke",
                "netflix", "hbo", "pro tv", "antena", "sport", "fitness"
            )
        )

        val categoryScores = mutableMapOf<String, Int>()

        categoryPatterns.forEach { (category, keywords) ->
            var score = 0
            keywords.forEach { keyword ->
                if (lowerText.contains(keyword)) {
                    score += 2
                }
                if (lowerMerchant.contains(keyword)) {
                    score += 5
                }
                // Exact match gets highest score
                if (lowerMerchant == keyword || lowerMerchant.startsWith(keyword)) {
                    score += 10
                }
            }
            if (score > 0) {
                categoryScores[category] = score
            }
        }

        val result = categoryScores.maxByOrNull { it.value }?.key ?: "Other"
        Log.d("ReceiptScanner", "Category guessing - scores: $categoryScores, result: $result")
        return result
    }

    fun createExpenseFromReceipt(receiptData: ReceiptData, userId: String): ExpenseDto {
        return ExpenseDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            amount = receiptData.amount,
            category = receiptData.category,
            description = if (receiptData.merchantName.isNotEmpty())
                "Purchase at ${receiptData.merchantName}"
            else "Receipt Scan",
            timestamp = LocalDateTime.now(),
            receiptImageUrl = null
        )
    }

    data class ReceiptData(
        val success: Boolean,
        val amount: Double = 0.0,
        val merchantName: String = "",
        val category: String = "Other",
        val rawText: String = "",
        val error: String = ""
    )
}