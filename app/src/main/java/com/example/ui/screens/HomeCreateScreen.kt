package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AspectRatioPreset
import com.example.model.ViralClipCandidate
import com.example.model.YouTubeVideoInfo
import com.example.ui.components.AspectRatioChipRow
import com.example.ui.components.DurationPresetRow
import com.example.ui.components.NetworkModeBadge
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import com.example.viewmodel.AnalysisUiState
import com.example.viewmodel.CloudSyncUiState

@Composable
fun HomeCreateScreen(
    isOnline: Boolean,
    userDisplayName: String,
    cloudSyncState: CloudSyncUiState,
    youtubeUrl: String,
    targetDurationSec: Int,
    selectedAspectRatio: AspectRatioPreset,
    captionLanguage: String,
    analysisState: AnalysisUiState,
    onYoutubeUrlChange: (String) -> Unit,
    onTargetDurationChange: (Int) -> Unit,
    onAspectRatioChange: (AspectRatioPreset) -> Unit,
    onCaptionLanguageChange: (String) -> Unit,
    onExtractViralClips: () -> Unit,
    onPickLocalVideo: () -> Unit,
    onPickLocalImage: () -> Unit,
    onOpenCandidate: (YouTubeVideoInfo, ViralClipCandidate) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Atmospheric Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = stringResource(R.string.cd_hero_art),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.25f),
                                    ObsidianBg.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AcidLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "CLIPPULSE",
                                style = MaterialTheme.typography.labelLarge,
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NetworkModeBadge(isOnline = isOnline)
                            Surface(
                                onClick = { showAccountDialog = true },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("account_sync_button"),
                                shape = RoundedCornerShape(999.dp),
                                color = SurfaceElevated.copy(alpha = 0.9f),
                                border = BorderStroke(
                                    1.dp,
                                    when (cloudSyncState) {
                                        is CloudSyncUiState.Synced -> AcidLime.copy(alpha = 0.6f)
                                        is CloudSyncUiState.Error -> CoralPulse.copy(alpha = 0.7f)
                                        CloudSyncUiState.Loading -> CyanWave.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = when (cloudSyncState) {
                                            is CloudSyncUiState.Synced -> Icons.Rounded.CloudDone
                                            is CloudSyncUiState.Error -> Icons.Rounded.CloudOff
                                            CloudSyncUiState.Loading -> Icons.Rounded.CloudSync
                                        },
                                        contentDescription = stringResource(R.string.cd_account_menu),
                                        tint = when (cloudSyncState) {
                                            is CloudSyncUiState.Synced -> AcidLime
                                            is CloudSyncUiState.Error -> CoralPulse
                                            CloudSyncUiState.Loading -> CyanWave
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = userDisplayName.substringBefore(" ").take(10).ifBlank { "Account" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.hero_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = stringResource(R.string.hero_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // YouTube Link Input + Studio Controls Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = CoralPulse
                        )
                        Text(
                            text = "YouTube Link to Viral Clips",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                    }

                    OutlinedTextField(
                        value = youtubeUrl,
                        onValueChange = onYoutubeUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_url_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.youtube_url_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPulse,
                            unfocusedBorderColor = OutlineHairline,
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceDark,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = CyanWave
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (youtubeUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onYoutubeUrlChange("") },
                                        modifier = Modifier.testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.cd_clear_link),
                                            tint = InkSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            onYoutubeUrlChange(clip.trim())
                                        }
                                    },
                                    modifier = Modifier.testTag("paste_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = stringResource(R.string.cd_paste_link),
                                        tint = AcidLime
                                    )
                                }
                            }
                        }
                    )

                    // Aspect Ratio Selector (19:6, 9:16, 16:9, 1:1)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Export Aspect Ratio",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                        AspectRatioChipRow(
                            selected = selectedAspectRatio,
                            onSelect = onAspectRatioChange
                        )
                    }

                    // Target Clip Length (30s, 60s, 1 min+ up to 3 min)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clip Length (30s – 60s – 1m+)",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            Text(
                                text = formatDurationShort(targetDurationSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = AcidLime
                            )
                        }
                        DurationPresetRow(
                            selectedSec = targetDurationSec,
                            onSelectSec = onTargetDurationChange
                        )
                        Slider(
                            value = targetDurationSec.toFloat(),
                            onValueChange = { onTargetDurationChange(it.toInt()) },
                            valueRange = 15f..180f,
                            modifier = Modifier.testTag("duration_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = AcidLime,
                                activeTrackColor = AcidLime,
                                inactiveTrackColor = SurfaceBright
                            )
                        )
                    }

                    // Caption Language Preference
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("English / Hinglish", "Hindi", "English").forEach { lang ->
                            val selected = captionLanguage == lang
                            FilterChip(
                                selected = selected,
                                onClick = { onCaptionLanguageChange(lang) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = {
                                    Text(
                                        text = lang,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    selectedContainerColor = CyanContainer,
                                    selectedLabelColor = CyanWave,
                                    labelColor = InkSecondary
                                )
                            )
                        }
                    }

                    // Primary Extract CTA
                    Button(
                        onClick = onExtractViralClips,
                        enabled = analysisState !is AnalysisUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("extract_viral_clips_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPulse,
                            contentColor = OnCoral
                        )
                    ) {
                        if (analysisState is AnalysisUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = OnCoral
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = analysisState.statusMessage,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) {
                                    stringResource(R.string.action_extract_clips)
                                } else {
                                    "Create Offline Cut Draft"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Offline & Local Direct Studio Import Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = CyanWave
                        )
                        Column {
                            Text(
                                text = "Offline Local Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Text(
                                text = "Edit device videos or photos offline with captions, TTS, filters, effects & music.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickLocalVideo,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_local_video_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import Video", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onPickLocalImage,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_local_photo_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AcidLime)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import Photo", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Error State Card
        if (analysisState is AnalysisUiState.Error) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_error_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = CoralContainer,
                    border = BorderStroke(1.dp, ErrorCrimson)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorCrimson
                        )
                        Text(
                            text = analysisState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Viral Clip Results List
        if (analysisState is AnalysisUiState.Success) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (analysisState.offlineGenerated) "Offline Clip Draft" else "Top Viral Clips Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = "${analysisState.videoInfo.title} · ${analysisState.videoInfo.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AcidLimeContainer
                    ) {
                        Text(
                            text = "${analysisState.candidates.size} CUTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(analysisState.candidates, key = { it.id }) { candidate ->
                ViralCandidateCard(
                    videoInfo = analysisState.videoInfo,
                    candidate = candidate,
                    aspectLabel = selectedAspectRatio.label,
                    onEditClick = { onOpenCandidate(analysisState.videoInfo, candidate) }
                )
            }
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Creator Account & Cloud Sync",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Signed in as: $userDisplayName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = when (cloudSyncState) {
                            is CloudSyncUiState.Synced -> "Firebase Cloud Firestore synced (${cloudSyncState.count} cloud projects)."
                            is CloudSyncUiState.Loading -> "Syncing your studio projects with Firebase Cloud…"
                            is CloudSyncUiState.Error -> "Offline / Sync notice: ${cloudSyncState.message}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("sign_out_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPulse, contentColor = OnCoral)
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun ViralCandidateCard(
    videoInfo: YouTubeVideoInfo,
    candidate: ViralClipCandidate,
    aspectLabel: String,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("viral_candidate_card"),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineHairline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 108.dp, height = 74.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevated)
                ) {
                    AsyncImage(
                        model = videoInfo.thumbnailUrl,
                        contentDescription = candidate.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianBg.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = formatDurationShort(candidate.durationSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AcidLimeContainer
                        ) {
                            Text(
                                text = "${candidate.viralScore}% VIRAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = AcidLime,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceElevated
                        ) {
                            Text(
                                text = "${candidate.startTimeLabel}–${candidate.endTimeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanWave,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = candidate.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = candidate.hookReason,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )

            AnimatedVisibility(visible = candidate.subtitles.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = null,
                            tint = AcidLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "\"${candidate.subtitles.firstOrNull()?.text.orEmpty()}\" · ${candidate.subtitles.size} auto-captions ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Button(
                onClick = onEditClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_candidate_editor_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceElevated,
                    contentColor = InkPrimary
                )
            ) {
                Text(
                    text = "Open in Studio Editor ($aspectLabel)",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = CoralPulse,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
