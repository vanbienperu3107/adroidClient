package dev.chungjungsoo.gptmobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chungjungsoo.gptmobile.data.database.entity.ChatRoom
import dev.chungjungsoo.gptmobile.data.model.ApiType
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeSession
import dev.chungjungsoo.gptmobile.presentation.ui.home.DirectChatRow
import dev.chungjungsoo.gptmobile.presentation.ui.home.HomeTopAppBar
import dev.chungjungsoo.gptmobile.presentation.ui.home.NewChatChoiceDialog
import dev.chungjungsoo.gptmobile.presentation.ui.home.OpenCodeChatRow
import dev.chungjungsoo.gptmobile.presentation.ui.opencode.ServerActionMenu
import dev.chungjungsoo.gptmobile.presentation.ui.setting.SettingsNavigationItems
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class ApprovedUiUserJourneyInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun chatsRoutesOpenCodeAndDirectRowsAndExposesSearchAndSettings() {
        var route = ""
        val openCode = CachedOpenCodeSession("server", "/workspace/Project", "ses_123", 1, "OpenCode history", 1, "idle", 1)
        val direct = ChatRoom(7, "Direct history", listOf(ApiType.OPENAI))
        composeRule.setContent {
            MaterialTheme {
                Column {
                    HomeTopAppBar(false, 0, false, "", androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(), {}, { route = "search" }, { route = "settings" }, {})
                    OpenCodeChatRow(openCode) { route = "opencode:${openCode.serverId}:${openCode.directory}:${openCode.sessionId}" }
                    DirectChatRow(direct, false, false, emptyMap(), {}, { route = "direct:${direct.id}" }, {})
                }
            }
        }

        composeRule.onNodeWithContentDescription("Search chats").performClick()
        assertEquals("search", route)
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertEquals("settings", route)
        composeRule.onNodeWithTag("opencode-chat-ses_123").performClick()
        assertEquals("opencode:server:/workspace/Project:ses_123", route)
        composeRule.onNodeWithTag("direct-chat-7").performClick()
        assertEquals("direct:7", route)
    }

    @Test fun newChatChoiceUsesExplicitProviderSelection() {
        var choice = ""
        composeRule.setContent { MaterialTheme { NewChatChoiceDialog({}, { choice = "opencode" }, { choice = "direct" }) } }
        composeRule.onNodeWithTag("new-chat-opencode").performClick()
        assertEquals("opencode", choice)
        composeRule.onNodeWithTag("new-chat-direct").performClick()
        assertEquals("direct", choice)
    }

    @Test fun settingsExposesEverySupportedBranch() {
        var destination = ""
        composeRule.setContent {
            MaterialTheme {
                Column {
                    SettingsNavigationItems(
                        onTheme = { destination = "theme" },
                        onOpenCode = { destination = "opencode" },
                        onPlatform = { destination = it.name },
                        onCliproxy = { destination = "cliproxy" },
                        onAbout = { destination = "about" }
                    )
                }
            }
        }
        composeRule.onNodeWithTag("settings-theme").performClick()
        assertEquals("theme", destination)
        composeRule.onNodeWithTag("settings-opencode").performClick()
        assertEquals("opencode", destination)
        composeRule.onNodeWithTag("settings-cliproxy").performClick()
        assertEquals("cliproxy", destination)
        composeRule.onNodeWithTag("settings-about").performClick()
        assertEquals("about", destination)
        composeRule.onNodeWithTag("settings-openai").performClick()
        assertEquals("OPENAI", destination)
        composeRule.onNodeWithTag("settings-anthropic").performClick()
        assertEquals("ANTHROPIC", destination)
        composeRule.onNodeWithTag("settings-google").performClick()
        assertEquals("GOOGLE", destination)
        composeRule.onNodeWithTag("settings-ollama").performClick()
        assertEquals("OLLAMA", destination)
    }

    @Test fun serverActionsOfferOnlyDefaultProjectEditAndDelete() {
        var action = ""
        composeRule.setContent {
            MaterialTheme {
                ServerActionMenu(true, {}, { action = "default" }, { action = "edit" }, { action = "delete" })
            }
        }
        composeRule.onNodeWithText("Default project").assertIsDisplayed().performClick()
        assertEquals("default", action)
        composeRule.onNodeWithText("Edit").performClick()
        assertEquals("edit", action)
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals("delete", action)
        composeRule.onNodeWithText("Projects").assertDoesNotExist()
    }
}
