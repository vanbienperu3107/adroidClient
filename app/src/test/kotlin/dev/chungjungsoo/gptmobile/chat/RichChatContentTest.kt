package dev.chungjungsoo.gptmobile.chat

import dev.chungjungsoo.gptmobile.presentation.ui.chat.HtmlSandboxPolicy
import dev.chungjungsoo.gptmobile.presentation.ui.chat.RichChatBlock
import dev.chungjungsoo.gptmobile.presentation.ui.chat.richChatBlocks
import dev.chungjungsoo.gptmobile.presentation.ui.chat.safeImageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichChatContentTest {
    @Test fun parsesOnlyExplicitMermaidAndHtmlFences() {
        val blocks = richChatBlocks("before\n```mermaid\nflowchart TD\nA-->B\n```\n```html\n<h1>Safe</h1>\n```")
        assertTrue(blocks.any { it is RichChatBlock.Mermaid })
        assertTrue(blocks.any { it is RichChatBlock.Html })
    }

    @Test fun unsafeImageSchemesStayText() {
        assertTrue(safeImageSource("https://example.com/chart.png"))
        assertTrue(safeImageSource("data:image/png;base64,AA=="))
        assertFalse(safeImageSource("http://example.com/chart.png"))
        assertFalse(safeImageSource("file:///sdcard/chart.png"))
        assertFalse(safeImageSource("javascript:alert(1)"))
        assertFalse(safeImageSource("data:image/png,not-base64"))
    }

    @Test fun unsafeMarkdownImageIsNotAnArtifact() {
        val blocks = richChatBlocks("![x](file:///data/local/tmp/a.png)")
        assertEquals(1, blocks.size)
        assertTrue(blocks.single() is RichChatBlock.Text)
    }

    @Test fun htmlSandboxPolicyDeniesCodeAndEveryExternalStorageOrNetworkPath() {
        val policy = HtmlSandboxPolicy()
        assertFalse(policy.javaScriptEnabled)
        assertFalse(policy.javaScriptCanOpenWindows)
        assertFalse(policy.fileAccess)
        assertFalse(policy.contentAccess)
        assertFalse(policy.networkLoads)
        assertFalse(policy.networkImages)
        assertFalse(policy.domStorage)
    }
}
