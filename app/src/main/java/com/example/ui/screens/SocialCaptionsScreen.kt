package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.QuickShareTarget
import com.example.model.ClipProject
import com.example.model.SocialCaptionBundle
import com.example.ui.components.NetworkModeBadge
import com.example.ui.components.QuickShareTargetStrip
import com.example.ui.theme.*

private enum class SocialPlatform(val label: String) {
    INSTAGRAM("Instagram Reels"),
    YOUTUBE("YT Shorts"),
    TIKTOK("TikTok"),
    TWITTER("X / LinkedIn")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialCaptionsScreen(
    project: ClipProject,
    isOnline: Boolean,
    isRegenerating: Boolean,
    onUpdateBundle: (SocialCaptionBundle) -> Unit,
    onRegenerateAi: (String) -> Unit,
    onQuickShareText: (String) -> Unit,
    onQuickShareTarget: (QuickShareTarget, String) -> Unit,
    onCopiedFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var selectedPlatform by remember { mutableStateOf(SocialPlatform.INSTAGRAM) }
    var selectedTone by remember { mutableStateOf("Viral Punchy") }
    val bundle = project.socialCaptions

    val activeCaption = when (selectedPlatform) {
        SocialPlatform.INSTAGRAM -> bundle.instagramCaption
        SocialPlatform.YOUTUBE -> bundle.youtubeShortsCaption
        SocialPlatform.TIKTOK -> bundle.tiktokCaption
        SocialPlatform.TWITTER -> bundle.twitterCaption
    }

    val fullSharePayload = buildString {
        if (bundle.viralHook.isNotBlank()) {
            appendLine(bundle.viralHook)
            appendLine()
        }
        appendLine(activeCaption.ifBlank { project.title })
        if (bundle.hashtags.isNotEmpty()) {
            appendLine()
            append(bundle.hashtags.joinToString(" "))
        }
    }.trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Social Captions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary
                )
                Text(
                    text = "Ready to copy or share for ${project.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
            NetworkModeBadge(isOnline = isOnline)
        }

        // Tone & AI Regenerate Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, OutlineHairline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AI Caption Vibe",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Viral Punchy", "Hinglish Desi", "Storytelling", "Bold CTA").forEach { tone ->
                        val active = selectedTone == tone
                        FilterChip(
                            selected = active,
                            onClick = { selectedTone = tone },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(tone, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                selectedContainerColor = CoralContainer,
                                selectedLabelColor = CoralPulse,
                                labelColor = InkSecondary
                            )
                        )
                    }
                }
                Button(
                    onClick = { onRegenerateAi(selectedTone) },
                    enabled = !isRegenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("regenerate_social_captions_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPulse,
                        contentColor = OnCoral
                    )
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = OnCoral
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating with Gemini AI…")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isOnline) "Auto-Generate Social Captions" else "Offline Mode (Edit Manually Below)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Viral Hook Highlight
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AcidLimeContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "3-SECOND HOOK",
                    style = MaterialTheme.typography.labelSmall,
                    color = AcidLime,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = bundle.viralHook,
                    onValueChange = { onUpdateBundle(bundle.copy(viralHook = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Scroll-stopping opening hook…", color = InkMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AcidLime,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )
            }
        }

        // Platform Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialPlatform.entries.forEach { plat ->
                val selected = selectedPlatform == plat
                FilterChip(
                    selected = selected,
                    onClick = { selectedPlatform = plat },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("platform_tab_${plat.name.lowercase()}"),
                    label = { Text(plat.label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceCard,
                        selectedContainerColor = CyanContainer,
                        selectedLabelColor = CyanWave,
                        labelColor = InkSecondary
                    )
                )
            }
        }

        // Editable Caption Text Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, OutlineHairline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = activeCaption,
                    onValueChange = { updatedText ->
                        val next = when (selectedPlatform) {
                            SocialPlatform.INSTAGRAM -> bundle.copy(instagramCaption = updatedText)
                            SocialPlatform.YOUTUBE -> bundle.copy(youtubeShortsCaption = updatedText)
                            SocialPlatform.TIKTOK -> bundle.copy(tiktokCaption = updatedText)
                            SocialPlatform.TWITTER -> bundle.copy(twitterCaption = updatedText)
                        }
                        onUpdateBundle(next)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 124.dp)
                        .testTag("social_caption_editor"),
                    label = { Text("${selectedPlatform.label} Caption") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanWave,
                        unfocusedBorderColor = OutlineHairline,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )

                if (bundle.hashtags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bundle.hashtags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, OutlineHairline)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanWave,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(fullSharePayload))
                            onCopiedFeedback("Caption + hashtags copied!")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_caption_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcidLime)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy All")
                    }

                    Button(
                        onClick = { onQuickShareText(fullSharePayload) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_share_caption_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanWave,
                            contentColor = OnCyan
                        )
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Sheet", fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (!project.lastExportedUri.isNullOrBlank()) {
                            "Direct Share Exported Video + Caption"
                        } else {
                            "Quick Share Caption to App"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                    QuickShareTargetStrip(
                        onShareTarget = { target -> onQuickShareTarget(target, fullSharePayload) },
                        includeMoreOption = false
                    )
                }
            }
        }
    }
}
