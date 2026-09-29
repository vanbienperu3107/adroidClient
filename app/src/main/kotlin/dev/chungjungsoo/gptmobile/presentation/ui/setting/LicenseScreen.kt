package dev.chungjungsoo.gptmobile.presentation.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection

@Composable
fun LicenseScreen(onNavigationClick: () -> Unit) = Column(Modifier.fillMaxSize()) {
    BoardHeader("License", "Open source libraries", onNavigationClick, "⋮")
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        BoardSection("Libraries")
        listOf("Jetpack Compose", "Room", "Ktor", "kotlinx.serialization", "Hilt").forEach { BoardRow(it, "Apache License 2.0", trailing = "") }
    }
}
