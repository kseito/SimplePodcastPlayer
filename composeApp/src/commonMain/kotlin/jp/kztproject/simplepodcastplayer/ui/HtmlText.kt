package jp.kztproject.simplepodcastplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * A composable that displays HTML text with proper formatting.
 * Platform-specific implementations handle HTML parsing differently.
 *
 * @param html The HTML string to display
 * @param modifier Modifier to apply to the Text
 * @param style Text style to apply
 * @param color Text color
 * @param maxLines Maximum number of lines
 * @param overflow How to handle text overflow
 * @param textAlign Text alignment
 */
@Composable
expect fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
)
