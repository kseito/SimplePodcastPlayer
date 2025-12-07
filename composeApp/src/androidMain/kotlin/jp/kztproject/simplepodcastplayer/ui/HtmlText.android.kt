package jp.kztproject.simplepodcastplayer.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
actual fun HtmlText(
    html: String,
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    textAlign: TextAlign?,
) {
    val annotatedString = AnnotatedString.fromHtml(html)

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}
