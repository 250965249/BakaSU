package org.bakasu.bakasu.ui.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins keyboard and D-pad traversal over a screen layout.
 *
 * The layout mirrors the app shell: content above, a bottom bar below, all inside one plain
 * container. An earlier attempt wrapped that container in `focusable()` to bootstrap focus; on
 * device it stranded focus on the container and directional keys did nothing at all. These tests
 * lock in that the container must stay a plain, non-focusable box.
 *
 * The rule starts in [InputMode.Keyboard] because Compose testing defaults to touch mode, where
 * focus indications are suppressed and traversal does not behave like a hardware keyboard.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenFocusTraversalTest {
    @get:Rule
    val composeTestRule = createComposeRule(
        config = ComposeUiTestConfig(inputMode = InputMode.Keyboard),
    )

    private fun Modifier.focusItem(tag: String, itemSize: Dp = 120.dp): Modifier =
        this.testTag(tag).size(itemSize).focusable()

    @Test
    fun firstDirectionalKeyEntersContentWithoutFocusableContainer() {
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Box(Modifier.focusItem("item_0"))
                Box(Modifier.focusItem("item_1"))
            }
        }

        // Nothing holds focus on entry; the platform must still route the key into the content.
        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.DirectionDown) }

        composeTestRule.onNodeWithTag("item_0").assertIsFocused()
    }

    @Test
    fun directionalKeyMovesThroughContentWithoutWrapping() {
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Box(Modifier.focusItem("item_0"))
                Box(Modifier.focusItem("item_1"))
                Box(Modifier.focusItem("item_2"))
            }
        }

        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_0").assertIsFocused()

        composeTestRule.onNodeWithTag("item_0").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_1").assertIsFocused()

        composeTestRule.onNodeWithTag("item_1").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_2").assertIsFocused()

        // Directional search does not wrap around at the end of the list.
        composeTestRule.onNodeWithTag("item_2").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_2").assertIsFocused()
    }

    @Test
    fun directionalKeyReachesBarBelowContent() {
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.focusItem("item_0"))
                    Box(Modifier.focusItem("item_1"))
                }
                Row(
                    Modifier.fillMaxWidth().height(80.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.focusItem("tab_0", 60.dp))
                    Box(Modifier.focusItem("tab_1", 60.dp))
                }
            }
        }

        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_0").assertIsFocused()

        composeTestRule.onNodeWithTag("item_0").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_1").assertIsFocused()

        composeTestRule.onNodeWithTag("item_1").performKeyInput { pressKey(Key.DirectionDown) }

        // Focus must leave the content column and land on the bar instead of stopping dead.
        composeTestRule.onNodeWithTag("tab_0").assertIsFocused()
    }

    @Test
    fun directionalKeyReturnsUpwardFromBar() {
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.focusItem("item_0"))
                    Box(Modifier.focusItem("item_1"))
                }
                Row(
                    Modifier.fillMaxWidth().height(80.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.focusItem("tab_0", 60.dp))
                    Box(Modifier.focusItem("tab_1", 60.dp))
                }
            }
        }

        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_0").assertIsFocused()
        composeTestRule.onNodeWithTag("item_0").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("item_1").assertIsFocused()
        composeTestRule.onNodeWithTag("item_1").performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.onNodeWithTag("tab_0").assertIsFocused()

        composeTestRule.onNodeWithTag("tab_0").performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.onNodeWithTag("item_1").assertIsFocused()
    }

    @Test
    fun tabKeyTraversesInAppearanceOrderAndBackwards() {
        composeTestRule.setContent {
            Column(Modifier.fillMaxSize().testTag("screen")) {
                Box(Modifier.focusItem("item_0"))
                Box(Modifier.focusItem("item_1"))
            }
        }

        composeTestRule.onNodeWithTag("screen").performKeyInput { pressKey(Key.Tab) }
        composeTestRule.onNodeWithTag("item_0").assertIsFocused()

        composeTestRule.onNodeWithTag("item_0").performKeyInput { pressKey(Key.Tab) }
        composeTestRule.onNodeWithTag("item_1").assertIsFocused()

        composeTestRule.onNodeWithTag("item_1").performKeyInput {
            withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
        }
        composeTestRule.onNodeWithTag("item_0").assertIsFocused()
    }
}
