package dev.chungjungsoo.gptmobile.presentation.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.gptmobile.R
import dev.chungjungsoo.gptmobile.data.model.ApiType
import dev.chungjungsoo.gptmobile.presentation.theme.GPTMobileTheme
import dev.chungjungsoo.gptmobile.util.getPlatformAPIBrandText

@Composable
fun UserChatBubble(
    modifier: Modifier = Modifier,
    text: String,
    onCopyClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121212),
                contentColor = Color.White
            )
        ) {
            RichChatContent(text, Modifier.padding(16.dp))
        }
        CopyTextChip(onCopyClick)
    }
}

@Composable
fun OpponentChatBubble(
    modifier: Modifier = Modifier,
    canRetry: Boolean,
    isLoading: Boolean,
    isError: Boolean = false,
    apiType: ApiType,
    text: String,
    onCopyClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Card(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF141414)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                RichChatContent(text.trimIndent() + if (isLoading) "▊" else "", Modifier.padding(24.dp))
                if (!isLoading) {
                    BrandText(apiType)
                }
            }

            if (!isLoading) {
                Row {
                    if (!isError) {
                        CopyTextChip(onCopyClick)
                    }
                    if (canRetry) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RetryChip(onRetryClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyTextChip(onCopyClick: () -> Unit) {
    AssistChip(
        onClick = onCopyClick,
        label = { Text(stringResource(R.string.copy_text)) },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                contentDescription = stringResource(R.string.copy_text),
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Composable
private fun RetryChip(onRetryClick: () -> Unit) {
    AssistChip(
        onClick = onRetryClick,
        label = { Text(stringResource(R.string.retry)) },
        leadingIcon = {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.retry),
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Composable
private fun BrandText(apiType: ApiType) {
    Box(
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = getPlatformAPIBrandText(apiType),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
fun UserChatBubblePreview() {
    val sampleText = """
        How can I print hello world
        in Python?
    """.trimIndent()
    GPTMobileTheme {
        UserChatBubble(text = sampleText, onCopyClick = {})
    }
}

@Preview
@Composable
fun OpponentChatBubblePreview() {
    val sampleText = """
        # Demo
    
        Emphasis, aka italics, with *asterisks* or _underscores_. Strong emphasis, aka bold, with **asterisks** or __underscores__. Combined emphasis with **asterisks and _underscores_**. [Links with two blocks, text in square-brackets, destination is in parentheses.](https://www.example.com). Inline `code` has `back-ticks around` it.
    
        1. First ordered list item
        2. Another item
            * Unordered sub-list.
        3. And another item.
            You can have properly indented paragraphs within list items. Notice the blank line above, and the leading spaces (at least one, but we'll use three here to also align the raw Markdown).
    
        * Unordered list can use asterisks
        - Or minuses
        + Or pluses
    """.trimIndent()
    GPTMobileTheme {
        OpponentChatBubble(
            text = sampleText,
            canRetry = true,
            isLoading = false,
            apiType = ApiType.OPENAI,
            onCopyClick = {},
            onRetryClick = {}
        )
    }
}
