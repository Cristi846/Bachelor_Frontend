package com.example.bachelor_frontend.service

import com.example.bachelor_frontend.classes.ExpenseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ConversationMemory(
    val lastExpenseDiscussed: ExpenseDto? = null,
    val currentTopic: ConversationTopic = ConversationTopic.GENERAL,
    val userPreferences: Map<String, String> = emptyMap(),
    val conversationStage: ConversationStage = ConversationStage.GREETING
)

enum class ConversationTopic {
    GENERAL,
    EXPENSE_TRACKING,
    BUDGET_ANALYSIS,
    SPENDING_ADVICE,
    CATEGORY_MANAGEMENT,
    FAMILY_FINANCES,
    RECEIPT_SCANNING
}

enum class ConversationStage {
    GREETING,
    INFORMATION_GATHERING,
    PROCESSING,
    CONFIRMATION,
    FOLLOW_UP
}

class ConversationManager {
    private val _memory = MutableStateFlow(ConversationMemory())
    val memory: StateFlow<ConversationMemory> = _memory

    private val conversationHistory = mutableListOf<Pair<String, LocalDateTime>>()

    fun updateTopic(topic: ConversationTopic) {
        _memory.value = _memory.value.copy(currentTopic = topic)
    }

    fun updateStage(stage: ConversationStage) {
        _memory.value = _memory.value.copy(conversationStage = stage)
    }

    fun setLastExpense(expense: ExpenseDto) {
        _memory.value = _memory.value.copy(lastExpenseDiscussed = expense)
    }

    fun addToHistory(message: String) {
        conversationHistory.add(message to LocalDateTime.now())
        // Keep only last 10 messages to avoid memory bloat
        if (conversationHistory.size > 10) {
            conversationHistory.removeAt(0)
        }
    }

    fun getRecentHistory(): List<String> {
        return conversationHistory.takeLast(5).map { it.first }
    }

    fun updatePreference(key: String, value: String) {
        val newPreferences = _memory.value.userPreferences.toMutableMap()
        newPreferences[key] = value
        _memory.value = _memory.value.copy(userPreferences = newPreferences)
    }

    fun resetConversation() {
        _memory.value = ConversationMemory()
        conversationHistory.clear()
    }
}