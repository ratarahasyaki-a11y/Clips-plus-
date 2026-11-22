package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.media.QuickShareCoordinator
import com.example.media.QuickShareTarget
import com.example.model.ClipProject
import com.example.model.SocialCaptionBundle
import com.example.model.SocialPlatformTab
import com.example.model.ViralCaptionTone
import com.example.ui.theme.*
import com.example.viewmodel.SocialCaptionUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViralCaptionStudioContent(
    project: ClipProject,
    isOnline: Boolean,
    isRegenerating: Boolean,
    captionState: SocialCaptionUiState,
    onUpdateBundle: (SocialCaptionBundle) -> Unit,
    onGenerateAi: (tone: String, extraContext: String) -> Unit,
    onDismissError: () -> Unit,
    onQuickShareText: (String) -> Unit,
    onQuickShareTarget: (QuickShareTarget, String) -> Unit,
    onCopiedFeedback: (String) -> Unit,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bundle = project.socialCaptions

    var selectedPlatform by remember { mutableStateOf(SocialPlatformTab.INSTAGRAM) }
    var selectedTone by remember(bundle.generatedTone) {
        mutableStateOf(
            ViralCaptionTone.entries.firstOrNull { it.label.equals(bundle.generatedTone, ignoreCase = true) }
                ?: ViralCaptionTone.VIRAL_PUNCHY
        )
    }
    var extraContextHint by remember(project.id) { mutableStateOf("") }
    var customTagInput by remember { mutableStateOf("") }
    var excludedTags by remember(project.id, bundle.hashtags) { mutableStateOf(setOf<String>()) }
    var recentlyCopiedKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recentlyCopiedKey) {
        if (recentlyCopiedKey != null) {
            delay(1_800L)
            recentlyCopiedKey = null
        }
    }

    val selectedHashtags = remember(bundle.hashtags, excludedTags) {
        bundle.hashtags.filterNot { it in excludedTags }
    }
    val activeCaption = selectedPlatform.captionFrom(bundle)
    val fullSharePayload = bundle.buildFullPost(
        platform = selectedPlatform,
        fallbackTitle = project.title,
        selectedHashtags = selectedHashtags
    )
    val captionOnlyPayload = bundle.buildCaptionOnly(
        platform = selectedPlatform,
        fallbackTitle = project.title
    )
    val hashtagsOnlyPayload = selectedHashtags.joinToString(" ")

    val previewThumb = project.clips.firstOrNull()?.thumbnailUrl?.ifBlank { project.coverThumbnailUrl }
        ?: project.coverThumbnailUrl

    fun copyAndConfirm(key: String, label: String, text: String, feedback: String) {
        if (text.isBlank()) {
            onCopiedFeedback("Nothing to copy yet — generate or type a caption first.")
            return
        }
        QuickShareCoordinator.copyToClipboard(context, label, text)
        recentlyCopiedKey = key
        onCopiedFeedback(feedback)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactMode) 12.dp else 14.dp)
    ) {
        // 1. Video Content Scan + AI Generator Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(listOf(CoralPulse.copy(alpha = 0.5f), CyanWave.copy(alpha = 0.45f)))
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Context Preview Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewThumb.isNotBlank()) {
                            AsyncImage(
                                model = previewThumb,
                                contentDescription = stringResource(R.string.cd_video_content_preview),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MovieFilter,
                                contentDescription = stringResource(R.string.cd_video_content_preview),
                                tint = CoralPulse,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianBg.copy(alpha = 0.82f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = formatDurationShort((project.totalDurationMs / 1000L).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = AcidLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = CoralPulse,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.viral_ai_card_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = project.title.ifBlank { "Untitled Video Cut" },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContentSignalChip(
                                text = "${project.clips.size.coerceAtLeast(1)} clip${if (project.clips.size == 1) "" else "s"}",
                                tint = CyanWave,
                                bg = CyanContainer
                            )
                            ContentSignalChip(
                                text = "${project.subtitles.size} subs",
                                tint = AcidLime,
                                bg = AcidLimeContainer
                            )
                            ContentSignalChip(
                                text = "Visual AI",
                                tint = CoralPulse,
                                bg = CoralContainer
                            )
                        }
                    }
                }

                // Tone / Vibe Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.viral_ai_tone_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViralCaptionTone.entries.forEach { tone ->
                            val active = selectedTone == tone
                            FilterChip(
                                selected = active,
                                onClick = { selectedTone = tone },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("viral_tone_${tone.name.lowercase()}"),
                                label = { Text(tone.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    selectedContainerColor = CoralContainer,
                                    selectedLabelColor = CoralPulse,
                                    labelColor = InkSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = active,
                                    borderColor = OutlineHairline,
                                    selectedBorderColor = CoralPulse.copy(alpha = 0.75f)
                                )
                            )
                        }
                    }
                }

                // Optional Creator Focus / CTA Hint
                OutlinedTextField(
                    value = extraContextHint,
                    onValueChange = { extraContextHint = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viral_caption_hint_input"),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.viral_ai_hint_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (extraContextHint.isNotEmpty()) {
                            IconButton(onClick = { extraContextHint = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_link),
                                    tint = InkSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanWave,
                        unfocusedBorderColor = OutlineHairline,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )

                // Primary AI Generate Button
                Button(
                    onClick = { onGenerateAi(selectedTone.label, extraContextHint) },
                    enabled = !isRegenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_viral_captions_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPulse,
                        contentColor = OnCoral,
                        disabledContainerColor = CoralContainer,
                        disabledContentColor = CoralPulse
                    )
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CoralPulse
                        )
                        Spacer(Modifier.width(10.dp))
                        val statusLabel = (captionState as? SocialCaptionUiState.Analyzing)?.statusMessage
                            ?: stringResource(R.string.viral_ai_generating_default)
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) {
                                stringResource(R.string.action_generate_viral_captions)
                            } else {
                                stringResource(R.string.action_offline_edit_captions)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Transparent Error State Banner (with Retry)
                AnimatedVisibility(
                    visible = captionState is SocialCaptionUiState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val errMessage = (captionState as? SocialCaptionUiState.Error)?.message.orEmpty()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viral_caption_error_card"),
                        shape = RoundedCornerShape(14.dp),
                        color = ErrorCrimson.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onDismissError) {
                                    Text("Dismiss", color = InkSecondary)
                                }
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = { onGenerateAi(selectedTone.label, extraContextHint) },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .testTag("retry_viral_captions_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ErrorCrimson,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // AI Video Content Insight Banner
                if (bundle.visualInsight.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyanContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = CyanWave,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                val badgeSuffix = (captionState as? SocialCaptionUiState.Success)
                                    ?.let { " · ${it.sourceSummary}" }
                                    .orEmpty()
                                Text(
                                    text = "AI VIDEO CONTENT INSIGHT$badgeSuffix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanWave,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bundle.visualInsight,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 3-Second Viral Hook Picker Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AcidLimeContainer.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = AcidLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.viral_ai_hook_header),
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                copyAndConfirm(
                                    key = "hook",
                                    label = "Viral Hook",
                                    text = bundle.viralHook,
                                    feedback = "Viral hook copied!"
                                )
                            }
                            .testTag("copy_hook_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianBg.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (recentlyCopiedKey == "hook") Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy_hook),
                                tint = AcidLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (recentlyCopiedKey == "hook") "Copied" else "Copy Hook",
                                style = MaterialTheme.typography.labelSmall,
                                color = AcidLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Hook Variation Selector Chips (when AI generated multiple hook ideas)
                if (bundle.alternateHooks.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bundle.alternateHooks.forEachIndexed { idx, hookOption ->
                            val isSelectedHook = bundle.viralHook.trim() == hookOption.trim()
                            FilterChip(
                                selected = isSelectedHook,
                                onClick = { onUpdateBundle(bundle.copy(viralHook = hookOption)) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("viral_hook_option_$idx"),
                                label = {
                                    Text(
                                        text = "Hook #${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ObsidianBg.copy(alpha = 0.45f),
                                    selectedContainerColor = AcidLime,
                                    selectedLabelColor = ObsidianBg,
                                    labelColor = InkPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bundle.viralHook,
                    onValueChange = { onUpdateBundle(bundle.copy(viralHook = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viral_hook_input"),
                    placeholder = { Text("Scroll-stopping opening hook…", color = InkMuted) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AcidLime,
                        unfocusedBorderColor = OutlineHairline.copy(alpha = 0.5f),
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )
            }
        }

        // 3. Platform Caption Selector & Editor Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialPlatformTab.entries.forEach { plat ->
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
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = OutlineHairline,
                        selectedBorderColor = CyanWave.copy(alpha = 0.75f)
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, OutlineHairline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPlatform.label} Caption",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    val charCount = activeCaption.length
                    val overLimit = charCount > selectedPlatform.recommendedMaxChars
                    Text(
                        text = "$charCount / ${selectedPlatform.recommendedMaxChars}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overLimit) AmberGold else InkSecondary
                    )
                }

                OutlinedTextField(
                    value = activeCaption,
                    onValueChange = { updatedText ->
                        onUpdateBundle(selectedPlatform.updateBundle(bundle, updatedText))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp)
                        .testTag("social_caption_editor"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.viral_ai_caption_placeholder),
                            color = InkMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanWave,
                        unfocusedBorderColor = OutlineHairline,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )

                // Copy Caption Only (Hook + Caption)
                OutlinedButton(
                    onClick = {
                        copyAndConfirm(
                            key = "caption",
                            label = "${selectedPlatform.label} Caption",
                            text = captionOnlyPayload,
                            feedback = "${selectedPlatform.shortLabel} caption copied!"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_caption_only_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                ) {
                    Icon(
                        imageVector = if (recentlyCopiedKey == "caption") Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (recentlyCopiedKey == "caption") "Caption Copied!" else "Copy Caption Only",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 4. Interactive Viral Hashtag Lab Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, OutlineHairline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.viral_ai_hashtags_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (bundle.hashtags.isEmpty()) {
                                "Tap Generate above or add custom #tags below"
                            } else {
                                "${selectedHashtags.size}/${bundle.hashtags.size} selected · Tap pills to toggle"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }

                    if (bundle.hashtags.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                excludedTags = if (selectedHashtags.size == bundle.hashtags.size) {
                                    bundle.hashtags.toSet()
                                } else {
                                    emptySet()
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (selectedHashtags.size == bundle.hashtags.size) "Clear" else "Select All",
                                color = CyanWave,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (bundle.hashtags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bundle.hashtags.forEachIndexed { idx, tag ->
                            val isIncluded = tag !in excludedTags
                            Surface(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        excludedTags = if (isIncluded) {
                                            excludedTags + tag
                                        } else {
                                            excludedTags - tag
                                        }
                                    }
                                    .testTag("hashtag_chip_$idx"),
                                shape = RoundedCornerShape(999.dp),
                                color = if (isIncluded) CyanContainer else SurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isIncluded) CyanWave.copy(alpha = 0.8f) else OutlineHairline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isIncluded) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = CyanWave,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isIncluded) CyanWave else InkMuted,
                                        fontWeight = if (isIncluded) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Add Custom Hashtag Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_custom_hashtag_input"),
                        singleLine = true,
                        placeholder = { Text("Add custom #tag", color = InkMuted, style = MaterialTheme.typography.bodySmall) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AcidLime,
                            unfocusedBorderColor = OutlineHairline,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        )
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val clean = customTagInput.trim().replace(" ", "")
                            if (clean.isNotEmpty() && clean != "#") {
                                val normalized = if (clean.startsWith("#")) clean else "#$clean"
                                if (normalized !in bundle.hashtags) {
                                    onUpdateBundle(bundle.copy(hashtags = bundle.hashtags + normalized))
                                }
                                excludedTags = excludedTags - normalized
                                customTagInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_custom_hashtag_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = AcidLimeContainer,
                            contentColor = AcidLime
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_add_hashtag)
                        )
                    }
                }

                // Copy Hashtags Only Button
                OutlinedButton(
                    onClick = {
                        copyAndConfirm(
                            key = "tags",
                            label = "Viral Hashtags",
                            text = hashtagsOnlyPayload,
                            feedback = "${selectedHashtags.size} hashtags copied!"
                        )
                    },
                    enabled = selectedHashtags.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_hashtags_only_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selectedHashtags.isNotEmpty()) CoralPulse.copy(alpha = 0.7f) else OutlineHairline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPulse)
                ) {
                    Icon(
                        imageVector = if (recentlyCopiedKey == "tags") Icons.Rounded.Check else Icons.Rounded.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (recentlyCopiedKey == "tags") {
                            "Hashtags Copied!"
                        } else {
                            "Copy ${selectedHashtags.size} Hashtags Only"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 5. Primary Copy Full Post + Direct Share Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            copyAndConfirm(
                                key = "full",
                                label = "${selectedPlatform.label} Viral Post",
                                text = fullSharePayload,
                                feedback = "Full post (hook + caption + ${selectedHashtags.size} #tags) copied!"
                            )
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .height(50.dp)
                            .testTag("copy_full_post_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(
                            imageVector = if (recentlyCopiedKey == "full") Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (recentlyCopiedKey == "full") "Copied!" else "Copy Full Post",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onQuickShareText(fullSharePayload) },
                        modifier = Modifier
                            .weight(0.95f)
                            .height(50.dp)
                            .testTag("quick_share_caption_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
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

@Composable
private fun ContentSignalChip(
    text: String,
    tint: Color,
    bg: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
