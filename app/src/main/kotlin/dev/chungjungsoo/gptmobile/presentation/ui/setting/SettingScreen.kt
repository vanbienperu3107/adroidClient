package dev.chungjungsoo.gptmobile.presentation.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.chungjungsoo.gptmobile.data.model.ApiType
import dev.chungjungsoo.gptmobile.data.model.DynamicTheme
import dev.chungjungsoo.gptmobile.data.model.ThemeMode
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection
import dev.chungjungsoo.gptmobile.presentation.common.LocalDynamicTheme
import dev.chungjungsoo.gptmobile.presentation.common.LocalThemeMode
import dev.chungjungsoo.gptmobile.presentation.common.LocalThemeViewModel
import dev.chungjungsoo.gptmobile.util.collectManagedState

@Composable
fun SettingScreen(modifier: Modifier = Modifier, settingViewModel: SettingViewModel = hiltViewModel(), onNavigationClick: () -> Unit, onNavigateToOpenCode: () -> Unit, onNavigateToPlatformSetting: (ApiType) -> Unit, onNavigateToCliproxy: () -> Unit, onNavigateToAboutPage: () -> Unit) {
    val dialog by settingViewModel.dialogState.collectManagedState()
    Column(modifier.fillMaxSize()) {
        BoardHeader("Settings", "Configure app, providers and workspace", onNavigationClick, "⋮")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SettingsNavigationItems(settingViewModel::openThemeDialog, onNavigateToOpenCode, onNavigateToPlatformSetting, onNavigateToCliproxy, onNavigateToAboutPage)
        }
    }
    if (dialog.isThemeDialogOpen) ThemeSettingDialog(settingViewModel)
}

@Composable
fun SettingsNavigationItems(onTheme: () -> Unit, onOpenCode: () -> Unit, onPlatform: (ApiType) -> Unit, onCliproxy: () -> Unit, onAbout: () -> Unit) {
    BoardSection("Appearance")
    BoardRow("Theme Settings", "Dynamic theme, Dark mode", "◐", onTheme)
    BoardSection("Providers")
    listOf(ApiType.OPENAI, ApiType.ANTHROPIC, ApiType.GOOGLE, ApiType.OLLAMA).forEach { type -> BoardRow("${type.name.lowercase().replaceFirstChar { it.uppercase() }} Settings", "API key, model, system prompt", "•", { onPlatform(type) }) }
    BoardSection("Connections")
    BoardRow("Cliproxy", "Direct provider configuration", "○", onCliproxy)
    BoardRow("OpenCode servers", "Server profiles and default project", "▣", onOpenCode)
    BoardSection("App")
    BoardRow("About", "Version, license, feedback", "i", onAbout)
}

@Composable
fun ThemeSetting(onItemClick: () -> Unit) = BoardRow("Theme Settings", "Dynamic theme, Dark mode", "◐", onItemClick)

@Composable
fun AboutPageItem(onItemClick: () -> Unit) = BoardRow("About", "Version, license, feedback", "i", onItemClick)

@Composable
fun ThemeSettingDialog(settingViewModel: SettingViewModel = hiltViewModel()) {
    val theme = LocalThemeViewModel.current
    AlertDialog(onDismissRequest = settingViewModel::closeThemeDialog, title = { Text("Theme settings") }, text = {
        Column {
            Text("Dynamic theme")
            DynamicTheme.entries.forEach { value -> TextButton(onClick = { theme.updateDynamicTheme(value) }) { Text(if (LocalDynamicTheme.current == value) "✓ ${value.name}" else value.name) } }
            Text("Dark mode", Modifier.padding(top = 12.dp))
            ThemeMode.entries.forEach { value -> TextButton(onClick = { theme.updateThemeMode(value) }) { Text(if (LocalThemeMode.current == value) "✓ ${value.name}" else value.name) } }
        }
    }, confirmButton = { TextButton(onClick = settingViewModel::closeThemeDialog) { Text("Done") } })
}
