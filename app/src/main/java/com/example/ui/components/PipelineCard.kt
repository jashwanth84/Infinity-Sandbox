package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AgentType
import com.example.domain.model.AgentExecutionState
import com.example.domain.model.AgentStepStatus
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.InfinityCyan
import com.example.ui.theme.InfinityEmerald
import com.example.ui.theme.InfinityPink
import com.example.ui.theme.InfinityRed
import com.example.ui.theme.InfinityViolet

@Composable
fun PipelineCard(
    agentStates: Map<AgentType, AgentExecutionState>,
    modifier: Modifier = Modifier
) {
    val expandedStates = remember { mutableStateMapOf<AgentType, Boolean>() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Multi-agent Pipeline",
                    tint = InfinityCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Multi-Agent AI Pipeline",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                val completedCount = agentStates.values.count { it.status == AgentStepStatus.DONE }
                Text(
                    text = "$completedCount/${agentStates.size} completed",
                    color = InfinityCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Rows for each agent
            agentStates.forEach { (type, state) ->
                val isExpanded = expandedStates[type] ?: false
                AgentRow(
                    type = type,
                    state = state,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedStates[type] = !isExpanded }
                )
            }
        }
    }
}

@Composable
private fun AgentRow(
    type: AgentType,
    state: AgentExecutionState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val agentIcon = when (type) {
        AgentType.ORCHESTRATOR -> Icons.Default.Psychology
        AgentType.RESEARCHER -> Icons.Default.Search
        AgentType.VISION -> Icons.Default.Visibility
        AgentType.CODER -> Icons.Default.Code
        AgentType.DEBUGGER -> Icons.Default.BugReport
        AgentType.HELPER -> Icons.Default.Lightbulb
        AgentType.MERGER -> Icons.AutoMirrored.Filled.MergeType
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isExpanded) DarkSurfaceVariant else Color.Transparent)
            .clickable { onToggleExpand() }
            .padding(8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF221A3D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = agentIcon,
                    contentDescription = type.displayName,
                    tint = InfinityCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = type.description,
                    color = Color(0xFFA59EC4),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // Status Badge & Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.elapsedMs > 0) {
                    val sec = String.format("%.1fs", state.elapsedMs / 1000f)
                    Text(
                        text = sec,
                        color = Color(0xFF8E86B0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                when (state.status) {
                    AgentStepStatus.WAITING -> {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Waiting",
                            tint = Color(0xFF6B648A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AgentStepStatus.RUNNING -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = InfinityCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AgentStepStatus.DONE -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = InfinityEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AgentStepStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Failed",
                            tint = InfinityRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle output",
                    tint = Color(0xFFA59EC4),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Output View
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Thinking block
                if (state.liveThinking.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D1735))
                            .border(1.dp, InfinityViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Thinking",
                                    tint = InfinityViolet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Model Thinking",
                                    color = InfinityViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())) {
                                Text(
                                    text = state.liveThinking,
                                    color = Color(0xFFD4CEF2),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Live Content block
                if (state.liveContent.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F0C1B))
                            .padding(8.dp)
                            .heightIn(max = 150.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = state.liveContent,
                            color = Color(0xFFF2F0FA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                } else if (state.errorMessage != null) {
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = InfinityRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    Text(
                        text = if (state.status == AgentStepStatus.RUNNING) "Thinking and streaming tokens..." else "Awaiting execution...",
                        color = Color(0xFF8E86B0),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}
