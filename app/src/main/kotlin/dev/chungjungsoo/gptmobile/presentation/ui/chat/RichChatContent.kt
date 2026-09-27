package dev.chungjungsoo.gptmobile.presentation.ui.chat

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import dev.chungjungsoo.gptmobile.presentation.ui.opencode.OpenCodeMarkdown
import java.net.URI
import java.util.Base64

internal data class HtmlSandboxPolicy(
    val javaScriptEnabled: Boolean = false,
    val javaScriptCanOpenWindows: Boolean = false,
    val fileAccess: Boolean = false,
    val contentAccess: Boolean = false,
    val networkLoads: Boolean = false,
    val networkImages: Boolean = false,
    val domStorage: Boolean = false
)
sealed interface RichChatBlock {
    data class Text(val value: String) : RichChatBlock
    data class Mermaid(val source: String) : RichChatBlock
    data class Html(val source: String) : RichChatBlock
    data class Image(val source: String) : RichChatBlock
}

/** Parses only explicit Markdown fences and image syntax. All other model output stays text. */
fun richChatBlocks(text: String): List<RichChatBlock> {
    val blocks = mutableListOf<RichChatBlock>()
    val fence = Regex("```(mermaid|html)\\s*\\n([\\s\\S]*?)```")
    var cursor = 0
    fence.findAll(text).forEach { match ->
        if (match.range.first > cursor) blocks += RichChatBlock.Text(text.substring(cursor, match.range.first))
        val type = match.groupValues[1]
        val source = match.groupValues[2].trim()
        if (source.isNotEmpty()) blocks += if (type == "mermaid") RichChatBlock.Mermaid(source) else RichChatBlock.Html(source)
        cursor = match.range.last + 1
    }
    if (cursor < text.length) blocks += parseImagesOrText(text.substring(cursor))
    return blocks.ifEmpty { listOf(RichChatBlock.Text(text)) }
}

private fun parseImagesOrText(value: String): List<RichChatBlock> {
    val image = Regex("!\\[[^]]*]\\(([^)]+)\\)")
    val blocks = mutableListOf<RichChatBlock>()
    var cursor = 0
    image.findAll(value).forEach { match ->
        if (match.range.first > cursor) blocks += RichChatBlock.Text(value.substring(cursor, match.range.first))
        val source = match.groupValues[1].trim()
        if (safeImageSource(source)) blocks += RichChatBlock.Image(source) else blocks += RichChatBlock.Text(match.value)
        cursor = match.range.last + 1
    }
    if (cursor < value.length) blocks += RichChatBlock.Text(value.substring(cursor))
    return blocks
}

fun safeImageSource(source: String): Boolean {
    val dataImage = Regex("^data:image/(png|jpeg|jpg|gif|webp);base64,[A-Za-z0-9+/=\\s]+$")
    if (dataImage.matches(source)) return true
    return runCatching {
        URI(source).let { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() }
    }.getOrDefault(false)
}

@Composable
internal fun RichChatContent(text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        richChatBlocks(text).forEach { block ->
            when (block) {
                is RichChatBlock.Text -> if (block.value.isNotBlank()) OpenCodeMarkdown(block.value.trim())
                is RichChatBlock.Mermaid -> MermaidArtifact(block.source)
                is RichChatBlock.Html -> HtmlArtifact(block.source)
                is RichChatBlock.Image -> ImageArtifact(block.source)
            }
        }
    }
}

@Composable
private fun MermaidArtifact(source: String) {
    var sourceVisible by remember { mutableStateOf(false) }
    ArtifactCard("Diagram", "Mermaid", sourceVisible, { sourceVisible = !sourceVisible }) {
        if (sourceVisible) {
            Text(source, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        } else {
            // This is deliberately native and limited: Mermaid source never reaches a browser engine.
            source.lines().filter { it.isNotBlank() && !it.startsWith("flowchart") && !it.startsWith("graph") }.take(16).forEach { line ->
                Text(line.replace("-->", " → ").replace("[", "").replace("]", ""), modifier = Modifier.padding(vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun HtmlArtifact(source: String) {
    var sourceVisible by remember { mutableStateOf(false) }
    ArtifactCard("HTML", if (sourceVisible) "Source" else "Preview", sourceVisible, { sourceVisible = !sourceVisible }) {
        if (sourceVisible) {
            Text(source, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        } else {
            SandboxedHtml(source)
        }
    }
}

@Composable
private fun ImageArtifact(source: String) {
    ArtifactCard("Image", "Preview", false, {}) {
        val bitmap = remember(source) { decodeDataImage(source) }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated image",
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
            )
        } else {
            AsyncImage(
                model = source,
                contentDescription = "Generated image",
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
            )
        }
    }
}

private fun decodeDataImage(source: String) = runCatching {
    if (!source.startsWith("data:image/")) return@runCatching null
    val payload = source.substringAfter("base64,", missingDelimiterValue = "")
    val bytes = Base64.getDecoder().decode(payload)
    if (payload.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

@Composable
private fun ArtifactCard(kind: String, action: String, sourceVisible: Boolean, onAction: () -> Unit, body: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(kind, style = MaterialTheme.typography.labelLarge)
            if (kind != "Image") AssistChip(onClick = onAction, label = { Text(if (sourceVisible) "Preview" else action) })
            body()
        }
    }
}

/** Applies every WebView isolation switch before any untrusted document is loaded. */
@SuppressLint("SetJavaScriptEnabled")
internal fun configureSandboxedHtml(settings: WebSettings) {
    val policy = HtmlSandboxPolicy()
    settings.javaScriptEnabled = policy.javaScriptEnabled
    settings.javaScriptCanOpenWindowsAutomatically = policy.javaScriptCanOpenWindows
    settings.allowFileAccess = policy.fileAccess
    settings.allowContentAccess = policy.contentAccess
    settings.blockNetworkLoads = !policy.networkLoads
    settings.blockNetworkImage = !policy.networkImages
    settings.domStorageEnabled = policy.domStorage
}

private class SandboxedHtmlClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
}

@Composable
private fun SandboxedHtml(source: String) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 320.dp).background(MaterialTheme.colorScheme.surface),
        factory = {
            WebView(context).apply {
                configureSandboxedHtml(settings)
                webViewClient = SandboxedHtmlClient()
                loadDataWithBaseURL(null, source, "text/html", "utf-8", null)
            }
        },
        update = { view -> view.loadDataWithBaseURL(null, source, "text/html", "utf-8", null) }
    )
}
