package com.flatnotes.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.flatnotes.data.NoteTint
import kotlinx.coroutines.launch

/**
 * 扁平配色。界面不使用 MaterialTheme，避免默认紫色、色调和投影。
 */
object FlatPalette {
    val Background = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF8E8E8E)
    val Placeholder = Color(0xFFC5C5C5)
    val Divider = Color(0xFFEEEEEE)
    val Accent = Color(0xFFFFC107)
    val AccentPressed = Color(0xFFFFB300)
    val Toolbar = Color(0xFFF5F5F5)
    val Icon = Color(0xFF212121)
    val IconMuted = Color(0xFF666666)
    val HintBackground = Color(0xFFFFF8E1)
    val HintText = Color(0xFF8D6E00)
    val Destructive = Color(0xFFC62828)
    val Scrim = Color(0x33000000)
    val Pressed = Color(0x14000000)
}

data class TintStyle(
    val background: Color,
    val border: Color,
)

fun NoteTint.toStyle(): TintStyle = when (this) {
    NoteTint.Yellow -> TintStyle(Color(0xFFFFF6D8), Color(0xFFE4D08A))
    NoteTint.Blue -> TintStyle(Color(0xFFE7F2FC), Color(0xFFC9DCF1))
    NoteTint.Green -> TintStyle(Color(0xFFE5F6E8), Color(0xFFC3E3C9))
    NoteTint.White -> TintStyle(Color(0xFFFFFFFF), Color(0xFFE3E3E3))
}

object FlatDimens {
    val Radius = 8.dp
    val ScreenHorizontal = 20.dp
    val GridGap = 8.dp
    val GridPadding = 12.dp
    val FabSize = 56.dp
}

@Stable
private object FlatPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return FlatPressNode(interactionSource)
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 1001
}

private class FlatPressNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) {
            drawRect(color = FlatPalette.Pressed)
        }
    }
}

@Composable
fun FlatTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIndication provides FlatPressIndication,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = FlatPalette.Accent,
            backgroundColor = FlatPalette.Accent.copy(alpha = 0.28f),
        ),
        content = content,
    )
}
