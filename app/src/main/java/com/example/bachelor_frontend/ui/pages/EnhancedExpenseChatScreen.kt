package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.network.ChatNetworkService
import com.example.bachelor_frontend.utils.ExpenseChatParser
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedExpenseChatScreen(
    onExpenseCreated: (ExpenseDto) -> Unit,
    onBack: () -> Unit,
    userId: String,
    userCurrency: String = "USD",
    useBackend: Boolean = false // Toggle between local parsing and backend
) {
    val localParser = remember { ExpenseChatParser() }
    val networkService = remember { ChatNetworkService() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var messages by remember {
        mutableStateOf(listOf(
            ChatMessage(
                content = "Hi! I'm your AI expense assistant. You can tell me about your purchases and I'll help track them.\n\n" +
                        if (useBackend) "🤖 Using AI-powered parsing" else "🔧 Using local parsing" +
                                "\n\nTry saying:\n• \"I bought groceries from Auchan for 200 lei\"\n• \"Spent 50 euros at McDonald's\"\n• \"Gas station 40 dollars\"",
                isUser = false
            )
        ))
    }

    // Currency formatter
    val currencyFormat = remember(userCurrency) {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(userCurrency)
        }
    }

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun handleUserMessage(messageText: String) {
        if (messageText.isBlank() || isProcessing) return

        // Add user message immediately
        val userMessage = ChatMessage(
            content = messageText,
            isUser = true
        )
        messages = messages + userMessage

        // Show typing indicator
        val typingMessage = ChatMessage(
            content = "🤔 Analyzing your message...",
            isUser = false,
            id = "typing"
        )
        messages = messages + typingMessage

        isProcessing = true

        coroutineScope.launch {
            try {
                val (responseMessage, expense) = if (useBackend) {
                    handleMessageWithBackend(messageText, userId, userCurrency, networkService)
                } else {
                    handleMessageWithLocalParser(messageText, userId, userCurrency, localParser)
                }

                // Remove typing indicator and add actual response
                messages = messages.dropLast(1) + responseMessage

                // If an expense was created, save it
                expense?.let { onExpenseCreated(it) }

            } catch (e: Exception) {
                // Remove typing indicator and show error
                val errorMessage = ChatMessage(
                    content = "Sorry, I encountered an error: ${e.message}\n\nPlease try again or rephrase your message.",
                    isUser = false
                )
                messages = messages.dropLast(1) + errorMessage
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (useBackend) Icons.Default.CloudDone else Icons.Default.Devices,
                            contentDescription = "AI Assistant",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text("Expense Assistant")
                            Text(
                                text = if (useBackend) "AI-Powered" else "Local Processing",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add test button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val testResult = networkService.testConnection()
                                    testResult.fold(
                                        onSuccess = { result ->
                                            messages = messages + ChatMessage(
                                                content = "🔧 Connection Test:\n$result",
                                                isUser = false
                                            )
                                        },
                                        onFailure = { error ->
                                            messages = messages + ChatMessage(
                                                content = "🔧 Connection Test Failed:\n${error.message}",
                                                isUser = false
                                            )
                                        }
                                    )
                                } catch (e: Exception) {
                                    messages = messages + ChatMessage(
                                        content = "🔧 Test Error: ${e.message}",
                                        isUser = false
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Test Connection")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Show processing indicator
                    if (isProcessing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Tell me about your expense...") },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    handleUserMessage(messageText)
                                    messageText = ""
                                    keyboardController?.hide()
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = {
                                handleUserMessage(messageText)
                                messageText = ""
                                keyboardController?.hide()
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = if (messageText.isNotBlank() && !isProcessing)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (messageText.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                EnhancedChatMessageBubble(
                    message = message,
                    currencyFormat = currencyFormat,
                    onConfirmExpense = { expense ->
                        onExpenseCreated(expense)
                        messages = messages + ChatMessage(
                            content = "✅ Expense added successfully!\n\n${currencyFormat.format(expense.amount)} spent on ${expense.category}",
                            isUser = false
                        )
                    },
                    onSuggestionClick = { suggestion ->
                        messageText = suggestion
                    },
                    userId = userId,
                    userCurrency = userCurrency,
                    localParser = localParser
                )
            }
        }
    }
}

private suspend fun handleMessageWithBackend(
    messageText: String,
    userId: String,
    userCurrency: String,
    networkService: ChatNetworkService
): Pair<ChatMessage, ExpenseDto?> {
    val response = networkService.parseExpenseMessage(messageText, userId, userCurrency)

    val botMessage = ChatMessage(
        content = response.message,
        isUser = false,
        networkResponse = response
    )

    val expense = if (response.success && response.expenseDto != null) {
        response.expenseDto
    } else null

    return Pair(botMessage, expense)
}

private fun handleMessageWithLocalParser(
    messageText: String,
    userId: String,
    userCurrency: String,
    parser: ExpenseChatParser
): Pair<ChatMessage, ExpenseDto?> {
    val parsed = parser.parseExpenseMessage(messageText, userCurrency)
    val suggestions = parser.getSuggestions(messageText)

    val (responseContent, expense) = generateLocalResponse(parsed, parser, userCurrency, userId)

    val botMessage = ChatMessage(
        content = responseContent,
        isUser = false,
        parsedExpense = if (parsed.amount != null) parsed else null,
        suggestions = suggestions
    )

    return Pair(botMessage, expense)
}

private fun generateLocalResponse(
    parsed: ExpenseChatParser.ParsedExpense,
    parser: ExpenseChatParser,
    userCurrency: String,
    userId: String
): Pair<String, ExpenseDto?> {
    return when {
        parsed.amount != null && parsed.confidence >= 0.6f -> {
            val expense = parser.createExpenseFromParsed(parsed, userId, userCurrency)
            val currencyFormat = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance(userCurrency)
            }

            val response = buildString {
                append("Got it! I've extracted:\n\n")
                append("💰 Amount: ${currencyFormat.format(parsed.amount)}\n")
                if (parsed.merchant != null) append("🏪 Merchant: ${parsed.merchant}\n")
                append("📂 Category: ${parsed.category ?: "Other"}\n\n")
                append("Would you like me to add this expense?")
            }

            Pair(response, expense)
        }

        parsed.amount != null && parsed.confidence >= 0.3f -> {
            val response = buildString {
                append("I think I understood:\n\n")
                append("💰 Amount: ${parsed.amount} ${parsed.currency ?: userCurrency}\n")
                if (parsed.merchant != null) append("🏪 From: ${parsed.merchant}\n")
                append("📂 Category: ${parsed.category ?: "Other"}\n\n")
                append("Is this correct? If yes, I can add it. If not, please provide more details.")
            }

            Pair(response, null)
        }

        else -> {
            val response = buildString {
                append("I need a bit more information. ")
                if (parsed.amount == null) {
                    append("Could you tell me the amount you spent? ")
                }
                append("\n\nTry something like:\n")
                append("• \"I spent 50 euros at McDonald's\"\n")
                append("• \"Bought groceries for 200 lei\"\n")
                append("• \"Gas station 40 dollars\"")
            }

            Pair(response, null)
        }
    }
}

@Composable
fun EnhancedChatMessageBubble(
    message: ChatMessage,
    currencyFormat: NumberFormat,
    onConfirmExpense: (ExpenseDto) -> Unit,
    onSuggestionClick: (String) -> Unit,
    userId: String,
    userCurrency: String,
    localParser: ExpenseChatParser
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (message.isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Text(
                text = message.content,
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        // Network response confirmation
        if (!message.isUser && message.networkResponse != null && message.networkResponse.success) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Expense Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    message.networkResponse.expenseDto?.let { expense ->
                        Text("Amount: ${currencyFormat.format(expense.amount)}")
                        Text("Category: ${expense.category}")
                        if (expense.description.isNotEmpty()) {
                            Text("Description: ${expense.description}")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onConfirmExpense(expense) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Expense")
                        }
                    }
                }
            }
        }

        // Local parser confirmation (same as before)
        if (!message.isUser && message.parsedExpense != null && message.parsedExpense.amount != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Expense Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Amount: ${currencyFormat.format(message.parsedExpense.amount)}")
                    Text("Category: ${message.parsedExpense.category ?: "Other"}")
                    if (message.parsedExpense.merchant != null) {
                        Text("Merchant: ${message.parsedExpense.merchant}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            localParser.createExpenseFromParsed(message.parsedExpense, userId, userCurrency)?.let {
                                onConfirmExpense(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Expense")
                    }
                }
            }
        }

        // Suggestions (same as before)
        if (!message.isUser && message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            message.suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onSuggestionClick(suggestion.removePrefix("💡 ")) },
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Timestamp
        Text(
            text = DateTimeFormatter.ofPattern("HH:mm").format(
                java.time.Instant.ofEpochMilli(message.timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(
                top = 4.dp,
                start = if (message.isUser) 0.dp else 12.dp,
                end = if (message.isUser) 12.dp else 0.dp
            )
        )
    }
}

// Updated ChatMessage data class to support network responses
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val parsedExpense: ExpenseChatParser.ParsedExpense? = null,
    val suggestions: List<String> = emptyList(),
    val networkResponse: ChatNetworkService.ParseExpenseResponse? = null
)