// HybridAIChatScreen.kt - Works with both Gemini AI and fallback service
package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.bachelor_frontend.service.FallbackAIService
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.sql.Date
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridAIChatScreen(
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
    // Initialize both AI services
    val geminiService = remember { GeminiAIService() }
    val fallbackService = remember { FallbackAIService() }
    val coroutineScope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedBudgetType by remember { mutableStateOf(BudgetType.PERSONAL) }
    var useGemini by remember { mutableStateOf(true) } // Toggle between AI services
    var connectionStatus by remember { mutableStateOf("Checking AI connection...") }

    val listState = rememberLazyListState()
    val userFamily by familyViewModel.family.collectAsState()

    // Check AI service availability on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Test Gemini connection with a simple message
                val testContext = GeminiAIService.ConversationContext(
                    userId = userId,
                    userCurrency = userCurrency
                )
                geminiService.processComplexMessage("test", testContext)
                connectionStatus = "🤖 Advanced AI Connected"
                useGemini = true
            } catch (e: Exception) {
                connectionStatus = "🧠 Smart Assistant Ready"
                useGemini = false
            }
        }
    }

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
                        "💰 Track expenses naturally (\"I spent 50 euros at the grocery store\")\n" +
                        "📊 Analyze your budget and spending patterns\n" +
                        "💡 Provide personalized spending advice\n" +
                        "📈 Forecast your monthly expenses\n" +
                        "🎯 Set and manage financial goals\n\n" +
                        if (useGemini) {
                            "I'm powered by advanced AI and can understand complex conversations!"
                        } else {
                            "I use smart rules to understand your financial needs!"
                        } + "\n\nWhat would you like to do today?",
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

                val aiResponse = if (useGemini) {
                    try {
                        val conversationHistory = messages.takeLast(6).map {
                            "${if (it.isUser) "User" else "Assistant"}: ${it.content}"
                        }
                        geminiService.processComplexMessage(
                            message = userMessage,
                            context = context,
                            conversationHistory = conversationHistory
                        )
                    } catch (e: Exception) {
                        // Fallback to rule-based service if Gemini fails
                        useGemini = false
                        connectionStatus = "🧠 Smart Assistant (Fallback Mode)"
                        fallbackService.processMessage(userMessage, context)
                    }
                } else {
                    fallbackService.processMessage(userMessage, context)
                }

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
                        content = "I'm sorry, I encountered an error. Please try again. 🔄",
                        isUser = false
                    )
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun generateInsights() {
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

            val insights = if (useGemini) {
                try {
                    geminiService.generateBudgetInsights(context)
                } catch (e: Exception) {
                    fallbackService.handleBudgetAnalysis(context)
                }
            } else {
                fallbackService.handleBudgetAnalysis(context)
            }

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

    fun generateForecast() {
        if (!useGemini) {
            // For fallback service, provide a simple forecast
            processUserMessage("Can you analyze my spending and give me advice?")
            return
        }

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

            try {
                val forecast = geminiService.generateSpendingForecast(context)
                addMessage(
                    ChatMessage(
                        content = forecast.message,
                        isUser = false,
                        suggestions = forecast.suggestions,
                        actionType = forecast.actionType
                    )
                )
            } catch (e: Exception) {
                addMessage(
                    ChatMessage(
                        content = "I'll analyze your spending patterns and provide some insights based on your recent expenses.",
                        isUser = false
                    )
                )
                val insights = fallbackService.handleAdviceRequest(context)
                addMessage(
                    ChatMessage(
                        content = insights.message,
                        isUser = false,
                        suggestions = insights.suggestions,
                        actionType = insights.actionType
                    )
                )
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Financial Assistant")
                        Text(
                            text = connectionStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick insights button
                    IconButton(onClick = { generateInsights() }) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = "Budget Analysis",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Spending forecast button (only for Gemini)
                    if (useGemini) {
                        IconButton(onClick = { generateForecast() }) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = "Spending Forecast",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Settings menu
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear Conversation") },
                            onClick = {
                                messages = listOf(messages.first()) // Keep welcome message
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text(if (useGemini) "Use Simple Mode" else "Try Advanced AI") },
                            onClick = {
                                useGemini = !useGemini
                                connectionStatus = if (useGemini) "🤖 Advanced AI Connected" else "🧠 Smart Assistant Ready"
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (useGemini) Icons.Default.Psychology else Icons.Default.AutoAwesome,
                                    contentDescription = null
                                )
                            }
                        )
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add expenses to:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        FilterChip(
                            selected = selectedBudgetType == BudgetType.PERSONAL,
                            onClick = { selectedBudgetType = BudgetType.PERSONAL },
                            label = { Text("Personal") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        FilterChip(
                            selected = selectedBudgetType == BudgetType.FAMILY,
                            onClick = { selectedBudgetType = BudgetType.FAMILY },
                            label = { Text("Family") },
                            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) }
                        )
                    }
                }
            }

            // Quick action chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { processUserMessage("Show me my budget analysis") },
                        label = { Text("Budget Analysis") },
                        leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { processUserMessage("Give me spending advice") },
                        label = { Text("Spending Tips") },
                        leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { processUserMessage("Help me track an expense") },
                        label = { Text("Track Expense") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                }
                if (useGemini) {
                    item {
                        AssistChip(
                            onClick = { generateForecast() },
                            label = { Text("Forecast") },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    EnhancedChatMessageItem(
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
                                    content = "✅ Perfect! I've added your expense:\n\n" +
                                            "💰 ${expenseData.amount} ${expenseData.currency}\n" +
                                            "📂 ${expenseData.category}\n" +
                                            "📝 ${expenseData.description}\n" +
                                            if (selectedBudgetType == BudgetType.FAMILY) "👨‍👩‍👧‍👦 Added to family budget" else "👤 Added to personal budget",
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
                                    Text(
                                        text = if (useGemini) "AI is analyzing..." else "Processing your request...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Example prompts (only show when input is empty)
                    if (inputText.isEmpty()) {
                        Text(
                            text = "💡 Try saying:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(
                                listOf(
                                    "I spent 25 euros on groceries",
                                    "Analyze my budget",
                                    "How can I save money?",
                                    "Show spending by category"
                                )
                            ) { example ->
                                SuggestionChip(
                                    onClick = {
                                        inputText = example
                                        processUserMessage(example)
                                        inputText = ""
                                    },
                                    label = {
                                        Text(
                                            text = example,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (useGemini) {
                                        "Ask me anything about your finances..."
                                    } else {
                                        "Track expenses or ask for advice..."
                                    }
                                )
                            },
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
}

@Composable
fun EnhancedChatMessageItem(
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
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp, top = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Show expense creation button
                    if (message.expenseData != null && message.actionType == GeminiAIService.ActionType.CREATE_EXPENSE) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "💰 ${message.expenseData.amount} ${message.expenseData.currency}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📂 ${message.expenseData.category}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (message.expenseData.merchant != null) {
                                    Text(
                                        text = "🏪 ${message.expenseData.merchant}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onCreateExpense(message.expenseData) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add This Expense")
                                }
                            }
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
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 8.dp, top = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}