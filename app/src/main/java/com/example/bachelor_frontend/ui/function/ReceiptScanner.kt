package com.example.bachelor_frontend.ui.function

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
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
import kotlin.math.min
import kotlin.math.max

class ReceiptScanner(private val context: Context) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Enhanced merchant patterns with Romanian specifics
    private val romanianMerchantPatterns = listOf(
        // Company types with better boundaries
        Pattern.compile(
            "(?:^|\\n)\\s*([A-Z][A-Za-z\\s&'.-]{3,35})\\s+(?:SA|SRL|S\\.R\\.L\\.)\\s*(?:\\n|$)",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),
        // Chain stores
        Pattern.compile(
            "(?:^|\\n)\\s*(KAUFLAND|CARREFOUR|AUCHAN|MEGA\\s*IMAGE|PROFI|PENNY|LIDL|SELGROS|METRO|CORA|REAL)\\s*(?:\\n|$)",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),
        // Fast food chains
        Pattern.compile(
            "(?:^|\\n)\\s*(MC\\s*DONALD|KFC|BURGER\\s*KING|SUBWAY|PIZZA\\s*HUT|DOMINO|TACO\\s*BELL)\\s*(?:\\n|$)",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),
        // General merchant pattern with better context
        Pattern.compile(
            "(?:^|\\n)\\s*([A-Z][A-Z\\s&'.-]{4,25})\\s*(?:\\n|$)",
            Pattern.MULTILINE
        )
    )

    // Enhanced total patterns with more variations
    private val romanianTotalPatterns = listOf(
        // Total with Lei - exact format
        Pattern.compile(
            "(?:^|\\n)\\s*(?:TOTAL|Total|TOTAL\\s+DE\\s+PLATA)\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*(?:\\n|$)",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),
        // Total at end of line with currency
        Pattern.compile(
            "(?:TOTAL|Total).*?(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),
        // Total with colon/equals
        Pattern.compile(
            "(?:TOTAL|Total|SUMA)\\s*[:=]\\s*(\\d{1,6}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        ),
        // De plata (to pay) pattern
        Pattern.compile(
            "(?:DE\\s+PLATA|PLATA)\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        ),
        // Card payment amount
        Pattern.compile(
            "(?:CARD|Plata\\s+cu\\s+cardul).*?(\\d{1,6}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        ),
        // Last resort - any total pattern
        Pattern.compile(
            "(?:TOTAL|Total).*?(\\d{1,6}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
        )
    )

    data class ReceiptData(
        val success: Boolean,
        val amount: Double = 0.0,
        val merchantName: String = "",
        val category: String = "Other",
        val rawText: String = "",
        val error: String = "",
        val confidence: Float = 0.0f,
        val processedImageUri: String? = null
    )

    suspend fun processReceiptImage(imageUri: Uri): ReceiptData {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EnhancedReceiptScanner", "Starting receipt processing...")

                // Step 1: Load and preprocess the image
                val preprocessedBitmap = loadAndPreprocessImage(imageUri)
                if (preprocessedBitmap == null) {
                    return@withContext ReceiptData(
                        success = false,
                        error = "Failed to load image"
                    )
                }

                // Step 2: Create InputImage from preprocessed bitmap
                val image = InputImage.fromBitmap(preprocessedBitmap, 0)

                // Step 3: Process with MLKit
                val recognizedText = processImage(image)

                Log.d("EnhancedReceiptScanner", "Raw OCR text:\n${recognizedText.text}")

                // Step 4: Parse the receipt data with enhanced logic
                val result = parseReceiptDataEnhanced(recognizedText)

                // Clean up
                if (!preprocessedBitmap.isRecycled) {
                    preprocessedBitmap.recycle()
                }

                result
            } catch (e: IOException) {
                Log.e("EnhancedReceiptScanner", "Error processing image", e)
                ReceiptData(
                    success = false,
                    error = "Failed to process image: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e("EnhancedReceiptScanner", "Error in receipt scanning", e)
                ReceiptData(
                    success = false,
                    error = "Error analyzing receipt: ${e.message}"
                )
            }
        }
    }

    private fun loadAndPreprocessImage(imageUri: Uri): Bitmap? {
        return try {
            Log.d("EnhancedReceiptScanner", "Loading image from URI: $imageUri")

            // Load original bitmap
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e("EnhancedReceiptScanner", "Failed to decode bitmap from URI")
                return null
            }

            Log.d("EnhancedReceiptScanner", "Original image size: ${originalBitmap.width}x${originalBitmap.height}")

            // Apply preprocessing pipeline
            var processedBitmap = originalBitmap

            // 1. Fix orientation
            processedBitmap = fixOrientation(processedBitmap, imageUri)

            // 2. Resize if too large (max 2048px on longest side)
            processedBitmap = resizeIfNeeded(processedBitmap, 2048)

            // 3. Enhance contrast and brightness
            processedBitmap = enhanceContrast(processedBitmap)

            // 4. Apply noise reduction
            processedBitmap = reduceNoise(processedBitmap)

            // 5. Convert to grayscale for better OCR
            processedBitmap = convertToGrayscale(processedBitmap)

            Log.d("EnhancedReceiptScanner", "Processed image size: ${processedBitmap.width}x${processedBitmap.height}")

            // Clean up original if different
            if (processedBitmap != originalBitmap && !originalBitmap.isRecycled) {
                originalBitmap.recycle()
            }

            processedBitmap
        } catch (e: Exception) {
            Log.e("EnhancedReceiptScanner", "Error preprocessing image", e)
            null
        }
    }

    private fun fixOrientation(bitmap: Bitmap, imageUri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            Log.w("EnhancedReceiptScanner", "Could not fix orientation", e)
            bitmap
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (resizedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return resizedBitmap
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap? {
        val enhancedBitmap =
            bitmap.config?.let { Bitmap.createBitmap(bitmap.width, bitmap.height, it) }
        val canvas = enhancedBitmap?.let { Canvas(it) }
        val paint = Paint()

        // Increase contrast and brightness
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, 15f,  // Red
            0f, 1.5f, 0f, 0f, 15f,  // Green
            0f, 0f, 1.5f, 0f, 15f,  // Blue
            0f, 0f, 0f, 1f, 0f      // Alpha
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        if (canvas != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return enhancedBitmap
    }

    private fun reduceNoise(bitmap: Bitmap): Bitmap? {
        // Simple noise reduction by slight blur
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Apply a simple 3x3 blur kernel
        val result = IntArray(width * height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                    }
                }

                r /= 9
                g /= 9
                b /= 9

                result[y * width + x] = Color.rgb(r, g, b)
            }
        }

        val denoisedBitmap = bitmap.config?.let { Bitmap.createBitmap(width, height, it) }
        if (denoisedBitmap != null) {
            denoisedBitmap.setPixels(result, 0, width, 0, 0, width, height)
        }

        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return denoisedBitmap
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        return grayscaleBitmap
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

    private fun parseReceiptDataEnhanced(text: Text): ReceiptData {
        val extractedText = text.text
        var confidence = 0.0f

        // Enhanced amount detection with multiple attempts
        var amount = findRomanianTotalAmount(extractedText)
        if (amount > 0) confidence += 0.4f

        if (amount <= 0) {
            amount = findTraditionalTotalAmount(extractedText)
            if (amount > 0) confidence += 0.3f
        }

        if (amount <= 0) {
            amount = findAmountByPosition(text)
            if (amount > 0) confidence += 0.25f
        }

        if (amount <= 0) {
            amount = findLargestReasonableAmount(extractedText)
            if (amount > 0) confidence += 0.2f
        }

        // Enhanced merchant detection
        val merchantName = findMerchantNameEnhanced(extractedText, text)
        if (merchantName.isNotEmpty()) confidence += 0.3f

        // Enhanced category detection
        val category = enhancedCategoryGuessing(extractedText, merchantName)
        if (category != "Other") confidence += 0.2f

        Log.d("EnhancedReceiptScanner", "Final results - Amount: $amount, Merchant: $merchantName, Category: $category, Confidence: $confidence")

        return if (amount > 0) {
            ReceiptData(
                success = true,
                amount = amount,
                merchantName = merchantName,
                category = category,
                rawText = extractedText,
                confidence = confidence
            )
        } else {
            ReceiptData(
                success = false,
                error = "Could not find a valid amount on the receipt",
                rawText = extractedText,
                confidence = confidence
            )
        }
    }

    private fun findAmountByPosition(text: Text): Double {
        // Look for amounts in the bottom third of the receipt
        val textBlocks = text.textBlocks
        if (textBlocks.isEmpty()) return 0.0

        val maxY = textBlocks.maxOfOrNull { it.boundingBox?.bottom ?: 0 } ?: 0
        val bottomThird = maxY * 2 / 3

        val amountPattern = Pattern.compile("(\\d{1,6}[.,]\\d{2})")

        for (block in textBlocks) {
            val blockY = block.boundingBox?.top ?: 0
            if (blockY >= bottomThird) {
                val matcher = amountPattern.matcher(block.text)
                while (matcher.find()) {
                    try {
                        val amountStr = matcher.group(1)?.replace(",", ".") ?: continue
                        val amount = amountStr.toDouble()
                        if (amount >= 1.0 && amount <= 10000.0) {
                            Log.d("EnhancedReceiptScanner", "Found amount by position: $amount in bottom section")
                            return amount
                        }
                    } catch (e: NumberFormatException) {
                        continue
                    }
                }
            }
        }
        return 0.0
    }

    private fun findMerchantNameEnhanced(text: String, textStructure: Text): String {
        // First try structured detection from top of receipt
        val topLines = text.split("\n").take(5)

        for (line in topLines) {
            val cleanLine = line.trim()
            if (cleanLine.length > 3 &&
                !cleanLine.contains(Regex("\\d{2}[.,]\\d{2}")) &&
                !cleanLine.matches(Regex("^\\d+.*")) &&
                !cleanLine.contains(Regex("(?i)(bon|fiscal|nr|data|ora|receipt)"))) {

                // Check against known patterns
                for (pattern in romanianMerchantPatterns) {
                    val matcher = pattern.matcher(cleanLine)
                    if (matcher.find()) {
                        val merchant = matcher.group(1)?.trim() ?: continue
                        if (merchant.length > 2) {
                            return cleanMerchantName(merchant)
                        }
                    }
                }

                // If no pattern match but looks like a merchant name
                if (cleanLine.length in 4..35 && cleanLine.any { it.isLetter() }) {
                    return cleanMerchantName(cleanLine)
                }
            }
        }

        // Try to find merchant using text block positions (top of receipt)
        val textBlocks = textStructure.textBlocks
        if (textBlocks.isNotEmpty()) {
            val minY = textBlocks.minOfOrNull { it.boundingBox?.top ?: Int.MAX_VALUE } ?: 0
            val topSection = minY + 200 // Top 200 pixels

            for (block in textBlocks) {
                val blockY = block.boundingBox?.top ?: Int.MAX_VALUE
                if (blockY <= topSection) {
                    val cleanText = block.text.trim()
                    if (cleanText.length in 4..35 &&
                        cleanText.any { it.isLetter() } &&
                        !cleanText.contains(Regex("\\d{2}[.,]\\d{2}"))) {
                        return cleanMerchantName(cleanText)
                    }
                }
            }
        }

        return ""
    }

    private fun findRomanianTotalAmount(text: String): Double {
        Log.d("EnhancedReceiptScanner", "Searching for Romanian TOTAL patterns...")

        for ((index, pattern) in romanianTotalPatterns.withIndex()) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                try {
                    val amountStr = matcher.group(1)?.trim() ?: continue
                    val normalizedAmount = amountStr.replace(",", ".")
                    val amount = normalizedAmount.toDouble()

                    if (amount > 0 && amount < 100000) { // Increased upper limit
                        Log.d("EnhancedReceiptScanner", "Found amount $amount using pattern ${index + 1}")
                        Log.d("EnhancedReceiptScanner", "Matched text: '${matcher.group(0)}'")
                        return amount
                    }
                } catch (e: NumberFormatException) {
                    Log.d("EnhancedReceiptScanner", "Failed to parse amount: ${matcher.group(1)}")
                    continue
                }
            }
        }

        return 0.0
    }

    private fun findTraditionalTotalAmount(text: String): Double {
        Log.d("EnhancedReceiptScanner", "Searching for traditional TOTAL patterns...")

        val lines = text.split("\n")
        for (i in lines.indices) {
            val line = lines[i].trim()

            // Look for lines containing "total" (case insensitive)
            if (line.contains("total", ignoreCase = true) ||
                line.contains("suma", ignoreCase = true) ||
                line.contains("plata", ignoreCase = true)) {

                Log.d("EnhancedReceiptScanner", "Found total line: '$line'")

                // Look for amounts in this line and next few lines
                for (j in i..min(i + 2, lines.size - 1)) {
                    val searchLine = lines[j]
                    val amountPattern = Pattern.compile("(\\d{1,6}[.,]\\d{2})")
                    val matcher = amountPattern.matcher(searchLine)

                    while (matcher.find()) {
                        try {
                            val amountStr = matcher.group(1)?.replace(",", ".") ?: continue
                            val amount = amountStr.toDouble()
                            if (amount >= 1.0 && amount <= 50000.0) {
                                Log.d("EnhancedReceiptScanner", "Found amount $amount near total line")
                                return amount
                            }
                        } catch (e: NumberFormatException) {
                            continue
                        }
                    }
                }
            }
        }

        return 0.0
    }

    private fun findLargestReasonableAmount(text: String): Double {
        Log.d("EnhancedReceiptScanner", "Searching for largest reasonable amount...")

        val amounts = mutableListOf<Double>()
        val amountPattern = Pattern.compile("\\b(\\d{1,6}[.,]\\d{2})\\b")
        val matcher = amountPattern.matcher(text)

        while (matcher.find()) {
            try {
                val amountStr = matcher.group(1)?.replace(",", ".") ?: continue
                val amount = amountStr.toDouble()

                if (amount >= 1.00 && amount <= 50000.0) {
                    amounts.add(amount)
                    Log.d("EnhancedReceiptScanner", "Found potential amount: $amount")
                }
            } catch (e: NumberFormatException) {
                continue
            }
        }

        return when {
            amounts.isEmpty() -> 0.0
            amounts.size == 1 -> amounts[0]
            else -> {
                val sorted = amounts.sortedDescending()
                // If the largest amount is much larger than others, it might be a total
                val largest = sorted[0]
                val secondLargest = sorted.getOrNull(1) ?: 0.0

                if (largest > secondLargest * 2 && largest > 10.0) {
                    largest
                } else {
                    // Return the amount that appears most frequently
                    amounts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: largest
                }
            }
        }
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("[^A-Za-z\\s&'.-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(40)
    }

    private fun enhancedCategoryGuessing(text: String, merchantName: String): String {
        val lowerText = text.lowercase()
        val lowerMerchant = merchantName.lowercase()
        val combinedText = "$lowerText $lowerMerchant"

        val categoryPatterns = mapOf(
            "Food" to listOf(
                // Romanian stores
                "kaufland", "carrefour", "auchan", "mega image", "profi", "penny", "lidl",
                "selgros", "metro", "cora", "real", "hypermarket", "supermarket",
                "alimentara", "piata", "magazin alimentar",
                // Restaurants and fast food
                "restaurant", "cafe", "cafea", "cofetarie", "brutarie", "patiserie",
                "pizza", "burger", "kfc", "mcdonald", "subway", "doner", "shaorma",
                "food", "mancare", "bautura", "lapte", "paine", "carne", "legume",
                // Food items that might appear on receipt
                "milk", "bread", "meat", "vegetables", "fruit", "cheese"
            ),
            "Transportation" to listOf(
                // Gas stations
                "petrom", "rompetrol", "omv", "mol", "lukoil", "shell", "bp", "esso",
                "benzina", "motorina", "combustibil", "carburant", "fuel",
                // Parking and transport
                "parcare", "parking", "uber", "bolt", "taxi", "transport",
                "autobuz", "metrou", "stb", "ratb", "bilet", "abonament",
                "cfr", "tarom", "train", "airplane", "bus"
            ),
            "Shopping" to listOf(
                // Electronics and general retail
                "emag", "altex", "flanco", "media galaxy", "pc garage",
                "dedeman", "leroy merlin", "ikea", "jysk", "praktiker",
                "mall", "shopping", "magazin", "store",
                "haine", "imbracaminte", "pantofi", "shoes", "clothing",
                "electronice", "electronics", "mobila", "furniture"
            ),
            "Healthcare" to listOf(
                "farmacie", "farmacy", "catena", "help net", "dona", "sensiblu", "farmacia tei",
                "spital", "hospital", "clinica", "clinic", "medic", "doctor",
                "dentist", "medicament", "medicine", "reteta", "prescription",
                "consultatii", "consultation", "analize", "radiografie"
            ),
            "Utilities" to listOf(
                "enel", "electrica", "e-on", "gdf suez", "engie", "distrigaz",
                "apa", "water", "nova", "rcs", "rds", "orange", "vodafone",
                "telekom", "digi", "upc", "internet", "telefon", "phone",
                "curent", "electricity", "gaz", "gas", "factura", "bill"
            ),
            "Entertainment" to listOf(
                "cinema", "cinematograf", "movie", "bilet", "ticket", "concert",
                "teatru", "theater", "opera", "muzeu", "museum", "parc", "park",
                "distractii", "entertainment", "jocuri", "games", "bowling",
                "karaoke", "netflix", "hbo", "spotify", "subscription"
            )
        )

        var bestCategory = "Other"
        var maxScore = 0

        categoryPatterns.forEach { (category, keywords) ->
            var score = 0
            keywords.forEach { keyword ->
                val keywordCount = combinedText.split(keyword).size - 1
                score += keywordCount * 2

                // Bonus for exact merchant match
                if (lowerMerchant.contains(keyword)) {
                    score += 5
                }

                // Bonus for merchant name starting with keyword
                if (lowerMerchant.startsWith(keyword)) {
                    score += 3
                }
            }

            if (score > maxScore) {
                maxScore = score
                bestCategory = category
            }
        }

        Log.d("EnhancedReceiptScanner", "Category detection - Best: $bestCategory (score: $maxScore)")
        return bestCategory
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
}