package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardPrimaryButton
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection
import dev.chungjungsoo.gptmobile.util.collectManagedState

@Composable
fun OpenCodeServerListScreen(onBack: () -> Unit, onEdit: (String?) -> Unit, viewModel: OpenCodeServerViewModel = hiltViewModel()) {
    val profiles by viewModel.profiles.collectManagedState()
    val projects by viewModel.projects.collectManagedState()
    var delete by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    var projectFor by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    var actions by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh().join() }
    Column(Modifier.fillMaxSize()) {
        BoardHeader("OpenCode servers", if (profiles.isEmpty()) "No workspace connected" else "${profiles.size} workspace connected", onBack, "⋮")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            BoardPrimaryButton("+ Add server", { onEdit(null) }, Modifier.padding(top = 20.dp))
            if (profiles.isEmpty()) {
                BoardRow("Add your OpenCode server", "The server URL must use HTTPS. Basic credentials stay encrypted in Android Keystore.", "+")
            } else {
                BoardSection("Saved profiles")
                profiles.forEach { profile -> BoardRow(profile.displayName, "${profile.baseUrl}\nDefault project: ${profile.defaultDirectory ?: "Not selected"}", "▣", { actions = profile }, trailing = "⋮") }
            }
        }
    }
    actions?.let { profile ->
        AlertDialog(onDismissRequest = { actions = null }, title = { Text(profile.displayName) }, text = {
            Column {
                TextButton(onClick = {
                    actions = null
                    projectFor = profile
                    viewModel.loadProjects(profile)
                }, Modifier.fillMaxWidth()) { Text("Default project") }
                TextButton(onClick = {
                    actions = null
                    onEdit(profile.serverId)
                }, Modifier.fillMaxWidth()) { Text("Edit") }
                TextButton(onClick = {
                    actions = null
                    delete = profile
                }, Modifier.fillMaxWidth()) { Text("Delete") }
            }
        }, confirmButton = { TextButton(onClick = { actions = null }) { Text("Done") } })
    }
    projectFor?.let { profile ->
        AlertDialog(onDismissRequest = { projectFor = null }, title = { Text("Default project") }, text = {
            Column {
                if (projects.isEmpty()) Text("No projects are available.")
                projects.forEach { project ->
                    TextButton(onClick = {
                        viewModel.setDefaultProject(profile, project.directory)
                        projectFor = null
                    }, Modifier.fillMaxWidth()) { Text(project.directory) }
                }
            }
        }, confirmButton = { TextButton(onClick = { projectFor = null }) { Text("Cancel") } })
    }
    delete?.let { profile ->
        AlertDialog(onDismissRequest = { delete = null }, title = { Text("Delete ${profile.displayName}?") }, text = { Text("This removes the Android profile, cache access and encrypted credential reference. It does not delete anything on the OpenCode server.") }, confirmButton = {
            TextButton(onClick = {
                viewModel.delete(profile)
                delete = null
            }) { Text("Delete server") }
        }, dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } })
    }
}

@Composable
fun ServerActionMenu(expanded: Boolean, onDismiss: () -> Unit, onDefaultProject: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    if (expanded) {
        AlertDialog(onDismissRequest = onDismiss, title = { Text("Server actions") }, text = {
            Column {
                TextButton(onClick = onDefaultProject) { Text("Default project") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
    }
}

@Composable
fun OpenCodeServerEditScreen(onBack: () -> Unit, onSaved: () -> Unit = onBack, viewModel: OpenCodeServerViewModel = hiltViewModel()) {
    val profile by viewModel.editingProfile.collectManagedState()
    val state by viewModel.state.collectManagedState()
    val error by viewModel.formError.collectManagedState()
    var name by remember(profile?.serverId) { mutableStateOf(profile?.displayName.orEmpty()) }
    var url by remember(profile?.serverId) { mutableStateOf(profile?.baseUrl.orEmpty()) }
    var username by remember { mutableStateOf("opencode") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        BoardHeader(if (profile == null) "Add OpenCode server" else "Edit OpenCode server", "Connect safely", onBack, "⋮")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text("Basic password is masked and is never stored in preferences.", Modifier.padding(top = 20.dp))
            Field("Server name", name) { name = it }
            Field("HTTPS URL", url) { url = it }
            Field("Basic username", username) { username = it }
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text(if (profile == null) "Password" else "New password (optional)") }, visualTransformation = PasswordVisualTransformation())
            Row(Modifier.padding(top = 16.dp)) {
                TextButton(onClick = { viewModel.test(name, url, username, password) }) { Text("Test connection") }
                Text(connectionText(state), Modifier.padding(start = 12.dp, top = 12.dp))
            }
            error?.let { Text(it) }
            BoardPrimaryButton("Save server", { viewModel.save(name, url, username, password, onSaved) }, Modifier.padding(top = 22.dp))
        }
    }
}

@Composable private fun Field(label: String, value: String, update: (String) -> Unit) {
    OutlinedTextField(value, update, Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text(label) }, singleLine = true)
}
private fun connectionText(state: OpenCodeConnectionState) = when (state) {
    is OpenCodeConnectionState.Connected -> "Connected: ${state.version}"
    OpenCodeConnectionState.NotChecked -> "Not checked"
    else -> state::class.simpleName.orEmpty()
}
