package dev.chungjungsoo.gptmobile.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onItemClick: () -> Unit,
    showTrailingIcon: Boolean,
    showLeadingIcon: Boolean,
    leadingIcon: @Composable () -> Unit? = {}
) {
    BoardRow(
        title = title,
        subtitle = description,
        icon = if (showLeadingIcon) "•" else "",
        onClick = if (enabled) onItemClick else null,
        trailing = if (showTrailingIcon) "›" else "",
        modifier = modifier
    )
}
