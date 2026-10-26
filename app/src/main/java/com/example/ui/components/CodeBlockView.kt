package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CodeBg
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeFunction
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.InfinityCyan

@Composable
fun CodeBlockView(
    code: String,
    language: String = "kotlin",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isFullscreen by remember { mutableStateOf(false) }

    val highlightedText = remember(code, language) {
        highlightSyntax(code, language)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2E264E), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CodeBg)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161228))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFF282048), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = language.uppercase(),
                        color = InfinityCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fullscreen expand
                IconButton(
                    onClick = { isFullscreen = true },
                    modifier = Modifier.size(32.dp).testTag("code_fullscreen_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Expand code",
                        tint = Color(0xFFA59EC4),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Copy button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("code_copy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFA59EC4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlightedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = CodeBg
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161228))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$language source code",
                            color = InfinityCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = highlightedText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lightweight syntax highlighter with colors for keywords, strings, functions, comments, and numbers.
 */
fun highlightSyntax(code: String, language: String): AnnotatedString {
    val keywords = setOf(
        "val", "var", "fun", "class", "interface", "object", "package", "import",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "override", "private", "public", "protected", "internal", "data", "sealed",
        "enum", "companion", "suspend", "inline", "const", "true", "false", "null", "this", "super",
        "public", "static", "void", "new", "extends", "implements", "final", "boolean", "int", "String"
    )

    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { index, line ->
            // Line number prefix
            val lineNum = (index + 1).toString().padStart(3, ' ') + "  "
            pushStyle(SpanStyle(color = Color(0xFF554D7A)))
            append(lineNum)
            pop()

            // Tokenize simple line
            var i = 0
            while (i < line.length) {
                val ch = line[i]

                // Line comment
                if (i < line.length - 1 && line.substring(i, i + 2) == "//") {
                    pushStyle(SpanStyle(color = CodeComment))
                    append(line.substring(i))
                    pop()
                    break
                }

                // String literal
                if (ch == '"' || ch == '\'') {
                    val endQuote = line.indexOf(ch, i + 1)
                    val strToken = if (endQuote != -1) line.substring(i, endQuote + 1) else line.substring(i)
                    pushStyle(SpanStyle(color = CodeString))
                    append(strToken)
                    pop()
                    i += strToken.length
                    continue
                }

                // Words / Identifiers
                if (ch.isLetter() || ch == '_') {
                    val start = i
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                        i++
                    }
                    val word = line.substring(start, i)
                    if (keywords.contains(word)) {
                        pushStyle(SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Bold))
                        append(word)
                        pop()
                    } else if (i < line.length && line[i] == '(') {
                        pushStyle(SpanStyle(color = CodeFunction))
                        append(word)
                        pop()
                    } else {
                        pushStyle(SpanStyle(color = Color(0xFFF2F0FA)))
                        append(word)
                        pop()
                    }
                    continue
                }

                // Numbers
                if (ch.isDigit()) {
                    val start = i
                    while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'L' || line[i] == 'f')) {
                        i++
                    }
                    val num = line.substring(start, i)
                    pushStyle(SpanStyle(color = CodeNumber))
                    append(num)
                    pop()
                    continue
                }

                // Default characters & symbols
                pushStyle(SpanStyle(color = Color(0xFFCEC8E8)))
                append(ch)
                pop()
                i++
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
