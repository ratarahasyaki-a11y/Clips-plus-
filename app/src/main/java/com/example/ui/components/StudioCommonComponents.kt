package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.QuickShareTarget
import com.example.model.AspectRatioPreset
import com.example.ui.theme.*

fun formatTimecode(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = (safe % 1000L) / 100L
    return "%02d:%02d.%d".format(mins, secs, tenths)
}

fun formatDurationShort(sec: Int): String {
    return if (sec < 60) {
        "${sec}s"
    } else {
        val m = sec / 60
        val s = sec % 60
        if (s == 0) "${m}m" else "${m}m ${s}s"
    }
}

@Composable
fun NetworkModeBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isOnline) CyanContainer.copy(alpha = 0.85f) else SurfaceElevated
    val fgColor = if (isOnline) CyanWave else AmberGold
    val dotColor = if (isOnline) AcidLime else AmberGold

    Surface(
        modifier = modifier.testTag("network_mode_badge"),
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Icon(
                imageVector = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                contentDescription = if (isOnline) "Online Gemini AI mode" else "Offline studio mode",
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isOnline) "Online · AI Ready" else "Offline Studio",
                style = MaterialTheme.typography.labelSmall,
                color = fgColor
            )
        }
    }
}

@Composable
fun AspectRatioChipRow(
    selected: AspectRatioPreset,
    onSelect: (AspectRatioPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AspectRatioPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(preset) }
                    .testTag("aspect_ratio_${preset.name.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) CoralContainer else SurfaceElevated,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) CoralPulse else OutlineHairline
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) InkPrimary else InkSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) CoralPulse else InkMuted
                    )
                }
            }
        }
    }
}

@Composable
fun DurationPresetRow(
    selectedSec: Int,
    onSelectSec: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        30 to "30s",
        45 to "45s",
        60 to "60s",
        75 to "1m 15s",
        90 to "1m 30s",
        120 to "2m+"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (sec, label) ->
            val isSelected = selectedSec == sec
            FilterChip(
                selected = isSelected,
                onClick = { onSelectSec(sec) },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("duration_preset_$sec"),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceElevated,
                    labelColor = InkSecondary,
                    selectedContainerColor = AcidLimeContainer,
                    selectedLabelColor = AcidLime
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = OutlineHairline,
                    selectedBorderColor = AcidLime
                )
            )
        }
    }
}

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun QuickShareTargetStrip(
    onShareTarget: (QuickShareTarget) -> Unit,
    modifier: Modifier = Modifier,
    includeMoreOption: Boolean = true
) {
    val targets = if (includeMoreOption) {
        QuickShareTarget.entries
    } else {
        listOf(
            QuickShareTarget.WHATSAPP,
            QuickShareTarget.INSTAGRAM_REELS,
            QuickShareTarget.YOUTUBE_SHORTS
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        targets.forEach { target ->
            val accent = Color(target.accentHex)
            val icon = when (target) {
                QuickShareTarget.WHATSAPP -> Icons.Rounded.Chat
                QuickShareTarget.INSTAGRAM_REELS -> Icons.Rounded.MovieFilter
                QuickShareTarget.YOUTUBE_SHORTS -> Icons.Rounded.PlayCircle
                QuickShareTarget.SYSTEM_MORE -> Icons.Rounded.Share
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShareTarget(target) }
                    .testTag("quick_share_${target.name.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Share to ${target.displayName}",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = target.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
