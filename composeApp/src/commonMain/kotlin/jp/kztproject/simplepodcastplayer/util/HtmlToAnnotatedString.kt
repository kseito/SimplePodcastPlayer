package jp.kztproject.simplepodcastplayer.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Converts HTML text to AnnotatedString with basic HTML tag support.
 * Supports: <p>, <br>, <b>, <strong>, <i>, <em>, and strips other tags.
 */
fun String.htmlToAnnotatedString(): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0
    val text = this@htmlToAnnotatedString
    val textLength = text.length

    val boldStack = mutableListOf<Int>()
    val italicStack = mutableListOf<Int>()

    while (currentIndex < textLength) {
        if (text[currentIndex] == '<') {
            val tagEnd = text.indexOf('>', currentIndex)
            if (tagEnd == -1) {
                // Malformed HTML, treat '<' as text
                append(text[currentIndex])
                currentIndex++
                continue
            }

            val tag = text.substring(currentIndex + 1, tagEnd).lowercase()
            val cleanTag = tag.split(' ')[0] // Remove attributes

            when {
                cleanTag == "br" || cleanTag == "br/" -> {
                    append('\n')
                }
                cleanTag == "p" -> {
                    if (this.length > 0 && this.text.lastOrNull() != '\n') {
                        append('\n')
                    }
                }
                cleanTag == "/p" -> {
                    if (this.length > 0 && this.text.lastOrNull() != '\n') {
                        append('\n')
                    }
                }
                cleanTag == "b" || cleanTag == "strong" -> {
                    boldStack.add(this.length)
                }
                cleanTag == "/b" || cleanTag == "/strong" -> {
                    if (boldStack.isNotEmpty()) {
                        val start = boldStack.removeAt(boldStack.size - 1)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, this.length)
                    }
                }
                cleanTag == "i" || cleanTag == "em" -> {
                    italicStack.add(this.length)
                }
                cleanTag == "/i" || cleanTag == "/em" -> {
                    if (italicStack.isNotEmpty()) {
                        val start = italicStack.removeAt(italicStack.size - 1)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, this.length)
                    }
                }
                // Strip other tags (like <a>, <div>, etc.)
            }

            currentIndex = tagEnd + 1
        } else {
            // Handle HTML entities
            if (text[currentIndex] == '&') {
                val entityEnd = text.indexOf(';', currentIndex)
                if (entityEnd != -1 && entityEnd - currentIndex < 10) {
                    val entity = text.substring(currentIndex, entityEnd + 1)
                    val char = when (entity) {
                        "&amp;" -> '&'
                        "&lt;" -> '<'
                        "&gt;" -> '>'
                        "&quot;" -> '"'
                        "&apos;" -> '\''
                        "&nbsp;" -> ' '
                        else -> null
                    }
                    if (char != null) {
                        append(char)
                        currentIndex = entityEnd + 1
                        continue
                    }
                }
            }

            append(text[currentIndex])
            currentIndex++
        }
    }
}
