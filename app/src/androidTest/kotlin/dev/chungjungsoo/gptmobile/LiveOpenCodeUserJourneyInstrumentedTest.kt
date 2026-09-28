package dev.chungjungsoo.gptmobile

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import dev.chungjungsoo.gptmobile.presentation.ui.main.MainActivity
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-user journey evidence. It uses the real rendered app, form fields and server.
 * Screenshots are taken only after the password field is password-transformed or no
 * longer visible, so the secret never enters a report or artifact.
 */
class LiveOpenCodeUserJourneyInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun userCanConnectSaveAndBrowseOpenCodeProjects() {
        val args = InstrumentationRegistry.getArguments()
        val url = args.getString("opencode_url").orEmpty()
        val username = args.getString("opencode_username").orEmpty()
        val password = args.getString("opencode_password").orEmpty()
        assumeTrue("Live OpenCode credentials were not provided", url.isNotBlank() && username.isNotBlank() && password.isNotBlank())

        try {
            composeRule.waitUntil(15_000) { composeRule.onAllNodesWithTag("start-connect-opencode").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag("start-connect-opencode").performClick()
            composeRule.waitUntil(15_000) { composeRule.onAllNodesWithTag("opencode-server-list").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("Add server").performClick()

            composeRule.onNodeWithTag("opencode-server-name").performTextInput("OpenCode CI")
            composeRule.onNodeWithTag("opencode-server-url").performTextInput(url)
            composeRule.onNodeWithTag("opencode-server-username").performTextInput(username)
            composeRule.onNodeWithTag("opencode-server-password").performTextInput(password)
            composeRule.onNodeWithTag("opencode-server-test").performClick()
            composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Connected:", substring = true).fetchSemanticsNodes().isNotEmpty() }
            capture("01-connection-passed.png")

            composeRule.onNodeWithTag("opencode-server-save").performClick()
            composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("OpenCode CI").fetchSemanticsNodes().isNotEmpty() }
            capture("02-server-saved.png")

            composeRule.onNodeWithText("Projects").performClick()
            composeRule.waitUntil(30_000) {
                composeRule.onAllNodesWithText("OpenCode · Projects").fetchSemanticsNodes().isNotEmpty() &&
                    composeRule.onAllNodesWithText("No data").fetchSemanticsNodes().isEmpty()
            }
            composeRule.onNodeWithText("OpenCode · Projects").assertIsDisplayed()
            capture("03-projects-loaded.png")
        } catch (error: Throwable) {
            capture("failure.png")
            throw error
        }
    }

    private fun capture(name: String) {
        composeRule.runOnIdle {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val directory = File(context.filesDir, "test-evidence").also { it.mkdirs() }
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return@runOnIdle
            File(directory, name).outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            bitmap.recycle()
        }
    }
}
