package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeInteraction
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryMessage
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeModelOption
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
    var interaction by remember { mutableStateOf<CachedOpenCodeInteraction?>(null) }
    var answer by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var showVariantPicker by remember { mutableStateOf(false) }
    var variantPickerModel by remember { mutableStateOf<OpenCodeModelOption?>(null) }
    val back = { if (!viewModel.up()) onBack() }

    BackHandler(onBack = back)

    if (showModelPicker) {
        OpenCodeModelPickerSheet(
            models = models,
            selected = selectedModel,
            onRefresh = viewModel::loadModels,
            onDismiss = { showModelPicker = false },
            onSelect = { model ->
                variantPickerModel = model
                showModelPicker = false
                if (model.variants.isEmpty()) {
                    viewModel.selectModel(model, null)
                } else {
                    showVariantPicker = true
                }
            }
        )
    }
    if (showVariantPicker) {
        OpenCodeVariantPickerSheet(
            model = variantPickerModel ?: selectedModel,
            selectedVariant = selectedVariant,
            onDismiss = { showVariantPicker = false },
            onSelect = { variant ->
                val model = variantPickerModel ?: selectedModel ?: return@OpenCodeVariantPickerSheet
                viewModel.selectModel(model, variant)
                showVariantPicker = false
            }
        )
    }
    interaction?.let { request ->
        AlertDialog(
            onDismissRequest = { interaction = null },
            title = { Text(request.title.take(120)) },
            text = {
                Column {
                    Text(request.details.take(8_000))
                    if (request.kind == "question") OutlinedTextField(answer, { answer = it }, label = { Text("Answer") })
                    if (request.kind == "permission" && request.allowsAlways) Text("Always allow - scope and duration are determined by the OpenCode server.")
                }
            },
            confirmButton = {
                Row {
                    if (request.kind == "permission") {
                        TextButton(enabled = !loading, onClick = {
                            viewModel.respond(request, "once")
                            interaction = null
                        }) { Text("Allow once") }
                        if (request.allowsAlways) {
                            TextButton(enabled = !loading, onClick = {
                                viewModel.respond(request, "always")
                                interaction = null
                            }) { Text("Always") }
                        }
                    } else {
                        TextButton(enabled = answer.isNotBlank() && !loading, onClick = {
                            viewModel.respond(request, "reply", answer)
                            interaction = null
                        }) { Text("Reply") }
                    }
                    TextButton(enabled = !loading, onClick = {
                        viewModel.respond(request, "reject")
                        interaction = null
                    }) { Text("Reject") }
                }
            },
            dismissButton = { TextButton(onClick = { interaction = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(data.sessions.firstOrNull { it.sessionId == viewModel.sessionId }?.title ?: "OpenCode chat")
                        Text(viewModel.directory ?: "OpenCode", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { TextButton(onClick = back) { Text("Back") } },
                actions = { TextButton(onClick = { viewModel.refresh() }, enabled = !loading) { Text("Refresh") } }
            )
        },
        bottomBar = {
            if (viewModel.sessionId != null) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Ask OpenCode…") }, enabled = !loading && !data.stale)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        AssistChip(onClick = {
                            viewModel.loadModels()
                            showModelPicker = true
                        }, label = { Text(selectedModel?.name ?: "Choose model") })
                        AssistChip(onClick = { showVariantPicker = true }, label = { Text(selectedVariant ?: "Server default") })
                        Spacer(Modifier.weight(1f))
                        TextButton(enabled = prompt.isNotBlank() && !loading && !data.stale, onClick = {
                            viewModel.prompt(prompt)
                            prompt = ""
                        }) { Text("Send") }
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.sessionId == null) {
            OpenCodeChatList(
                modifier = Modifier.padding(padding),
                data = data,
                loading = loading,
                onOpen = viewModel::session
            )
        } else {
            OpenCodeConversation(
                modifier = Modifier.padding(padding),
                data = data,
                loading = loading,
                onInteraction = {
                    answer = ""
                    interaction = it
                }
            )
        }
    }
}

@Composable
private fun OpenCodeChatList(modifier: Modifier, data: dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData, loading: Boolean, onOpen: (String) -> Unit) {
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (data.locked) item { Text("Authentication required. Return to server settings to sign in again.") }
        data.error?.let { item { Text(it) } }
        if (data.stale) item { CacheBanner() }
        if (!loading && !data.locked && data.sessions.isEmpty()) item { Text("No OpenCode chats in this project.") }
        items(data.sessions, key = { it.sessionId }) { chat ->
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(chat.sessionId) },
                headlineContent = { Text(chat.title) },
                supportingContent = { Text("OpenCode chat · ${chat.status}") },
                trailingContent = { Text("›") }
            )
        }
    }
}

@Composable
private fun OpenCodeConversation(modifier: Modifier, data: dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData, loading: Boolean, onInteraction: (CachedOpenCodeInteraction) -> Unit) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (data.locked) item { Text("Authentication required. Return to server settings to sign in again.") }
        data.error?.let { item { Text(it) } }
        if (data.stale) item { CacheBanner() }
        items(data.messages, key = { it.id }) { message -> OpenCodeMessage(message) }
        items(data.prompts, key = { it.clientMessageId }) { pending -> Text("Prompt ${pending.state.lowercase()}: ${pending.content.take(240)}") }
        items(data.interactions, key = { it.requestId }) { request ->
            Card(onClick = { onInteraction(request) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(if (request.kind == "permission") "Permission: ${request.title}" else "Question: ${request.title}", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun OpenCodeMessage(message: OpenCodeHistoryMessage) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(if (isUser) "YOU" else "OPENCODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.widthIn(max = 330.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                contentColor = if (isUser) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.parts.forEach { part -> RichChatContent(part.text ?: "[${part.type}]") }
            }
        }
    }
}

@Composable
private fun CacheBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text("Cached · not synchronized", fontWeight = FontWeight.SemiBold)
            Text("Pull to refresh before acting.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCodeModelPickerSheet(models: List<OpenCodeModelOption>, selected: OpenCodeModelOption?, onRefresh: () -> Unit, onDismiss: () -> Unit, onSelect: (OpenCodeModelOption) -> Unit) {
    var query by remember { mutableStateOf("") }
    val visibleModels = remember(models, query) { models.filter { query.isBlank() || listOf(it.name, it.providerId, it.id).any { value -> value.contains(query, true) } } }
    LaunchedEffect(Unit) { if (models.isEmpty()) onRefresh() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Choose model", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
            OutlinedTextField(query, { query = it }, label = { Text("Search models") }, singleLine = true, modifier = Modifier.fillMaxWidth().semantics { testTag = "opencode-model-search" })
            if (visibleModels.isEmpty()) {
                Text(if (models.isEmpty()) "No server models loaded." else "No models match your search.", Modifier.padding(vertical = 24.dp))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(visibleModels, key = { "${it.providerId}:${it.id}" }) { model ->
                        ListItem(
                            modifier = Modifier.fillMaxWidth().semantics { testTag = "opencode-model-${model.providerId}-${model.id}" }.clickable { onSelect(model) },
                            headlineContent = { Text(if (model == selected) "✓ ${model.name}" else model.name) },
                            supportingContent = { Text("${model.providerId}/${model.id}" + if (model.variants.isEmpty()) "" else " · ${model.variants.size} levels") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenCodeVariantPickerSheet(model: OpenCodeModelOption?, selectedVariant: String?, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Reasoning level", style = MaterialTheme.typography.headlineSmall)
            Text(model?.name ?: "Choose a model first", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedVariant == null) "✓ Server default" else "Server default") }
            model?.variants.orEmpty().forEach { variant -> TextButton(onClick = { onSelect(variant) }, modifier = Modifier.fillMaxWidth()) { Text(if (variant == selectedVariant) "✓ $variant" else variant) } }
        }
    }
}
