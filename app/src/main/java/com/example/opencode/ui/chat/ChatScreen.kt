package com.example.opencode.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.opencode.data.model.Message
import com.example.opencode.data.model.Part
import com.example.opencode.data.model.Todo

data class ChatTurn(
    val userMessage: Message? = null,
    val assistantParts: List<Part> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val turns = remember(uiState.messages) { buildTurns(uiState.messages) }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showTodoDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 5 && info.totalItemsCount > 0
        }
    }

    val isAtBottom = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) true
            else info.visibleItemsInfo.firstOrNull()?.index ?: 0 <= 1
        }
    }

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty() && (isAtBottom.value || uiState.isSending)) {
            kotlinx.coroutines.delay(100)
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.hasMore && !uiState.isLoadingMore) viewModel.loadMore()
    }

    if (showTodoDialog) {
        TodoDialog(todos = uiState.todos, onDismiss = { showTodoDialog = false }, onRefresh = { viewModel.loadTodos() })
    }
    if (showInfoDialog) {
        InfoDialog(session = uiState.sessionInfo, sessionId = sessionId, onDismiss = { showInfoDialog = false })
    }
    if (showShareDialog) {
        ShareDialog(session = uiState.sessionInfo, onDismiss = { showShareDialog = false }, onShare = { viewModel.shareSession() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text((uiState.sessionTitle ?: "Session").ifBlank { "Session" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text(sessionId.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Todo List") }, onClick = { showMenu = false; showTodoDialog = true; viewModel.loadTodos() }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                        DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; showShareDialog = true; viewModel.loadSessionDetails() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                        DropdownMenuItem(text = { Text("Session Info") }, onClick = { showMenu = false; showInfoDialog = true; viewModel.loadSessionDetails() }, leadingIcon = { Icon(Icons.Default.Info, null) })
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (turns.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Send a message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = true,
                    ) {
                        items(turns.reversed(), key = { t ->
                            val id = t.userMessage?.info?.id ?: t.assistantParts.firstOrNull()?.id
                            if (!id.isNullOrBlank()) id else "local_${t.userMessage?.parts?.firstOrNull()?.text?.hashCode() ?: turns.indexOf(t)}"
                        }) { turn -> TurnView(turn) }
                        if (uiState.isLoadingMore) {
                            item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } }
                        }
                    }
                }
            }

            ChatInput(
                value = uiState.inputText,
                onValueChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                isSending = uiState.isSending,
            )
        }
    }
}

// region Dialogs

@Composable
private fun TodoDialog(todos: List<Todo>, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Todo List") },
        text = {
            if (todos.isEmpty()) {
                Text("No todos in this session", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todos.forEach { todo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = todo.status == "completed", onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(todo.content.ifBlank { "(empty)" }, style = MaterialTheme.typography.bodyMedium)
                                Text(todo.priority, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Row { TextButton(onClick = onRefresh) { Text("Refresh") }; TextButton(onClick = onDismiss) { Text("Close") } } },
    )
}

@Composable
private fun InfoDialog(session: com.example.opencode.data.model.Session?, sessionId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("ID", sessionId)
                if (session != null) {
                    InfoRow("Title", session.title.ifBlank { "(untitled)" })
                    InfoRow("Version", session.version)
                    InfoRow("Directory", session.directory)
                    InfoRow("Created", formatEpoch(session.time.created))
                    InfoRow("Updated", formatEpoch(session.time.updated))
                    if (session.share?.url != null) InfoRow("Share URL", session.share!!.url)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ShareDialog(session: com.example.opencode.data.model.Session?, onDismiss: () -> Unit, onShare: () -> Unit) {
    val hasShare = session?.share?.url?.isNotBlank() == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Session") },
        text = {
            if (hasShare) {
                Column { Text("Share URL:", style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(4.dp)); Text(session!!.share!!.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            } else {
                Text("Create a shareable link for this session?", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            if (!hasShare) TextButton(onClick = { onShare(); onDismiss() }) { Text("Share") }
            else TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = if (!hasShare) ({ TextButton(onClick = onDismiss) { Text("Cancel") } }) else null,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatEpoch(millis: Long): String {
    if (millis == 0L) return "N/A"
    return try {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (_: Exception) { "$millis" }
}

// endregion

// region Turns

private fun buildTurns(messages: List<Message>): List<ChatTurn> {
    val turns = mutableListOf<ChatTurn>()
    var cur = ChatTurn()
    for (msg in messages) {
        if (msg.info == null) continue
        when (msg.info.role) {
            "user" -> { if (cur.userMessage != null || cur.assistantParts.isNotEmpty()) turns.add(cur); cur = ChatTurn(userMessage = msg) }
            "assistant" -> cur = cur.copy(assistantParts = cur.assistantParts + msg.parts)
        }
    }
    if (cur.userMessage != null || cur.assistantParts.isNotEmpty()) turns.add(cur)
    return turns
}

@Composable
private fun TurnView(turn: ChatTurn) {
    Column(Modifier.fillMaxWidth()) {
        if (turn.userMessage != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Surface(
                    shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f, fill = false).widthIn(max = 280.dp),
                ) {
                    Text(
                        text = turn.userMessage.parts.filter { it.type == "text" }.joinToString("\n") { it.text ?: "" }.ifBlank { "(empty)" },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.requiredSize(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (turn.assistantParts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.requiredSize(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.SmartToy, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    for (part in turn.assistantParts) {
                        when (part.type) {
                            "text" -> if (!part.text.isNullOrBlank()) Surface(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 1.dp, modifier = Modifier.padding(bottom = 4.dp)) {
                                MarkdownText(part.text!!, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface)
                            }
                            "reasoning" -> if (!part.text.isNullOrBlank()) CollapsibleCard(Icons.Default.Lightbulb, "Thinking", MaterialTheme.colorScheme.tertiary, Modifier.padding(bottom = 4.dp)) {
                                MarkdownText(part.text!!, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            "tool" -> ToolCallView(part, Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(15.dp), tint = tint); Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = tint, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column { HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant); content() }
            }
        }
    }
}

@Composable
private fun ToolCallView(part: Part, modifier: Modifier = Modifier) {
    val status = part.state?.status ?: "pending"
    val name = part.tool ?: "tool"
    val title = part.state?.title
    val output = part.state?.output ?: part.state?.error
    val tint = when (status) { "completed" -> MaterialTheme.colorScheme.primary; "running" -> MaterialTheme.colorScheme.tertiary; "error" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.outline }
    CollapsibleCard(Icons.Default.Build, name + if (!title.isNullOrBlank()) " — $title" else "", tint, modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            when (status) { "completed" -> Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = tint); "running" -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = tint); "error" -> Icon(Icons.Default.Error, null, Modifier.size(14.dp), tint = tint); else -> Icon(Icons.Default.HourglassEmpty, null, Modifier.size(14.dp), tint = tint) }
            Spacer(Modifier.width(6.dp)); Text(status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!output.isNullOrBlank()) Text(output.take(3000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

// endregion

// region Input

@Composable
private fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, isSending: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value, onValueChange,
                    placeholder = { Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    modifier = Modifier.weight(1f), maxLines = 6,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!isSending && value.isNotBlank()) onSend() }),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !isSending,
                    modifier = Modifier.size(48.dp),
                ) {
                    if (isSending) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.AutoMirrored.Filled.Send, "Send", Modifier.size(22.dp))
                }
            }
        }
    }
}

// endregion
