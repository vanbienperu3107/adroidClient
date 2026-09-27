package dev.chungjungsoo.gptmobile.presentation.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.chungjungsoo.gptmobile.data.cliproxy.CliproxyDirectConfig
import dev.chungjungsoo.gptmobile.data.cliproxy.CliproxyDirectRepository
import dev.chungjungsoo.gptmobile.data.cliproxy.CliproxyResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CliproxySettingsViewModel @Inject constructor(private val repository: CliproxyDirectRepository) : ViewModel() {
    private val _config = MutableStateFlow(CliproxyDirectConfig())
    val config = _config.asStateFlow()
    private val _models = MutableStateFlow(emptyList<String>())
    val models = _models.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    init {
        viewModelScope.launch { _config.value = repository.config() }
    }

    fun save(url: String, key: String, selectedModel: String?) = viewModelScope.launch {
        try {
            _config.value = repository.save(url, key.ifBlank { null }, true, selectedModel)
            _status.value = "Saved"
        } catch (_: Exception) {
            _status.value = "Check the HTTPS URL and API key"
        }
    }

    fun loadModels() = viewModelScope.launch {
        when (val result = repository.models()) {
            is CliproxyResult.Models -> {
                _models.value = result.values
                _status.value = if (result.values.isEmpty()) "No models are available" else null
            }
            CliproxyResult.Unauthorized -> _status.value = "Could not authenticate with Cliproxy. Check the API key."
            CliproxyResult.InvalidConfiguration -> _status.value = "Save a valid HTTPS URL and API key first."
            CliproxyResult.NetworkFailure -> _status.value = "Could not load Cliproxy models. Try again."
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CliproxySettingsScreen(onBack: () -> Unit, viewModel: CliproxySettingsViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsState()
    val models by viewModel.models.collectAsState()
    val status by viewModel.status.collectAsState()
    var url by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var key by remember { mutableStateOf("") }
    var model by remember(config.selectedModel) { mutableStateOf(config.selectedModel.orEmpty()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Cliproxy") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("Direct chat without OpenCode")
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text("HTTPS server URL") })
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("API key") }, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { viewModel.loadModels() }, modifier = Modifier.padding(top = 12.dp)) { Text("Load available models") }
            models.forEach { item ->
                TextButton(onClick = { model = item }) { Text(if (model == item) "✓ $item" else item) }
            }
            status?.let { Text(it, modifier = Modifier.padding(vertical = 12.dp)) }
            Button(onClick = { viewModel.save(url, key, model.ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) { Text("Save Cliproxy") }
        }
    }
}
