package jp.kztproject.simplepodcastplayer.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
    val plainText = html.stripHtmlTags()

    Text(
        text = plainText,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

// TODO: 一旦HTMLタグを削除するだけにする。UIKitView + NSAttributedStringという組み合わせでHTMLタグを反映させたい。
// HTMLタグを削除するヘルパー関数
private fun String.stripHtmlTags(): String = this.replace(Regex("<[^>]*>"), "")
