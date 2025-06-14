package com.example.bachelor_frontend.ui.compose

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.classes.FamilyDto
import com.example.bachelor_frontend.ui.function.ReceiptScanner
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onCaptureSuccess: (ExpenseDto, BudgetType) -> Unit,
    onCancel: () -> Unit,
    familyViewModel: FamilyViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val userFamily by familyViewModel.family.collectAsState()
    var selectedBudgetType by remember { mutableStateOf(BudgetType.PERSONAL) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCaptureState by remember { mutableStateOf<ImageCapture?>(null) }
    var processingImage by remember { mutableStateOf(false) }
    var showOCRResult by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<ReceiptScanner.ReceiptData?>(null) }
    var detectedExpense by remember { mutableStateOf<ExpenseDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showOCRResult && ocrResult != null) {
                ReceiptResultsScreen(
                    receiptData = ocrResult!!,
                    detectedExpense = detectedExpense,
                    onConfirm = { confirmedExpense, budgetType ->
                        onCaptureSuccess(confirmedExpense, budgetType)
                    },
                    onRetry = {
                        showOCRResult = false
                        processingImage = false
                    },
                    onCancel = onCancel,
                    userFamily = userFamily,
                    selectedBudgetType = selectedBudgetType,
                    onBudgetTypeChange = { selectedBudgetType = it }
                )
            } else if (hasCameraPermission) {
                CameraView(
                    executor = executor,
                    onImageCaptureCreated = { capture ->
                        imageCaptureState = capture
                        Log.d("ReceiptScannerScreen", "ImageCapture created and shared")
                    },
                    onImageCaptured = { uri ->
                        processingImage = true
                        coroutineScope.launch {
                            val receiptScanner = ReceiptScanner(context)
                            val result = receiptScanner.processReceiptImage(uri)
                            ocrResult = result

                            if (result.success) {
                                val userId = "currentUserId" // Replace with actual user ID from your auth system
                                detectedExpense = receiptScanner.createExpenseFromReceipt(result, userId).copy(
                                    receiptImageUrl = uri.toString()
                                )
                            }

                            showOCRResult = true
                            processingImage = false
                        }
                    },
                    onError = { error ->
                        Log.e("ReceiptScannerScreen", "Error taking photo: $error")
                        processingImage = false
                    }
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Position receipt in frame and take photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Log.d("ReceiptScannerScreen", "Camera button clicked")
                                if (!processingImage) {
                                    if (imageCaptureState == null) {
                                        Log.e("ReceiptScannerScreen", "ImageCapture is null!")
                                    } else {
                                        Log.d("ReceiptScannerScreen", "Taking photo...")
                                        processingImage = true
                                        takePhoto(
                                            imageCapture = imageCaptureState,
                                            context = context,
                                            executor = executor,
                                            onImageCaptured = { uri ->
                                                processingImage = true
                                                coroutineScope.launch {
                                                    val receiptScanner = ReceiptScanner(context)
                                                    val result = receiptScanner.processReceiptImage(uri)
                                                    ocrResult = result

                                                    if (result.success) {
                                                        val userId = "currentUserId"
                                                        detectedExpense = receiptScanner.createExpenseFromReceipt(result, userId).copy(
                                                            receiptImageUrl = uri.toString()
                                                        )
                                                    }

                                                    showOCRResult = true
                                                    processingImage = false
                                                }
                                            },
                                            onError = { error ->
                                                Log.e("ReceiptScannerScreen", "Error taking photo: $error")
                                                processingImage = false
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Take Photo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is required to scan receipts",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }

            if (processingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing receipt...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraView(
    executor: Executor,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onImageCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    onImageCaptureCreated(imageCapture)

                    Log.d("CameraView", "Setting up camera with ImageCapture instance")

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        Log.d("CameraView", "Camera bound to lifecycle successfully")
                    } catch (e: Exception) {
                        Log.e("CameraView", "Failed to bind camera use cases: ${e.message}")
                        onError("Failed to bind camera use cases: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("CameraView", "Failed to get camera provider: ${e.message}")
                    onError("Failed to get camera provider: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraView", "Disposing CameraView")
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) {
        onError("Camera not initialized")
        Log.e("ReceiptScannerScreen", "Cannot take photo - imageCapture is null")
        return
    }

    Log.d("ReceiptScannerScreen", "Starting photo capture")

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()))
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FinanceTracker")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    Log.d("ReceiptScannerScreen", "Taking picture with options created")

    try {
        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    Log.d("ReceiptScannerScreen", "Image saved: $savedUri")
                    if (savedUri != null) {
                        onImageCaptured(savedUri)
                    } else {
                        onError("Failed to save image: Uri is null")
                        Log.e("ReceiptScannerScreen", "Saved image URI is null")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    val errorMessage = "Failed to capture image: ${exception.message}"
                    onError(errorMessage)
                    Log.e("ReceiptScannerScreen", errorMessage, exception)
                }
            }
        )
        Log.d("ReceiptScannerScreen", "takePicture() called successfully")
    } catch (e: Exception) {
        val errorMessage = "Exception when taking picture: ${e.message}"
        onError(errorMessage)
        Log.e("ReceiptScannerScreen", errorMessage, e)
    }
}

@Composable
fun ReceiptResultsScreen(
    receiptData: ReceiptScanner.ReceiptData,
    detectedExpense: ExpenseDto?,
    onConfirm: (ExpenseDto, BudgetType) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    userFamily: FamilyDto?, // Add family data
    selectedBudgetType: BudgetType, // Add budget type
    onBudgetTypeChange: (BudgetType) -> Unit // Add budget type change handler
) {
    var amount by remember { mutableStateOf(detectedExpense?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(detectedExpense?.category ?: "Other") }
    var description by remember { mutableStateOf(detectedExpense?.description ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (receiptData.success) "Receipt Detected" else "Detection Failed",
            style = MaterialTheme.typography.headlineSmall
        )

        if (!receiptData.success) {
            Text(
                text = receiptData.error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )

            if (receiptData.rawText.isNotEmpty()) {
                Text(
                    text = "Detected Text:",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = receiptData.rawText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Try Again")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Please review the detected expense details:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Category: $category",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (userFamily != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Add to:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedBudgetType == BudgetType.PERSONAL,
                                onClick = { onBudgetTypeChange(BudgetType.PERSONAL) },
                                label = { Text("Personal") },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = selectedBudgetType == BudgetType.FAMILY,
                                onClick = { onBudgetTypeChange(BudgetType.FAMILY) },
                                label = { Text("Family") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (receiptData.merchantName.isNotEmpty()) {
                        Text(
                            text = "Merchant: ${receiptData.merchantName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text(
                text = "Raw Text Detected:",
                style = MaterialTheme.typography.titleSmall
            )

            Card(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = receiptData.rawText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Try Again")
                }

                Button(
                    onClick = {
                        detectedExpense?.let { expense ->
                            val amountValue = amount.toDoubleOrNull() ?: expense.amount
                            onConfirm(expense.copy(
                                amount = amountValue,
                                category = category,
                                description = description,
                                budgetType = selectedBudgetType,
                                familyId = if (selectedBudgetType == BudgetType.FAMILY) userFamily?.id else null
                            ), selectedBudgetType)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = detectedExpense != null
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}