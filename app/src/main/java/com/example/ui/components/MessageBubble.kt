package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.InfinityCyan
import com.example.ui.theme.InfinityEmerald
import com.example.ui.theme.InfinityPink
import com.example.ui.theme.InfinityViolet

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    onOpenProject: () -> Unit = {},
    onDownloadZip: () -> Unit = {},
    onShareProject: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF231B42)),
                contentAlignment = Alignment.Center
            ) {
                InfinityLogo(sizeDp = 24.dp)
            }
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // User image attachment if present
            if (isUser && !message.imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, InfinityCyan, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(message.imageUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Attached Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Message Body Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) Color(0xFF2A2054) else DarkSurface)
                    .border(1.dp, if (isUser) InfinityViolet.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = DarkTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    MarkdownContentRenderer(text = message.text)
                }
            }

            // Final Result Card if project is completed
            if (message.isFinalResult) {
                Spacer(modifier = Modifier.height(8.dp))
                FinalResultCard(
                    onOpenProject = onOpenProject,
                    onDownloadZip = onDownloadZip,
                    onShareProject = onShareProject
                )
            }
        }
    }
}

@Composable
fun FinalResultCard(
    onOpenProject: () -> Unit,
    onDownloadZip: () -> Unit,
    onShareProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, InfinityEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Project Ready",
                    tint = InfinityEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Project ready for review & export",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenProject,
                    colors = ButtonDefaults.buttonColors(containerColor = InfinityCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("open_project_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open",
                        tint = Color(0xFF0C0A14),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", color = Color(0xFF0C0A14), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDownloadZip,
                    colors = ButtonDefaults.buttonColors(containerColor = InfinityViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.1f).testTag("download_zip_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "ZIP",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download ZIP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onShareProject,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, InfinityPink),
                    modifier = Modifier.weight(0.9f).testTag("share_project_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = InfinityPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = InfinityPink, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Parses markdown segments (text and code blocks) and renders them appropriately.
 */
@Composable
fun MarkdownContentRenderer(text: String) {
    val segments = text.split("```")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEachIndexed { index, segment ->
            if (index % 2 == 1) {
                // Code block
                val firstLineBreak = segment.indexOf('\n')
                val language = if (firstLineBreak != -1) {
                    segment.substring(0, firstLineBreak).trim().ifBlank { "kotlin" }
                } else "kotlin"
                val code = if (firstLineBreak != -1) segment.substring(firstLineBreak + 1) else segment

                CodeBlockView(code = code.trimEnd(), language = language)
            } else {
                // Normal markdown/prose text
                val clean = segment.trim()
                if (clean.isNotEmpty()) {
                    Text(
                        text = clean,
                        color = DarkTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
