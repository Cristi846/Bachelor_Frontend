package com.example.bachelor_frontend.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.bachelor_frontend.classes.ExpenseDto
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility class for exporting financial data to various formats
 */
class DataExportUtil {

    companion object {
        /**
         * Export expenses to CSV format
         *
         * @param context The application context
         * @param expenses List of expenses to export
         * @param uri The URI of the file to save to (from document picker)
         * @return Success message or error message
         */
        fun exportToCSV(context: Context, expenses: List<ExpenseDto>, uri: Uri?): String {
            if (uri == null) {
                return "No file location selected"
            }

            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream)

                    // Write the CSV header
                    writer.append("ID,USER_ID,AMOUNT,CATEGORY,DESCRIPTION,DATE,TIME,RECEIPT_IMAGE_URL\n")

                    // Format the date and time
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

                    // Write each expense as a CSV line
                    expenses.forEach { expense ->
                        writer.append(expense.id).append(",")
                        writer.append(expense.userId).append(",")
                        writer.append(expense.amount.toString()).append(",")
                        writer.append(expense.category).append(",")
                        writer.append("\"").append(expense.description.replace("\"", "\"\"")).append("\",")
                        writer.append(expense.timestamp.format(dateFormatter)).append(",")
                        writer.append(expense.timestamp.format(timeFormatter)).append(",")
                        writer.append(expense.receiptImageUrl ?: "").append("\n")
                    }

                    writer.flush()
                    writer.close()
                    outputStream.close()

                    return "Successfully exported ${expenses.size} expenses to CSV"
                } else {
                    return "Failed to open output stream"
                }
            } catch (e: Exception) {
                return "Error exporting data: ${e.message}"
            }
        }

        /**
         * Export expenses to JSON format
         *
         * @param context The application context
         * @param expenses List of expenses to export
         * @param uri The URI of the file to save to (from document picker)
         * @return Success message or error message
         */
        fun exportToJSON(context: Context, expenses: List<ExpenseDto>, uri: Uri?): String {
            if (uri == null) {
                return "No file location selected"
            }

            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream)

                    // Create a JSON array to hold all expenses
                    val jsonArray = JSONArray()

                    // Format the date and time
                    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                    // Convert each expense to a JSON object
                    expenses.forEach { expense ->
                        val jsonExpense = JSONObject().apply {
                            put("id", expense.id)
                            put("userId", expense.userId)
                            put("amount", expense.amount)
                            put("category", expense.category)
                            put("description", expense.description)
                            put("timestamp", expense.timestamp.format(dateTimeFormatter))
                            put("receiptImageUrl", expense.receiptImageUrl ?: JSONObject.NULL)
                        }
                        jsonArray.put(jsonExpense)
                    }

                    // Create the root object containing the expenses array
                    val rootObject = JSONObject()
                    rootObject.put("expenses", jsonArray)

                    // Add some metadata
                    rootObject.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .format(Date()))
                    rootObject.put("totalExpenses", expenses.size)
                    rootObject.put("totalAmount", expenses.sumOf { it.amount })

                    // Write to file with pretty formatting
                    writer.write(rootObject.toString(4))
                    writer.flush()
                    writer.close()
                    outputStream.close()

                    return "Successfully exported ${expenses.size} expenses to JSON"
                } else {
                    return "Failed to open output stream"
                }
            } catch (e: Exception) {
                return "Error exporting data: ${e.message}"
            }
        }

        /**
         * Generate a default filename for the export
         *
         * @param format The file format (csv, json, pdf)
         * @return A default filename with current date
         */
        fun generateDefaultFilename(format: String): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            return "finance_tracker_export_$dateStr.$format".lowercase()
        }
    }
}