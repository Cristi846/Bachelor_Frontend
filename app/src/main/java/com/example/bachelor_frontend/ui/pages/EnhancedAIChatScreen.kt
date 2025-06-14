package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.service.GeminiAIService
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAIChatScreen(
    onExpenseCreated: (ExpenseDto, BudgetType) -> Unit,
    onBack: () -> Unit,
    userId: String,
    userCurrency: String,
    categories: List<String> = emptyList(),
    monthlyBudget: Double = 0.0,
    currentSpending: Double = 0.0,
    recentExpenses: List<ExpenseDto> = emptyList(),
    familyViewModel: FamilyViewModel = viewModel()
) {
    val aiService = remember { GeminiAIService() }
    val coroutineScope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedBudgetType by remember { mutableStateOf(BudgetType.PERSONAL) }

    val listState = rememberLazyListState()
    val userFamily by familyViewModel.family.collectAsState()

    // Scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Add welcome message
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                content = "👋 Hi! I'm your AI financial assistant. I can help you:\n\n" +
                        "💰 Track expenses (\"I spent 50 euros at the grocery store\")\n" +
                        "📊 Analyze your budget\n" +
                        "💡 Provide spending advice\n" +
                        "📈 Forecast your expenses\n\n" +
                        "What would you like to do today?",
                isUser = false
            )
        )
    }

    fun addMessage(message: ChatMessage) {
        messages = messages + message
    }

    fun processUserMessage(userMessage: String) {
        // Add user message
        addMessage(ChatMessage(content = userMessage, isUser = true))

        coroutineScope.launch {
            isLoading = true

            try {
                val context = GeminiAIService.ConversationContext(
                    userId = userId,
                    userCurrency = userCurrency,
                    recentExpenses = recentExpenses,
                    monthlyBudget = monthlyBudget,
                    currentSpending = currentSpending,
                    categories = categories
                )

                val conversationHistory = messages.takeLast(6).map {
                    "${if (it.isUser) "User" else "Assistant"}: ${it.content}"
                }

                val aiResponse = aiService.processComplexMessage(
                    message = userMessage,
                    context = context,
                    conversationHistory = conversationHistory
                )

                // Add AI response
                addMessage(
                    ChatMessage(
                        content = aiResponse.message,
                        isUser = false,
                        expenseData = aiResponse.expenseData,
                        suggestions = aiResponse.suggestions,
                        actionType = aiResponse.actionType
                    )
                )

            } catch (e: Exception) {
                addMessage(
                    ChatMessage(
                        content = "I'm sorry, I encountered an error. Please try again.",
                        isUser = false
                    )
                )
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Financial Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick action buttons
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val context = GeminiAIService.ConversationContext(
                                    userId = userId,
                                    userCurrency = userCurrency,
                                    recentExpenses = recentExpenses,
                                    monthlyBudget = monthlyBudget,
                                    currentSpending = currentSpending,
                                    categories = categories
                                )
                                val insights = aiService.generateBudgetInsights(context)
                                addMessage(
                                    ChatMessage(
                                        content = insights.message,
                                        isUser = false,
                                        suggestions = insights.suggestions,
                                        actionType = insights.actionType
                                    )
                                )
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Budget Analysis")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Budget type selector (if user has family)
            if (userFamily != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedBudgetType == BudgetType.PERSONAL,
                            onClick = { selectedBudgetType = BudgetType.PERSONAL },
                            label = { Text("Personal") }
                        )
                        FilterChip(
                            selected = selectedBudgetType == BudgetType.FAMILY,
                            onClick = { selectedBudgetType = BudgetType.FAMILY },
                            label = { Text("Family") }
                        )
                    }
                }
            }

            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(
                        message = message,
                        onCreateExpense = { expenseData ->
                            val expense = ExpenseDto(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                amount = expenseData.amount,
                                category = expenseData.category,
                                description = expenseData.description,
                                timestamp = LocalDateTime.now(),
                                budgetType = selectedBudgetType,
                                familyId = if (selectedBudgetType == BudgetType.FAMILY) userFamily?.id else null
                            )
                            onExpenseCreated(expense, selectedBudgetType)

                            addMessage(
                                ChatMessage(
                                    content = "✅ Expense added successfully! ${expenseData.amount} ${expenseData.currency} for ${expenseData.category}",
                                    isUser = false
                                )
                            )
                        },
                        onSuggestionClick = { suggestion ->
                            processUserMessage(suggestion)
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI is thinking...")
                                }
                            }
                        }
                    }
                }
            }

            // Input area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything about your finances...") },
                        maxLines = 3,
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                processUserMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCreateExpense: (GeminiAIService.ExpenseData) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp, top = 4.dp)
            )
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (message.isUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show expense creation button
                    if (message.expenseData != null && message.actionType == GeminiAIService.ActionType.CREATE_EXPENSE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onCreateExpense(message.expenseData) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Expense")
                        }
                    }

                    // Show suggestions
                    if (message.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        message.suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { onSuggestionClick(suggestion) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "${message.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }

        if (message.isUser) {
            Icon(
                Icons.Default.Person,
                contentDescription = "User",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}