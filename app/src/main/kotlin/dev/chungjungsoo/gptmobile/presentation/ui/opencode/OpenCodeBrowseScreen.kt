package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.util.collectManagedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCodeBrowseScreen(onBack: () -> Unit, viewModel: OpenCodeBrowseViewModel = hiltViewModel()) {
    val data by viewModel.data.collectManagedState()
    val loading by viewModel.loading.collectManagedState()
    var mutation by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var title by remember { mutableStateOf("") }
    var pendingInfo by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf("") }
    val back = { if (!viewModel.up()) onBack() }
    BackHandler(onBack = back)
    mutation?.let { (id, deleting) ->
        AlertDialog(
            onDismissRequest = { mutation = null },
            title = { Text(if (deleting) "Delete server session?" else "Rename session") },
            text = { if (deleting) Text("This deletes the session on the server and affects all devices. It is not just clearing Android cache.") else OutlinedTextField(title, { title = it }, label = { Text("Title") }) },
            confirmButton = {
                TextButton(onClick = {
                    mutation = null
                    viewModel.mutate(id, if (deleting) null else title)
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { mutation = null }) { Text("Cancel") } }
        )
    }
    if (pendingInfo) {
        AlertDialog(
            onDismissRequest = { pendingInfo = false },
            title = { Text("Pending requests") },
            text = { Text("Reply on desktop. Android replies are planned in Feature 05.") },
            confirmButton = { TextButton(onClick = { pendingInfo = false }) { Text("OK") } }
        )
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("OpenCode · ${viewModel.sessionId ?: viewModel.directory ?: "Projects"}") },
            navigationIcon = { TextButton(onClick = back) { Text("Back") } },
            actions = { TextButton(onClick = { viewModel.refresh() }, enabled = !loading) { Text("Refresh") } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (data.locked) item { Text("Authentication required. Return to server settings to sign in again.") }
            data.error?.let { item { Text(it) } }
            if (data.stale) item { Text("Cached data — not synchronized") }
            if (!loading && !data.locked && data.error == null && data.projects.isEmpty() && data.sessions.isEmpty() && data.messages.isEmpty()) item { Text("No data") }
            items(data.projects, key = { it.directory }) { project -> Button(onClick = { viewModel.project(project.directory) }) { Text(project.directory) } }
            items(data.sessions, key = { it.sessionId }) { session ->
                Column {
                    TextButton(onClick = { viewModel.session(session.sessionId) }) { Text(session.title) }
                    Text("${session.status} · updated ${session.updatedAt}")
                    Row {
                        TextButton(onClick = { pendingInfo = true }) { Text("Pending: ${data.pending?.get(session.sessionId) ?: if (data.pending == null) "?" else 0}") }
                        TextButton(enabled = !data.stale && !loading, onClick = {
                            title = session.title
                            mutation = session.sessionId to false
                        }) { Text("Rename") }
                        TextButton(enabled = !data.stale && !loading, onClick = { mutation = session.sessionId to true }) { Text("Delete") }
                    }
                }
            }
            if (data.sessions.isNotEmpty()) {
                item {
                    TextButton(enabled = !loading && !data.stale, onClick = { viewModel.moreSessions() }) { Text("Load more sessions (up to 10,000)") }
                }
            }
            items(data.messages, key = { it.id }) { message ->
                Column {
                    Text(message.role, style = MaterialTheme.typography.titleSmall)
                    message.parts.forEach { part ->
                        var expanded by remember(part.id) { mutableStateOf(false) }
                        val text = part.text ?: "[${part.type}]"
                        if (part.type == "reasoning") TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide reasoning" else "Show reasoning") }
                        if (part.type != "reasoning" || expanded) {
                            OpenCodeMarkdown(if (expanded) text.take(64000) else text.take(4000))
                            if (expanded && text.length > 64000) Text("Preview limited to 64,000 characters; full content is available on desktop.")
                            if (text.length > 4000 && !expanded) TextButton(onClick = { expanded = true }) { Text("Show more") }
                        }
                    }
                }
            }
            items(data.prompts, key = { it.clientMessageId }) { pending ->
                Text("Prompt ${pending.state.lowercase()}: ${pending.content.take(240)}")
                pending.uncertainty?.let { Text(it) }
            }
            if (viewModel.sessionId != null) {
                item {
                    OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Send prompt") }, enabled = !loading && !data.stale)
                    Row {
                        Button(enabled = prompt.isNotBlank() && !loading && !data.stale, onClick = {
                            viewModel.prompt(prompt)
                            prompt = ""
                        }) { Text("Send") }
                        TextButton(enabled = !loading && !data.stale, onClick = viewModel::abort) { Text("Stop agent") }
                    }
                }
            }
            if (data.messages.isNotEmpty()) item { TextButton(enabled = !loading, onClick = { viewModel.refresh(data.messages.first().id) }) { Text("Load older messages") } }
        }
    }
}
