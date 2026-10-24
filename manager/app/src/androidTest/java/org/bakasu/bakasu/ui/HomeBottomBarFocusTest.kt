package org.bakasu.bakasu.ui

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for keyboard reachability of the bottom navigation bar from home content.
 *
 * The floating bar used to be painted on top of the scrolling home content. Directional focus
 * compares node centers, and the bar's center sat above the last card's center, so DOWN from the
 * final card could never select the bar and focus was stranded there permanently. The bar also had
 * no focus group, so LEFT/RIGHT escaped sideways into the page content instead of moving between
 * tabs.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeBottomBarFocusTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>(
        config = ComposeUiTestConfig(inputMode = InputMode.Keyboard),
    )

    private data class Focused(val top: Int, val left: Int, val label: String)

    private fun focused(): Focused? {
        val nodes = composeTestRule.onAllNodes(isFocused()).fetchSemanticsNodes()
        val node = nodes.firstOrNull() ?: return null
        val bounds = node.boundsInRoot
        if (bounds.isEmpty) return null
        val text = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("/") { it.text }.orEmpty()
        val desc = node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString("/").orEmpty()
        return Focused(bounds.top.toInt(), bounds.left.toInt(), text.ifEmpty { desc })
    }

    private fun press(key: Key): Focused? {
        composeTestRule.onRoot().performKeyInput { pressKey(key) }
        composeTestRule.waitForIdle()
        Thread.sleep(250)
        composeTestRule.waitForIdle()
        return focused()
    }

    private fun isInBottomBar(node: Focused?): Boolean = node != null && node.top >= BOTTOM_BAR_TOP

    @Test
    fun bottomBarIsReachableAndTraversable() {
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Walk down until focus reaches the bar; the bar must be reachable in a bounded number of
        // presses rather than never.
        var reached: Focused? = null
        repeat(MAX_DOWN_PRESSES) {
            if (reached != null) return@repeat
            val node = press(Key.DirectionDown)
            if (isInBottomBar(node)) reached = node
        }
        assertTrue(
            "DOWN never reached the bottom bar; last focus was $reached",
            isInBottomBar(reached),
        )

        // The bar is a focus group: sideways movement must stay inside it.
        val afterLeft = press(Key.DirectionLeft)
        assertTrue("LEFT left the bottom bar: $afterLeft", isInBottomBar(afterLeft))
        assertTrue(
            "LEFT did not move between tabs: ${reached?.label} -> ${afterLeft?.label}",
            afterLeft?.left != reached?.left,
        )

        val afterRight = press(Key.DirectionRight)
        assertTrue("RIGHT left the bottom bar: $afterRight", isInBottomBar(afterRight))
        assertTrue(
            "RIGHT did not return to the previous tab: $afterRight",
            afterRight?.left == reached?.left,
        )

        // UP must hand focus back to the page content above the bar.
        val afterUp = press(Key.DirectionUp)
        assertTrue("UP did not return to content: $afterUp", afterUp != null && !isInBottomBar(afterUp))
    }

    private companion object {
        // The floating bar occupies y=2180..2356 on the reference device; anything at or below
        // this line is inside it.
        const val BOTTOM_BAR_TOP = 2100
        const val MAX_DOWN_PRESSES = 20
    }
}
