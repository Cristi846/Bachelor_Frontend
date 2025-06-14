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

    private val romanianTotalPatterns = listOf(
        // Pattern 1: "TOTAL" followed by amount on same line
        Pattern.compile(
            "^\\s*TOTAL\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 2: "Total" followed by amount on same line
        Pattern.compile(
            "^\\s*Total\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 3: "TOTAL LEI" followed by amount
        Pattern.compile(
            "^\\s*TOTAL\\s+LEI\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 4: "TOTAL DE PLATA" followed by amount
        Pattern.compile(
            "^\\s*TOTAL\\s+DE\\s+PLATA\\s*[:=]?\\s*(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 5: Amount after "TOTAL" with possible spaces/tabs
        Pattern.compile(
            "^\\s*TOTAL[\\s\\t]*([\\d]{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
        ),

        // Pattern 6: "Total:" followed by amount
        Pattern.compile(
            "^\\s*Total\\s*:\\s*(\\d{1,6}[.,]\\d{2})\\s*(?:RON|LEI|lei)?\\s*$",
            Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
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
        Log.d("ReceiptScanner", "=== STARTING RECEIPT ANALYSIS ===")
        Log.d("ReceiptScanner", "Full OCR text:\n$extractedText")
        Log.d("ReceiptScanner", "=== END OCR TEXT ===")

        var confidence = 0.0f

        // Try Romanian-specific total detection first
        var amount = findRomanianTotalAmount(extractedText)
        if (amount > 0) {
            confidence += 0.8f // High confidence for Romanian format
            Log.d("ReceiptScanner", "Romanian total found: $amount, confidence: $confidence")
        }

        // Fallback to position-based detection if needed
        if (amount <= 0) {
            amount = findAmountByPosition(text)
            if (amount > 0) confidence += 0.4f
        }

        // Try merchant detection
        val merchantName = findRomanianMerchantName(extractedText)
        if (merchantName.isNotEmpty()) confidence += 0.2f

        // Determine category based on merchant
        val category = categorizeMerchant(merchantName)

        Log.d("ReceiptScanner", "Final results - Amount: $amount, Merchant: $merchantName, Category: $category, Confidence: $confidence")

        return ReceiptData(
            success = amount > 0,
            amount = amount,
            merchantName = merchantName,
            category = category,
            rawText = extractedText,
            confidence = confidence,
            error = if (amount <= 0) "Could not detect total amount" else ""
        )
    }

    // Helper method to categorize based on merchant
    private fun categorizeMerchant(merchantName: String): String {
        val upperMerchant = merchantName.uppercase()
        return when {
            upperMerchant.contains("KAUFLAND") || upperMerchant.contains("CARREFOUR") ||
                    upperMerchant.contains("AUCHAN") || upperMerchant.contains("MEGA") ||
                    upperMerchant.contains("PROFI") || upperMerchant.contains("PENNY") ||
                    upperMerchant.contains("LIDL") -> "Food"

            upperMerchant.contains("PETROM") || upperMerchant.contains("ROMPETROL") ||
                    upperMerchant.contains("OMV") -> "Transportation"

            upperMerchant.contains("EMAG") || upperMerchant.contains("ALTEX") ||
                    upperMerchant.contains("FLANCO") -> "Shopping"

            upperMerchant.contains("CINEMA") || upperMerchant.contains("MALL") -> "Entertainment"

            upperMerchant.contains("FARMACI") || upperMerchant.contains("CATENA") ||
                    upperMerchant.contains("SENSIBLU") -> "Healthcare"

            else -> "Other"
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

    // Enhanced parsing method for Romanian receipts
    private fun findRomanianTotalAmount(text: String): Double {
        Log.d("ReceiptScanner", "Searching for Romanian total in text:\n$text")

        // Split text into lines for line-by-line analysis
        val lines = text.split('\n')
        Log.d("ReceiptScanner", "Total lines to analyze: ${lines.size}")

        // First pass: Look for lines that start with TOTAL variants
        for (i in lines.indices) {
            val line = lines[i].trim()
            Log.d("ReceiptScanner", "Analyzing line $i: '$line'")

            // Check if line starts with TOTAL (case insensitive)
            if (line.uppercase().startsWith("TOTAL")) {
                Log.d("ReceiptScanner", "Found TOTAL line: '$line'")

                // Try to extract amount from this line
                val amount = extractAmountFromTotalLine(line)
                if (amount > 0) {
                    Log.d("ReceiptScanner", "Successfully extracted amount: $amount from line: '$line'")
                    return amount
                }

                // If no amount on same line, check next line
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    Log.d("ReceiptScanner", "Checking next line: '$nextLine'")
                    val nextLineAmount = extractAmountFromLine(nextLine)
                    if (nextLineAmount > 0) {
                        Log.d("ReceiptScanner", "Found amount on next line: $nextLineAmount")
                        return nextLineAmount
                    }
                }
            }
        }

        // Second pass: Use regex patterns as fallback
        for (pattern in romanianTotalPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group(1)
                if (amountStr != null) {
                    try {
                        val amount = amountStr.replace(',', '.').toDouble()
                        Log.d("ReceiptScanner", "Found amount via regex: $amount from pattern: ${pattern.pattern()}")
                        if (amount > 0) return amount
                    } catch (e: NumberFormatException) {
                        Log.w("ReceiptScanner", "Could not parse amount: $amountStr")
                    }
                }
            }
        }

        Log.d("ReceiptScanner", "No Romanian total amount found")
        return 0.0
    }

    // Enhanced merchant detection for Romanian stores
    private fun findRomanianMerchantName(text: String): String {
        val lines = text.split('\n')

        // Look for Romanian chain stores first
        val romanianChains = listOf(
            "KAUFLAND", "CARREFOUR", "AUCHAN", "MEGA IMAGE", "PROFI",
            "PENNY", "LIDL", "SELGROS", "METRO", "CORA", "REAL"
        )

        for (line in lines.take(10)) { // Check first 10 lines
            val upperLine = line.trim().uppercase()
            for (chain in romanianChains) {
                if (upperLine.contains(chain)) {
                    Log.d("ReceiptScanner", "Found Romanian chain: $chain")
                    return chain
                }
            }
        }

        // Look for company types (SA, SRL)
        for (line in lines.take(5)) { // Check first 5 lines for company names
            val companyPattern = Pattern.compile(
                "([A-Z][A-Za-z\\s&'.-]{3,25})\\s+(?:SA|SRL|S\\.R\\.L\\.)",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = companyPattern.matcher(line.trim())
            if (matcher.find()) {
                val companyName = matcher.group(1)?.trim()
                if (companyName != null && companyName.length > 3) {
                    Log.d("ReceiptScanner", "Found company: $companyName")
                    return companyName
                }
            }
        }

        return ""
    }

    private fun extractAmountFromTotalLine(line: String): Double {
        // Remove TOTAL prefix and common words
        val cleanLine = line.uppercase()
            .replace("TOTAL", "")
            .replace("LEI", "")
            .replace("RON", "")
            .replace("DE", "")
            .replace("PLATA", "")
            .replace(":", "")
            .replace("=", "")
            .trim()

        Log.d("ReceiptScanner", "Cleaned TOTAL line: '$cleanLine'")

        return extractAmountFromLine(cleanLine)
    }

    // Extract numeric amount from any line
    private fun extractAmountFromLine(line: String): Double {
        // Pattern to match Romanian currency format: 123.45 or 123,45
        val amountPattern = Pattern.compile("(\\d{1,6}[.,]\\d{2})")
        val matcher = amountPattern.matcher(line)

        if (matcher.find()) {
            val amountStr = matcher.group(1)
            return try {
                val normalizedAmount = amountStr?.replace(',', '.')
                val amount = normalizedAmount?.toDouble() ?: 0.0
                Log.d("ReceiptScanner", "Extracted amount: $amount from line: '$line'")
                amount
            } catch (e: NumberFormatException) {
                Log.w("ReceiptScanner", "Failed to parse amount: $amountStr from line: '$line'")
                0.0
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