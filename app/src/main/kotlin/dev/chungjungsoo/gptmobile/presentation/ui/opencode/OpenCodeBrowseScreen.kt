package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeInteraction
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryMessage
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeModelOption
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardInk
import dev.chungjungsoo.gptmobile.presentation.common.BoardLine
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSurface
import dev.chungjungsoo.gptmobile.presentation.ui.chat.RichChatContent
import dev.chungjungsoo.gptmobile.util.collectManagedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCodeBrowseScreen(onBack: () -> Unit, viewModel: OpenCodeBrowseViewModel = hiltViewModel()) {
    val data by viewModel.data.collectManagedState()
    val loading by viewModel.loading.collectManagedState()
    val models by viewModel.models.collectManagedState()
    val selectedModel by viewModel.selectedModel.collectManagedState()
    val selectedVariant by viewModel.selectedVariant.collectManagedState()
    var prompt by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    var showVariants by remember { mutableStateOf(false) }
    var modelForVariant by remember { mutableStateOf<OpenCodeModelOption?>(null) }
    var interaction by remember { mutableStateOf<CachedOpenCodeInteraction?>(null) }
    val back = { if (!viewModel.up()) onBack() }
    BackHandler(onBack = back)
    if (showModels) {
        BoardModelPicker(models, selectedModel, { viewModel.loadModels() }, { showModels = false }) { model ->
            modelForVariant = model
            showModels = false
            if (model.variants.isEmpty()) viewModel.selectModel(model, null) else showVariants = true
        }
    }
    if (showVariants) {
        BoardVariantPicker(modelForVariant ?: selectedModel, selectedVariant, { showVariants = false }) { variant ->
            val model = modelForVariant ?: selectedModel ?: return@BoardVariantPicker
            viewModel.selectModel(model, variant)
            showVariants = false
        }
    }
    interaction?.let { request ->
        AlertDialog(onDismissRequest = { interaction = null }, title = { Text(request.title) }, text = { Text(request.details) }, confirmButton = {
            TextButton(onClick = {
                viewModel.respond(request, if (request.kind == "permission") "once" else "reject")
                interaction = null
            }) { Text(if (request.kind == "permission") "Allow once" else "Reject") }
        }, dismissButton = { TextButton(onClick = { interaction = null }) { Text("Cancel") } })
    }
    if (viewModel.sessionId == null) {
        Column {
            BoardHeader("OpenCode chats", viewModel.directory ?: "Select a project", back)
            OpenCodeSessionList(data, loading, viewModel::session)
        }
    } else {
        Column {
            BoardHeader(data.sessions.firstOrNull { it.sessionId == viewModel.sessionId }?.title ?: "OpenCode chat", "OpenCode chat · ${viewModel.directory ?: "Engineering project"}", back, "⋮", { viewModel.refresh() })
            BoardOpenCodeTimeline(Modifier.weight(1f), data, loading, selectedModel, selectedVariant) { interaction = it }
            Column(Modifier.fillMaxWidth().background(Color.White).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+", modifier = Modifier.background(BoardSurface, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 8.dp), color = BoardInk)
                    OutlinedTextField(prompt, { prompt = it }, Modifier.weight(1f).height(48.dp), placeholder = { Text("Ask OpenCode…") }, singleLine = true, enabled = !loading && !data.stale)
                    TextButton(enabled = prompt.isNotBlank() && !loading && !data.stale, onClick = {
                        viewModel.prompt(prompt)
                        prompt = ""
                    }, modifier = Modifier.background(BoardInk, RoundedCornerShape(12.dp))) { Text("↑", color = Color.White) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    BoardChip(selectedModel?.name ?: "Choose model") {
                        viewModel.loadModels()
                        showModels = true
                    }
                    BoardChip(selectedVariant ?: "Server default") { showVariants = true }
                }
            }
        }
    }
}

@Composable private fun OpenCodeSessionList(data: dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData, loading: Boolean, onOpen: (String) -> Unit) = LazyColumn(Modifier.padding(18.dp)) {
    if (loading) item { BoardRow("Loading chats from OpenCode…", "Fetching workspace sessions", trailing = "") }
    if (data.locked) item { BoardRow("Authentication required", "Return to server settings to sign in again", trailing = "") }
    data.error?.let { item { BoardRow(it, trailing = "") } }
    items(data.sessions, key = { it.sessionId }) { BoardRow(it.title, "OpenCode chat · ${it.status}", "▣", { onOpen(it.sessionId) }) }
    if (!loading && !data.locked && data.sessions.isEmpty()) item { BoardRow("No OpenCode chats in this project.", trailing = "") }
}

@Composable fun BoardOpenCodeTimeline(modifier: Modifier, data: dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData, loading: Boolean, model: OpenCodeModelOption?, variant: String?, onInteraction: (CachedOpenCodeInteraction) -> Unit) = LazyColumn(modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (loading) item { BoardRow("Loading chat from OpenCode…", "Fetching full history for this chat.", trailing = "", modifier = Modifier.padding(top = 15.dp)) }
    if (data.stale) item { BoardRow("Cached · not synchronized", "Pull to refresh before acting.", trailing = "") }
    if (data.locked) item { BoardRow("Authentication required", "Return to server settings to sign in again", trailing = "") }
    data.error?.let { item { BoardRow(it, trailing = "") } }
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text("Today", color = Color(0xFF616161), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
        }
    }
    items(data.messages, key = { it.id }) { BoardMessage(it, model?.name, variant) }
    items(data.prompts, key = { it.clientMessageId }) { BoardRow("Prompt ${it.state.lowercase()}", it.content.take(160), trailing = "") }
    items(data.interactions, key = { it.requestId }) { BoardRow(if (it.kind == "permission") "Permission needed" else "Question", it.title, "⌘", { onInteraction(it) }, modifier = Modifier.testTag("opencode-interaction-${it.requestId}")) }
}

@Composable private fun BoardMessage(message: OpenCodeHistoryMessage, model: String?, variant: String?) {
    val user = message.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        if (user) {
            Text("YOU", color = Color(0xFF616161), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(BoardSurface, RoundedCornerShape(14.dp)).padding(7.dp)) { Text("O", fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("OpenCode", fontWeight = FontWeight.SemiBold)
                    Text("${model ?: "OpenCode"} · ${variant ?: "Server default"}", color = Color(0xFF616161), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
            }
        }
        Card(modifier = Modifier.padding(top = 6.dp).widthIn(max = 326.dp), shape = RoundedCornerShape(if (user) 18.dp else 16.dp), colors = CardDefaults.cardColors(containerColor = if (user) BoardInk else Color.White), border = if (user) null else androidx.compose.foundation.BorderStroke(1.dp, BoardLine)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        message.parts.forEach { RichChatContent(it.text ?: "[${it.type}]") }
                    }
                } else {
                    message.parts.forEach { RichChatContent(it.text ?: "[${it.type}]") }
                }
            }
        }
    }
}

@Composable private fun BoardChip(text: String, onClick: () -> Unit) = TextButton(onClick = onClick, modifier = Modifier.background(BoardSurface, RoundedCornerShape(9.dp)).testTag("opencode-chip-$text")) { Text("$text ▾", color = BoardInk, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardModelPicker(models: List<OpenCodeModelOption>, selected: OpenCodeModelOption?, onRefresh: () -> Unit, onDismiss: () -> Unit, onSelect: (OpenCodeModelOption) -> Unit) {
    var query by remember { mutableStateOf("") }
    val visible = models.filter { query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) }
    LaunchedEffect(Unit) { if (models.isEmpty()) onRefresh() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 18.dp)) {
            Text("Choose model", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("opencode-model-search"), placeholder = { Text("Search models") }, singleLine = true)
            LazyColumn { items(visible, key = { "${it.providerId}:${it.id}" }) { option -> BoardRow(if (option == selected) "✓ ${option.name}" else option.name, "${option.providerId}/${option.id}", onClick = { onSelect(option) }, trailing = "", modifier = Modifier.testTag("opencode-model-${option.providerId}-${option.id}")) } }
        }
    }
}

@Composable
fun OpenCodeModelPickerSheet(models: List<OpenCodeModelOption>, selected: OpenCodeModelOption?, onRefresh: () -> Unit, onDismiss: () -> Unit, onSelect: (OpenCodeModelOption) -> Unit) = BoardModelPicker(models, selected, onRefresh, onDismiss, onSelect)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardVariantPicker(model: OpenCodeModelOption?, selected: String?, onDismiss: () -> Unit, onSelect: (String?) -> Unit) = ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(18.dp)) {
        Text("Reasoning level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        BoardRow(if (selected == null) "✓ Server default" else "Server default", "Let the OpenCode server choose", onClick = { onSelect(null) }, trailing = "")
        model?.variants.orEmpty().forEach { level -> BoardRow(if (level == selected) "✓ $level" else level, "Server-provided reasoning level", onClick = { onSelect(level) }, trailing = "") }
    }
}
