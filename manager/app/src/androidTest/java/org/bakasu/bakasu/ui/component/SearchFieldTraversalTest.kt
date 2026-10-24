package org.bakasu.bakasu.ui.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the explicit DOWN route that keeps a search field from trapping focus.
 *
 * On device the search field sits in the top app bar and directional search could not escape the
 * bar: DOWN wrapped back to the bar's navigation button, so the list below was unreachable. The
 * field now names its DOWN target explicitly, and these tests pin that contract.
 *
 * The rule starts in [InputMode.Keyboard]; Compose testing defaults to touch mode, where focus
 * traversal does not behave like a hardware keyboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class SearchFieldTraversalTest {
    @get:Rule
    val composeTestRule = createComposeRule(
        config = ComposeUiTestConfig(inputMode = InputMode.Keyboard),
    )

    private fun Modifier.focusItem(tag: String, h: Int = 120): Modifier = this.testTag(tag).fillMaxWidth().height(h.dp).focusable()

    @Test
    fun explicitDownRouteMovesFromSearchFieldIntoListContainer() {
        composeTestRule.setContent {
            // The requester is attached to the content container, not to a single row, so screens
            // whose first item is not itself focusable can still route DOWN into their content.
            val contentFocusRequester = remember { FocusRequester() }
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Box(Modifier.fillMaxWidth().height(110.dp).padding(start = 50.dp)) {
                    Box(Modifier.testTag("top_button").size(110.dp).focusable())
                }
                BasicTextField(
                    state = rememberTextFieldState(""),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier
                        .testTag("search_field")
                        .fillMaxWidth()
                        .height(58.dp)
                        .focusProperties { down = contentFocusRequester },
                )
                LazyColumn(Modifier.fillMaxSize().focusRequester(contentFocusRequester)) {
                    items(4) { i ->
                        Box(Modifier.focusItem("row_$i", 240))
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("top_button").assertIsFocused()

        composeTestRule.onNodeWithTag("top_button").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("search_field").assertIsFocused()

        // Without the explicit route focus wrapped back to top_button here.
        composeTestRule.onNodeWithTag("search_field").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("row_0").assertIsFocused()

        composeTestRule.onNodeWithTag("row_0").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("row_1").assertIsFocused()
    }
}
