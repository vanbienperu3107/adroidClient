package dev.chungjungsoo.gptmobile

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeBrowseData
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryMessage
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryPart
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeModelOption
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection
import dev.chungjungsoo.gptmobile.presentation.ui.opencode.BoardOpenCodeTimeline
import dev.chungjungsoo.gptmobile.presentation.ui.setting.SettingsNavigationItems
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FigmaBoardVisualInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun capturesApprovedChatsBoard() {
        composeRule.setContent {
            Frame {
                Column(Modifier.fillMaxSize()) {
                    BoardHeader("Chats", action = "⌕")
                    BoardRow("New chat", "OpenCode or Direct provider", "✎", {}, trailing = "Choose", Modifier.padding(18.dp))
                    BoardSection("OpenCode chats", Modifier.padding(horizontal = 18.dp))
                    BoardRow("Figma MCP review", "OpenCode chat · idle", "▣", {}, modifier = Modifier.padding(horizontal = 18.dp))
                    BoardSection("Direct chats", Modifier.padding(horizontal = 18.dp))
                    BoardRow("Brainstorm UI", "Cliproxy · Today", "○", {}, modifier = Modifier.padding(horizontal = 18.dp))
                }
            }
        }
        write("figma-chats.png")
    }

    @Test fun capturesApprovedOpenCodeBoard() {
        val data = OpenCodeBrowseData(messages = listOf(OpenCodeHistoryMessage("user", "user", listOf(OpenCodeHistoryPart("user-part", "text", "opencode có thể kết nối MCP trực tiếp figma không?"))), OpenCodeHistoryMessage("assistant", "assistant", listOf(OpenCodeHistoryPart("assistant-part", "text", "Có. OpenCode có thể gọi Figma MCP. Quyền truy cập phụ thuộc server và credential được cấu hình.")))))
        composeRule.setContent {
            Frame {
                Column(Modifier.fillMaxSize()) {
                    BoardHeader("Figma MCP review", "OpenCode chat · Engineering project", {}, "⋮")
                    BoardOpenCodeTimeline(Modifier.weight(1f), data, false, OpenCodeModelOption("cliproxy", "gpt-5.6-terra", "gpt-5.6-terra", listOf("high")), "high", {})
                    BoardRow("Ask OpenCode…", "gpt-5.6-terra · high", "+", {}, trailing = "↑", modifier = Modifier.padding(18.dp))
                }
            }
        }
        write("figma-opencode-chat.png")
    }

    @Test fun capturesApprovedSettingsBoard() {
        composeRule.setContent {
            Frame {
                Column(Modifier.fillMaxSize()) {
                    BoardHeader("Settings", "Configure app, providers and workspace", {}, "⋮")
                    SettingsNavigationItems({}, {}, {}, {}, {})
                }
            }
        }
        write("figma-settings.png")
    }

    @Composable
    private fun Frame(content: @Composable () -> Unit) = MaterialTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .testTag("figma-evidence-screen")
        ) {
            Box(Modifier.requiredSize(390.dp, 844.dp)) { content() }
        }
    }

    private fun write(name: String) {
        composeRule.waitForIdle()
        val image = composeRule.onNodeWithTag("figma-evidence-screen").captureToImage()
        check(image.width >= 300 && image.height >= 600) { "Expected a full emulator frame, got ${image.width}x${image.height}" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/figma-evidence")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri).use { output ->
            check(output != null && image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
