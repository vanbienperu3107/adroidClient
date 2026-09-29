package dev.chungjungsoo.gptmobile

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
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
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FigmaBoardVisualInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun capturesApprovedChatsBoard() {
        composeRule.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().testTag("figma-evidence-screen")) {
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
        val data = OpenCodeBrowseData(
            messages = listOf(
                OpenCodeHistoryMessage("user", "user", listOf(OpenCodeHistoryPart("user-part", "text", "opencode có thể kết nối MCP trực tiếp figma không?"))),
                OpenCodeHistoryMessage("assistant", "assistant", listOf(OpenCodeHistoryPart("assistant-part", "text", "Có. OpenCode có thể gọi Figma MCP. Quyền truy cập phụ thuộc server và credential được cấu hình.")))
            )
        )
        composeRule.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().testTag("figma-evidence-screen")) {
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
            MaterialTheme {
                Column(Modifier.fillMaxSize().testTag("figma-evidence-screen")) {
                    BoardHeader("Settings", "Configure app, providers and workspace", {}, "⋮")
                    SettingsNavigationItems({}, {}, {}, {}, {})
                }
            }
        }
        write("figma-settings.png")
    }

    private fun write(name: String) {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "figma-evidence").also { it.mkdirs() }
        FileOutputStream(File(dir, name)).use { output ->
            check(composeRule.onNodeWithTag("figma-evidence-screen").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
