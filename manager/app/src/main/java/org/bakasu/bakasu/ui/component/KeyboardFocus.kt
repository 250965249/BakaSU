package org.bakasu.bakasu.ui.component

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a keyboard focus ring while [focused] is true.
 *
 * Compose only produces [androidx.compose.foundation.interaction.FocusInteraction] events for
 * hardware keyboard and D-pad traversal, so touch interaction never shows the ring. Components that
 * render their own clickable surface instead of a Material 3 primitive must attach this modifier to
 * stay visible during keyboard navigation.
 *
 * @param focused Whether the owning component currently holds focus.
 * @param shape The shape the ring follows. Usually the same shape used to clip the component.
 * @param color The ring color.
 * @param width The ring width.
 */
fun Modifier.keyboardFocusRing(
    focused: Boolean,
    shape: Shape,
    color: Color,
    width: Dp = 2.dp,
): Modifier = if (focused) {
    border(width = width, color = color, shape = shape)
} else {
    this
}
