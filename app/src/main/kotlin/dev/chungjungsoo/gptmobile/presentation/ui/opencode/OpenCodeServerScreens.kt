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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    onBrowse: (String) -> Unit = {},
    viewModel: OpenCodeServerViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectManagedState()
    var pendingDelete by remember { mutableStateOf<OpenCodeServerProfile?>(null) }
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
    Scaffold(topBar = { TopAppBar(title = { Text("OpenCode servers") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Button(onClick = { onEdit(null) }) { Text("Add server") }
            profiles.forEach { profile ->
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(profile.displayName)
                        Text(profile.baseUrl)
                        Button(onClick = { onBrowse(profile.serverId) }) { Text("Projects") }
                    }
                    Row {
                        Button(onClick = { onEdit(profile.serverId) }) { Text("Edit") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { pendingDelete = profile }) { Text("Delete") }
                    }
                }
            }
            Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) { Text("Back") }
        }
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
