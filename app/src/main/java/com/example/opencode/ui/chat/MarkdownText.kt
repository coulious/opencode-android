package com.example.opencode.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    SelectionContainer {
        Column(modifier) {
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.CodeBlock -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                block.code,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    is MdBlock.Heading -> {
                        Text(
                            block.text,
                            style = when (block.level) {
                                1 -> MaterialTheme.typography.titleLarge
                                2 -> MaterialTheme.typography.titleMedium
                                else -> MaterialTheme.typography.titleSmall
                            }.copy(fontWeight = FontWeight.Bold),
                            color = color,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    is MdBlock.Quote -> {
                        Box(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                            Text(
                                block.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is MdBlock.ListItem -> {
                        Text(
                            buildAnnotatedString {
                                append("  •  ")
                                appendInlineMarkdown(block.text, codeBg)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                    }
                    is MdBlock.Paragraph -> {
                    Text(
                        buildAnnotatedString { appendInlineMarkdown(block.text, codeBg) },
                        style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

private sealed class MdBlock {
    data class CodeBlock(val code: String) : MdBlock()
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class ListItem(val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
}

private fun parseMarkdownBlocks(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trimStart().startsWith("```")) {
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                if (code.isNotEmpty()) code.append("\n")
                code.append(lines[i])
                i++
            }
            blocks.add(MdBlock.CodeBlock(code.toString()))
            i++ // skip closing ```
            continue
        }

        // Heading
        val headingMatch = Regex("^(#{1,3})\\s+(.+)").matchEntire(line)
        if (headingMatch != null) {
            blocks.add(MdBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++
            continue
        }

        // Quote
        if (line.startsWith("> ")) {
            blocks.add(MdBlock.Quote(line.removePrefix("> ")))
            i++
            continue
        }

        // List item
        if (line.matches(Regex("^\\s*[-*+]\\s+.*"))) {
            blocks.add(MdBlock.ListItem(line.replaceFirst(Regex("^\\s*[-*+]\\s+"), "")))
            i++
            continue
        }

        // Empty line
        if (line.isBlank()) {
            i++
            continue
        }

        // Paragraph - collect consecutive non-empty lines
        val para = StringBuilder()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].matches(Regex("^#{1,3}\\s+.*")) &&
            !lines[i].startsWith("> ") &&
            !lines[i].matches(Regex("^\\s*[-*+]\\s+.*"))
        ) {
            if (para.isNotEmpty()) para.append(" ")
            para.append(lines[i])
            i++
        }
        if (para.isNotEmpty()) blocks.add(MdBlock.Paragraph(para.toString()))
    }

    return blocks
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, codeBg: Color) {
    var pos = 0
    while (pos < text.length) {
        // Code `...`
        if (text[pos] == '`') {
            val end = text.indexOf('`', pos + 1)
            if (end != -1) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    background = codeBg,
                )) {
                    append(text.substring(pos + 1, end))
                }
                pos = end + 1
                continue
            }
        }

        // Bold **...**
        if (pos + 1 < text.length && text[pos] == '*' && text[pos + 1] == '*') {
            val end = text.indexOf("**", pos + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(pos + 2, end))
                }
                pos = end + 2
                continue
            }
        }

        // Italic *...*
        if (text[pos] == '*' && (pos + 1 < text.length && text[pos + 1] != '*')) {
            val end = text.indexOf('*', pos + 1)
            if (end != -1 && end > pos + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(pos + 1, end))
                }
                pos = end + 1
                continue
            }
        }

        // Italic _..._
        if (text[pos] == '_') {
            val end = text.indexOf('_', pos + 1)
            if (end != -1 && end > pos + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(pos + 1, end))
                }
                pos = end + 1
                continue
            }
        }

        append(text[pos])
        pos++
    }
}
