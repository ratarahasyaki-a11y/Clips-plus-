package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.QuickShareTarget
import com.example.model.ClipProject
import com.example.model.SocialCaptionBundle
import com.example.ui.components.NetworkModeBadge
import com.example.ui.components.ViralCaptionStudioContent
import com.example.ui.theme.*
import com.example.viewmodel.SocialCaptionUiState

@Composable
fun SocialCaptionsScreen(
    project: ClipProject,
    savedProjects: List<ClipProject>,
    isOnline: Boolean,
    isRegenerating: Boolean,
    captionState: SocialCaptionUiState,
    onSelectProject: (ClipProject) -> Unit,
    onUpdateBundle: (SocialCaptionBundle) -> Unit,
    onRegenerateAi: (tone: String, extraContext: String) -> Unit,
    onDismissError: () -> Unit,
    onQuickShareText: (String) -> Unit,
    onQuickShareTarget: (QuickShareTarget, String) -> Unit,
    onCopiedFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.viral_ai_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary
                )
                Text(
                    text = "AI captions & hashtags tuned to your video content",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
            NetworkModeBadge(isOnline = isOnline)
        }

        // Video Project Switcher Strip (if multiple saved videos exist)
        if (savedProjects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select Video Project",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedProjects.take(12).forEachIndexed { idx, item ->
                        val selected = item.id == project.id
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .widthIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectProject(item) }
                                .testTag("caption_project_chip_$idx"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) AcidLimeContainer else SurfaceCard,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) AcidLime else OutlineHairline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VideoLibrary,
                                    contentDescription = null,
                                    tint = if (selected) AcidLime else InkSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) AcidLime else InkPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        ViralCaptionStudioContent(
            project = project,
            isOnline = isOnline,
            isRegenerating = isRegenerating,
            captionState = captionState,
            onUpdateBundle = onUpdateBundle,
            onGenerateAi = onRegenerateAi,
            onDismissError = onDismissError,
            onQuickShareText = onQuickShareText,
            onQuickShareTarget = onQuickShareTarget,
            onCopiedFeedback = onCopiedFeedback,
            compactMode = false
        )
    }
}
