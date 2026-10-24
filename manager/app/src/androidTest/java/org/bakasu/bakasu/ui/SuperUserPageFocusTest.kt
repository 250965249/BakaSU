package org.bakasu.bakasu.ui

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards directional traversal on the real superuser page.
 *
 * The page's search field sits in the top app bar. Before the DOWN route was made explicit, focus
 * wrapped back to the bar's navigation button and the app list below was unreachable, so a D-pad
 * user could not get into the content at all.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SuperUserPageFocusTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>(
        config = ComposeUiTestConfig(inputMode = InputMode.Keyboard),
    )

    private fun focusedText(): String {
        val nodes = composeTestRule.onAllNodes(isFocused()).fetchSemanticsNodes()
        if (nodes.isEmpty()) return ""
        return nodes.joinToString(" | ") { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                ?.joinToString("/") { it.text }.orEmpty() +
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.joinToString("/").orEmpty()
        }
    }

    private fun pressDown() {
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()
        Thread.sleep(300)
        composeTestRule.waitForIdle()
    }

    @Test
    fun directionalKeyReachesAppListFromSearchField() {
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("超级用户")[0].performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        pressDown()
        val onSearchField = focusedText()
        println("SUPAGE search field -> '$onSearchField'")

        pressDown()
        val afterSearch = focusedText()
        println("SUPAGE after search -> '$afterSearch'")

        // Without the explicit DOWN route this landed back on the top app bar instead.
        pressDown()
        val inList = focusedText()
        println("SUPAGE in list -> '$inList'")

        check(inList.isNotEmpty()) { "focus was lost after leaving the search field" }
        check(!inList.contains("超级用户访问日志")) {
            "focus wrapped back to the top app bar instead of entering the list: '$inList'"
        }
    }
}
