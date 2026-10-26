package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.AgentType
import com.example.ui.components.AttachBottomSheet
import com.example.ui.components.BottomInputBar
import com.example.ui.components.GeminiVideoSheet
import com.example.ui.components.HistoryDrawerContent
import com.example.ui.components.InfinityLogo
import com.example.ui.components.MessageBubble
import com.example.ui.components.PipelineCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.InfinityCyan
import com.example.ui.theme.InfinityPink
import com.example.ui.theme.InfinityViolet
import com.example.ui.viewmodel.SandboxViewModel
import com.example.util.ZipExporter
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: SandboxViewModel,
    onOpenProjectViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val agentStates by viewModel.agentStates.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isListeningVoice by viewModel.isListeningVoice.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val attachSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAttachSheet by remember { mutableStateOf(false) }

    val videoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showVideoSheet by remember { mutableStateOf(false) }

    var topMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Camera photo capture URI setup
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.setSelectedImageUri(tempPhotoUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg").apply {
                parentFile?.mkdirs()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery photo picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedImageUri(uri)
        }
    }

    // Audio record permission for voice typing
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceTyping()
        } else {
            Toast.makeText(context, "Microphone permission needed for voice typing", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to bottom when messages or agent states change
    val shouldScrollToBottom by remember {
        derivedStateOf {
            messages.size
        }
    }
    LaunchedEffect(shouldScrollToBottom, agentStates.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    // Show floating button when user scrolls up
    val showScrollToBottomButton by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 3 && lastVisible < totalItems - 2
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawerContent(
                sessions = sessions,
                activeSessionId = currentSessionId,
                onSelectSession = { id ->
                    viewModel.switchSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                onDeleteSession = { id -> viewModel.deleteSession(id) }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("app_title_row")
                        ) {
                            InfinityLogo(sizeDp = 28.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Infinity Sandbox",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // '+' New Chat button
                        IconButton(
                            onClick = { viewModel.createNewChat() },
                            modifier = Modifier.testTag("top_new_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = InfinityCyan
                            )
                        }

                        // Three-dot menu
                        Box {
                            IconButton(
                                onClick = { topMenuExpanded = true },
                                modifier = Modifier.testTag("top_menu_more_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = topMenuExpanded,
                                onDismissRequest = { topMenuExpanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open Project (${projectFiles.size} files)", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = InfinityCyan)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        onOpenProjectViewer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Project ZIP", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = InfinityViolet)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        scope.launch {
                                            val file = viewModel.exportProjectZip()
                                            if (file != null) {
                                                Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "No project files to export", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Animate UI to Video", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = InfinityPink)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        showVideoSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Toggle Theme", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = Color(0xFFFFB300))
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.toggleTheme()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Chat", color = Color(0xFFFF5252)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFF5252))
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        viewModel.clearChat()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("About Infinity Sandbox", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFA59EC4))
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        showAboutDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkSurface
                    )
                )
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat message feed
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
                    ) {
                        // Empty chat state welcome
                        if (messages.isEmpty() && agentStates.isEmpty()) {
                            item {
                                EmptyWelcomeView(
                                    onSelectPrompt = { prompt ->
                                        viewModel.onInputTextChange(prompt)
                                    }
                                )
                            }
                        }

                        // Message bubbles
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onOpenProject = onOpenProjectViewer,
                                onDownloadZip = {
                                    scope.launch {
                                        val file = viewModel.exportProjectZip()
                                        if (file != null) {
                                            Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onShareProject = {
                                    scope.launch {
                                        val file = viewModel.exportProjectZip()
                                        if (file != null) {
                                            shareFile(context, file)
                                        }
                                    }
                                }
                            )
                        }

                        // Live Multi-agent Pipeline Card
                        if (agentStates.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                    PipelineCard(agentStates = agentStates)
                                }
                            }
                        }

                        // Error Banner if present
                        if (errorMessage != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF38141F))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Pinned bottom input bar
                    BottomInputBar(
                        inputText = inputText,
                        onInputChange = { viewModel.onInputTextChange(it) },
                        selectedImageUri = selectedImageUri,
                        onRemoveImage = { viewModel.setSelectedImageUri(null) },
                        currentMode = currentMode,
                        onModeSelect = { viewModel.setPipelineMode(it) },
                        isRunning = isRunning,
                        isListeningVoice = isListeningVoice,
                        onAttachClick = { showAttachSheet = true },
                        onVoiceClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.toggleVoiceTyping()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSendClick = { viewModel.sendTask() },
                        onStopClick = { viewModel.cancelCurrentExecution() }
                    )
                }

                // Floating "Scroll to bottom" button
                AnimatedVisibility(
                    visible = showScrollToBottomButton,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 90.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                val target = (messages.size + if (agentStates.isNotEmpty()) 1 else 0)
                                if (target > 0) listState.animateScrollToItem(target)
                            }
                        },
                        containerColor = DarkSurfaceVariant,
                        contentColor = InfinityCyan,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Attach Bottom Sheet
    if (showAttachSheet) {
        AttachBottomSheet(
            sheetState = attachSheetState,
            onDismiss = { showAttachSheet = false },
            onPickImage = {
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onTakePhoto = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val photoFile = File(context.cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg").apply {
                        parentFile?.mkdirs()
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                    tempPhotoUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onSelectTemplate = { template ->
                viewModel.onInputTextChange(template)
            },
            onOpenVideoGenerator = {
                showVideoSheet = true
            }
        )
    }

    // Video Generator Bottom Sheet
    if (showVideoSheet) {
        GeminiVideoSheet(
            sheetState = videoSheetState,
            selectedImageUri = selectedImageUri,
            onPickImage = {
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onDismiss = { showVideoSheet = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfinityLogo(sizeDp = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About Infinity Sandbox", color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Infinity Sandbox is a multi-agent AI native Android coding sandbox that synthesizes production-ready Android apps in Kotlin and Java without external servers.",
                        color = DarkTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Specialist Agents:\n• Orchestrator & Merger: Nemotron Ultra\n• Researcher: GLM-5.3\n• Coder: DeepSeek V4.1 Flash\n• UI Analyst: MiniMax M3.1 Flash\n• Debugger: Nemotron Bolt\n• Helper: GLM-5.3 Flash",
                        color = Color(0xFFA59EC4),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = InfinityCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun EmptyWelcomeView(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InfinityLogo(sizeDp = 72.dp, animate = true)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Welcome to Infinity Sandbox.",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Describe what you want to build. Specialist AIs work in parallel to architect, research, code, debug, and merge your Android app.",
            color = Color(0xFFA59EC4),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        val promptSuggestions = listOf(
            "Create a tab bar screen with animated icons & badge counters",
            "Build an audio recorder with live visualizer waveform",
            "Design an e-commerce checkout flow with cart & payment",
            "Generate an offline note-taking app with Room & Search"
        )

        promptSuggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectPrompt(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "“$suggestion”",
                    color = Color(0xFFE2DEF5),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Project Archive"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
