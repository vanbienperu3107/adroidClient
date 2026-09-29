package dev.chungjungsoo.gptmobile.presentation.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection

@Composable
fun AboutScreen(onNavigationClick: () -> Unit, onNavigationToLicense: () -> Unit) {
    val version = LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0).versionName
    Column(Modifier.fillMaxSize()) {
        BoardHeader("About and license", "Support and legal", onNavigationClick, "⋮")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            BoardSection("App")
            BoardRow("Version", "v$version", "i", trailing = "")
            BoardRow("License", "Open source licenses", "i", onNavigationToLicense)
            BoardRow("GitHub", "Project source", "↗", trailing = "›")
            BoardRow("Bug Report", "Something is not working correctly?", "!", trailing = "›")
            BoardRow("Feedback", "We are open to all feedback!", "i", trailing = "›")
        }
    }
}
