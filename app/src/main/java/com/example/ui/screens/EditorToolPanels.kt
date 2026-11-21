package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.media.TtsLanguageOption
import com.example.model.*
import com.example.ui.components.DurationPresetRow
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.formatDurationShort
import com.example.ui.components.formatTimecode
import com.example.ui.theme.*

@Composable
fun ClipsToolPanel(
    project: ClipProject,
    onAddLocalVideo: () -> Unit,
    onAddLocalPhoto: () -> Unit,
    onAddExtraSegment: (String, Int) -> Unit,
    onApplyProjectLength: (Int) -> Unit,
    onUpdateClipDuration: (String, Int) -> Unit,
    onMoveClip: (Int, Int) -> Unit,
    onRemoveClip: (String) -> Unit
) {
    var showSegmentDialog by remember { mutableStateOf(false) }
    var segmentTitle by remember { mutableStateOf("Bonus Viral Cut") }
    var segmentDur by remember { mutableFloatStateOf(20f) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Multi-Clip Timeline (${project.clips.size})",
            subtitle = "Total: ${formatDurationShort((project.totalDurationMs / 1000L).toInt())} · Supports 30s, 60s & 1m+"
        )

        // Add Other Clips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddLocalVideo,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_local_video_clip_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanContainer,
                    contentColor = CyanWave
                )
            ) {
                Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Video", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onAddLocalPhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_local_photo_clip_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidLimeContainer,
                    contentColor = AcidLime
                )
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Photo", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = { showSegmentDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_extra_scene_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralContainer,
                    contentColor = CoralPulse
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Scene", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Quick Total Export Length Selector (30s, 60s, 1m+, 2m+)
        Text(
            text = "Total Output Length",
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondary
        )
        DurationPresetRow(
            selectedSec = (project.totalDurationMs / 1000L).toInt(),
            onSelectSec = onApplyProjectLength
        )

        // Clips List with duration adjust, reorder, delete
        project.clips.forEachIndexed { idx, clip ->
            val durSec = (clip.effectiveDurationMs / 1000L).toInt().coerceAtLeast(5)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${idx + 1} · ${clip.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${clip.sourceType.name.replace('_', ' ')} · ${formatDurationShort(durSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanWave
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { onMoveClip(idx, -1) },
                                enabled = idx > 0
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Move clip earlier",
                                    tint = if (idx > 0) InkPrimary else InkMuted
                                )
                            }
                            IconButton(
                                onClick = { onMoveClip(idx, 1) },
                                enabled = idx < project.clips.lastIndex
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Move clip later",
                                    tint = if (idx < project.clips.lastIndex) InkPrimary else InkMuted
                                )
                            }
                            IconButton(
                                onClick = { onRemoveClip(clip.id) },
                                enabled = project.clips.size > 1
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove clip",
                                    tint = if (project.clips.size > 1) ErrorCrimson else InkMuted
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Length:", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                        Slider(
                            value = durSec.toFloat(),
                            onValueChange = { onUpdateClipDuration(clip.id, it.toInt()) },
                            valueRange = 5f..180f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AcidLime,
                                activeTrackColor = AcidLime
                            )
                        )
                        Text(
                            text = formatDurationShort(durSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime
                        )
                    }
                }
            }
        }
    }

    if (showSegmentDialog) {
        AlertDialog(
            onDismissRequest = { showSegmentDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Add Bonus Clip Scene", color = InkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = segmentTitle,
                        onValueChange = { segmentTitle = it },
                        label = { Text("Scene Title") },
                        singleLine = true
                    )
                    Text(
                        text = "Duration: ${segmentDur.toInt()}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcidLime
                    )
                    Slider(
                        value = segmentDur,
                        onValueChange = { segmentDur = it },
                        valueRange = 10f..90f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddExtraSegment(segmentTitle, segmentDur.toInt())
                        showSegmentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPulse)
                ) { Text("Add Scene") }
            },
            dismissButton = {
                TextButton(onClick = { showSegmentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CaptionsToolPanel(
    project: ClipProject,
    isOnline: Boolean,
    isGenerating: Boolean,
    onToggleAutoCaptions: (Boolean) -> Unit,
    onStyleChange: (SubtitleStylePreset) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onYPosChange: (Float) -> Unit,
    onAutoGenerate: (String?) -> Unit,
    onAddCue: (String) -> Unit,
    onUpdateCue: (String, String, Long, Long) -> Unit,
    onDeleteCue: (String) -> Unit
) {
    var offlineScriptInput by remember { mutableStateOf("") }
    var newCueText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Automatic Captions",
            subtitle = if (isOnline) "AI-synced word-highlight subtitles" else "Offline timed subtitle builder",
            trailing = {
                Switch(
                    checked = project.autoCaptionsEnabled,
                    onCheckedChange = onToggleAutoCaptions,
                    modifier = Modifier.testTag("auto_captions_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnAcidLime,
                        checkedTrackColor = AcidLime
                    )
                )
            }
        )

        Button(
            onClick = { onAutoGenerate(offlineScriptInput.ifBlank { null }) },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_auto_captions_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralPulse, contentColor = OnCoral)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = OnCoral)
                Spacer(Modifier.width(8.dp))
                Text("Generating Auto-Captions…")
            } else {
                Icon(Icons.Rounded.ClosedCaption, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isOnline && offlineScriptInput.isBlank()) "Auto-Generate Captions (AI)"
                    else "Sync Captions Offline"
                )
            }
        }

        // Offline Script-to-Captions Builder
        OutlinedTextField(
            value = offlineScriptInput,
            onValueChange = { offlineScriptInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Optional Script (Auto-Splits Offline)") },
            placeholder = { Text("Paste any Hindi/English script to time automatically…", color = InkMuted) },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        // Subtitle Style Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubtitleStylePreset.entries.forEach { style ->
                val selected = project.subtitleStyle == style
                FilterChip(
                    selected = selected,
                    onClick = { onStyleChange(style) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    label = { Text(style.displayName, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        selectedContainerColor = AcidLimeContainer,
                        selectedLabelColor = AcidLime
                    )
                )
            }
        }

        // Size & Vertical Placement Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Size: ${project.subtitleFontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                Slider(
                    value = project.subtitleFontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..32f
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Height: ${(project.subtitleYFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                Slider(
                    value = project.subtitleYFraction,
                    onValueChange = onYPosChange,
                    valueRange = 0.2f..0.88f
                )
            }
        }

        // Quick Add Cue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCueText,
                onValueChange = { newCueText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add caption at playhead…", color = InkMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            IconButton(
                onClick = {
                    if (newCueText.isNotBlank()) {
                        onAddCue(newCueText.trim())
                        newCueText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AcidLime)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add subtitle cue", tint = OnAcidLime)
            }
        }

        // Editable Cue List
        project.subtitles.forEach { cue ->
            var draftText by remember(cue.id, cue.text) { mutableStateOf(cue.text) }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTimecode(cue.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave
                    )
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = {
                            draftText = it
                            onUpdateCue(cue.id, it, cue.startMs, cue.endMs)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AcidLime,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { onDeleteCue(cue.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Delete cue", tint = InkMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun TextOverlayToolPanel(
    project: ClipProject,
    onAddOverlay: (String, Int, Int, OverlayFontStyle) -> Unit,
    onUpdateOverlay: (TextOverlayItem) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    var draftText by remember { mutableStateOf("") }
    var selectedFont by remember { mutableStateOf(OverlayFontStyle.BOLD_DISPLAY) }
    var selectedTextColor by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var selectedBgColor by remember { mutableIntStateOf(0xCCFF3D5A.toInt()) }

    val colorSwatches = listOf(
        0xFFFFFFFF.toInt(),
        0xFFC8FF2E.toInt(),
        0xFFFF3D5A.toInt(),
        0xFF2EE6FF.toInt(),
        0xFFFFD83B.toInt(),
        0xFF0B0C10.toInt()
    )
    val bgSwatches = listOf(
        0xCCFF3D5A.toInt(),
        0xCC0B0C10.toInt(),
        0xCC2EE6FF.toInt(),
        0xCCC8FF2E.toInt(),
        0x00000000
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Custom Text Overlays",
            subtitle = "Add badges or headlines — drag directly on video preview to position"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_text_overlay_input"),
                placeholder = { Text("Enter text overlay (e.g. PART 1)…", color = InkMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    onAddOverlay(draftText.ifBlank { "MUST WATCH" }, selectedTextColor, selectedBgColor, selectedFont)
                    draftText = ""
                },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("add_text_overlay_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPulse)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Add")
            }
        }

        // Font Style Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayFontStyle.entries.forEach { style ->
                FilterChip(
                    selected = selectedFont == style,
                    onClick = { selectedFont = style },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    label = { Text(style.displayName) }
                )
            }
        }

        // Swatches Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ink:", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                colorSwatches.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selectedTextColor == argb) 2.5.dp else 1.dp,
                                color = if (selectedTextColor == argb) AcidLime else OutlineHairline,
                                shape = CircleShape
                            )
                            .clickable { selectedTextColor = argb }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tag:", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                bgSwatches.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (argb == 0) SurfaceDark else Color(argb))
                            .border(
                                width = if (selectedBgColor == argb) 2.5.dp else 1.dp,
                                color = if (selectedBgColor == argb) CyanWave else OutlineHairline,
                                shape = CircleShape
                            )
                            .clickable { selectedBgColor = argb },
                        contentAlignment = Alignment.Center
                    ) {
                        if (argb == 0) {
                            Icon(Icons.Rounded.Block, contentDescription = "No background", tint = InkMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Active Overlays List
        project.textOverlays.forEach { overlay ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = overlay.text,
                            onValueChange = { onUpdateOverlay(overlay.copy(text = it)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { onDeleteOverlay(overlay.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove text overlay", tint = ErrorCrimson)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Size: ${overlay.fontSizeSp.toInt()}", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                        Slider(
                            value = overlay.fontSizeSp,
                            onValueChange = { onUpdateOverlay(overlay.copy(fontSizeSp = it)) },
                            valueRange = 14f..42f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TtsVoiceoverToolPanel(
    ttsSettings: TtsSettings,
    languages: List<TtsLanguageOption>,
    isPreviewing: Boolean,
    onUpdateTts: ((TtsSettings) -> TtsSettings) -> Unit,
    onFillFromSubtitles: () -> Unit,
    onPreviewTts: () -> Unit,
    onStopTts: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Text-to-Speech Voiceover",
            subtitle = "100% Offline on-device narration mixed into exported MP4",
            trailing = {
                Switch(
                    checked = ttsSettings.enabled,
                    onCheckedChange = { enabled -> onUpdateTts { it.copy(enabled = enabled) } },
                    modifier = Modifier.testTag("tts_enable_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnCyan,
                        checkedTrackColor = CyanWave
                    )
                )
            }
        )

        OutlinedTextField(
            value = ttsSettings.script,
            onValueChange = { txt -> onUpdateTts { it.copy(script = txt, enabled = true) } },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .testTag("tts_script_input"),
            label = { Text("Voiceover Script (English / Hindi)") },
            placeholder = { Text("Type narration to convert to speech…", color = InkMuted) },
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onFillFromSubtitles,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("tts_fill_from_captions_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AcidLime)
            ) {
                Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Use Captions")
            }

            Button(
                onClick = { if (isPreviewing) onStopTts() else onPreviewTts() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("tts_preview_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPreviewing) ErrorCrimson else CyanWave,
                    contentColor = if (isPreviewing) OnCoral else OnCyan
                )
            ) {
                Icon(
                    if (isPreviewing) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isPreviewing) "Stop Voice" else "Speak Preview", fontWeight = FontWeight.Bold)
            }
        }

        // Voice Language Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { lang ->
                val selected = ttsSettings.languageTag == lang.tag
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateTts { it.copy(languageTag = lang.tag) } },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    label = { Text(lang.label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        selectedContainerColor = CyanContainer,
                        selectedLabelColor = CyanWave
                    )
                )
            }
        }

        // Speed, Pitch, Volume Sliders
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Speech Speed", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${"%.2f".format(ttsSettings.speechRate)}x", style = MaterialTheme.typography.labelSmall, color = CyanWave)
            }
            Slider(
                value = ttsSettings.speechRate,
                onValueChange = { r -> onUpdateTts { it.copy(speechRate = r) } },
                valueRange = 0.6f..1.8f
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Voice Pitch", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${"%.2f".format(ttsSettings.pitch)}x", style = MaterialTheme.typography.labelSmall, color = CyanWave)
            }
            Slider(
                value = ttsSettings.pitch,
                onValueChange = { p -> onUpdateTts { it.copy(pitch = p) } },
                valueRange = 0.6f..1.6f
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Voiceover Volume", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${(ttsSettings.volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AcidLime)
            }
            Slider(
                value = ttsSettings.volume,
                onValueChange = { v -> onUpdateTts { it.copy(volume = v) } },
                valueRange = 0.1f..1.0f
            )
        }
    }
}
