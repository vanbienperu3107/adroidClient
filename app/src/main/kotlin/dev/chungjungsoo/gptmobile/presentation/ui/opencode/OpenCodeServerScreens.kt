package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeConnectionState
import dev.chungjungsoo.gptmobile.domain.opencode.OpenCodeServerProfile
import dev.chungjungsoo.gptmobile.util.collectManagedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCodeServerListScreen(
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
    viewModel: OpenCodeServerViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectManagedState()
    var pendingDelete by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    var actionsFor by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    var projectProfile by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
    val projects by viewModel.projects.collectManagedState()
    LaunchedEffect(Unit) { viewModel.refresh().join() }
    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete server?") },
            text = { Text("Remove this profile and its saved credentials?") },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    viewModel.delete(profile)
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
    projectProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { projectProfile = null },
            title = { Text("Default project") },
            text = {
                Column {
                    if (projects.isEmpty()) Text("No projects are available.")
                    projects.forEach { project ->
                        TextButton(onClick = {
                            viewModel.setDefaultProject(profile, project.directory)
                            projectProfile = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(project.directory)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { projectProfile = null }) { Text("Cancel") } }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text("OpenCode servers") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Button(onClick = { onEdit(null) }) { Text("Add server") }
            profiles.forEach { profile ->
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName)
                        Text(profile.baseUrl)
                        profile.defaultDirectory?.let { Text("Default project: $it") }
                    }
                    Column {
                        IconButton(onClick = { actionsFor = profile }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Server actions")
                        }
                        ServerActionMenu(
                            expanded = actionsFor?.serverId == profile.serverId,
                            onDismiss = { actionsFor = null },
                            onDefaultProject = {
                                actionsFor = null
                                projectProfile = profile
                                viewModel.loadProjects(profile)
                            },
                            onEdit = {
                                actionsFor = null
                                onEdit(profile.serverId)
                            },
                            onDelete = {
                                actionsFor = null
                                pendingDelete = profile
                            }
                        )
                    }
                }
            }
            Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) { Text("Back") }
        }
    }
}

@Composable
fun ServerActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDefaultProject: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Default project") }, onClick = onDefaultProject)
        DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)
        DropdownMenuItem(text = { Text("Delete") }, onClick = onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCodeServerEditScreen(
    onBack: () -> Unit,
    viewModel: OpenCodeServerViewModel = hiltViewModel()
) {
    val profile by viewModel.editingProfile.collectManagedState()
    var name by remember(profile?.serverId) { mutableStateOf(profile?.displayName.orEmpty()) }
    var url by remember(profile?.serverId) { mutableStateOf(profile?.baseUrl.orEmpty()) }
    var username by remember(profile?.serverId) { mutableStateOf("opencode") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectManagedState()
    val formError by viewModel.formError.collectManagedState()
    Scaffold(topBar = { TopAppBar(title = { Text(if (profile == null) "Add OpenCode server" else "Edit OpenCode server") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it }, label = { Text("HTTPS URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(username, { username = it }, label = { Text("Basic username") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text(if (profile == null) "Password" else "New password (optional)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Text(connectionText(state), modifier = Modifier.padding(vertical = 12.dp))
            formError?.let { Text(it) }
            Row {
                Button(onClick = { viewModel.test(name, url, username, password) }) { Text("Test") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.save(name, url, username, password, onBack) }) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onBack) { Text("Cancel") }
            }
        }
    }
}

private fun connectionText(state: OpenCodeConnectionState): String = when (state) {
    is OpenCodeConnectionState.Connected -> "Connected: ${state.version}"
    OpenCodeConnectionState.NotChecked -> "Not checked"
    else -> state::class.simpleName.orEmpty()
}
