package dev.chungjungsoo.gptmobile.presentation.ui.opencode

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Conservative block Markdown subset: never loads images or executes HTML. */
@Composable
internal fun OpenCodeMarkdown(text: String) {
    Column {
        var code = false
        text.lines().forEach { line ->
            when {
                line.startsWith("```") -> code = !code
                code -> Text(line, fontFamily = FontFamily.Monospace)
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall)
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleLarge)
                line.startsWith("### ") -> Text(line.removePrefix("### "), fontWeight = FontWeight.Bold)
                line.startsWith("- ") || line.startsWith("* ") -> Text("• " + line.drop(2))
                line.startsWith("> ") -> Text("│ " + line.drop(2), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(line)
            }
        }
    }
}
