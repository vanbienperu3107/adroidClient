package dev.chungjungsoo.gptmobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeModelOption
import dev.chungjungsoo.gptmobile.presentation.ui.opencode.BoardVariantPicker
import dev.chungjungsoo.gptmobile.presentation.ui.opencode.OpenCodeModelPickerSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenCodeModelPickerInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun searchFiltersServerModelsAndSelectionReturnsTheExactModel() {
        val terra = OpenCodeModelOption("cliproxy", "gpt-5.6-terra", "GPT 5.6 Terra", listOf("low", "high"))
        val bunny = OpenCodeModelOption("free", "space-bunny", "Space Bunny Free", emptyList())
        var selected: OpenCodeModelOption? = null

        composeRule.setContent {
            MaterialTheme {
                OpenCodeModelPickerSheet(
                    models = listOf(terra, bunny),
                    selected = selected,
                    onRefresh = {},
                    onDismiss = {},
                    onSelect = { selected = it }
                )
            }
        }

        composeRule.onNodeWithTag("opencode-model-search").performTextInput("terra")
        composeRule.onNodeWithTag("opencode-model-cliproxy-gpt-5.6-terra").assertIsDisplayed().performClick()
        assertEquals(terra, selected)
    }

    @Test fun reasoningPickerReturnsSelectedServerLevelOrDefault() {
        val model = OpenCodeModelOption("cliproxy", "gpt-5.6-terra", "GPT 5.6 Terra", listOf("low", "high", "xhigh"))
        var selected: String? = "high"
        composeRule.setContent {
            MaterialTheme {
                BoardVariantPicker(model, selected, {}, { selected = it })
            }
        }

        composeRule.onNodeWithText("Server default").performClick()
        assertEquals(null, selected)
        composeRule.setContent {
            MaterialTheme {
                BoardVariantPicker(model, selected, {}, { selected = it })
            }
        }
        composeRule.onNodeWithText("xhigh").performClick()
        assertEquals("xhigh", selected)
    }
}
