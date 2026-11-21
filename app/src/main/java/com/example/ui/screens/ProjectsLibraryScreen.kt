package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.media.QuickShareTarget
import com.example.model.ClipProject
import com.example.ui.components.NetworkModeBadge
import com.example.ui.components.QuickShareTargetStrip
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsLibraryScreen(
    projects: List<ClipProject>,
    isOnline: Boolean,
    onOpenProject: (ClipProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onQuickShareProject: (ClipProject, QuickShareTarget) -> Unit,
    onOpenProjectInGallery: (String) -> Unit,
    onGoToCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Projects",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary
                )
                Text(
                    text = "Stored offline on device · Always editable",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
            NetworkModeBadge(isOnline = isOnline)
        }

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_timeline),
                            contentDescription = stringResource(R.string.cd_empty_projects),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 200.dp, height = 148.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Text(
                            text = "No Clip Projects Yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = "Paste a YouTube link or import a local video to build your first viral clip.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                        Button(
                            onClick = onGoToCreate,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("empty_projects_create_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPulse,
                                contentColor = OnCoral
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Create First Clip", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onDelete = { onDeleteProject(project.id) },
                        onQuickShare = { target -> onQuickShareProject(project, target) },
                        onOpenInGallery = { uri -> onOpenProjectInGallery(uri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ClipProject,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onQuickShare: (QuickShareTarget) -> Unit,
    onOpenInGallery: (String) -> Unit
) {
    val dateText = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())
        .format(Date(project.updatedAtMs))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen() }
            .testTag("saved_project_card"),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineHairline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    val thumb = project.coverThumbnailUrl.ifBlank {
                        project.clips.firstOrNull()?.thumbnailUrl.orEmpty()
                    }
                    if (thumb.isNotBlank()) {
                        AsyncImage(
                            model = thumb,
                            contentDescription = project.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MovieCreation,
                            contentDescription = null,
                            tint = CoralPulse
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.clips.size.coerceAtLeast(1)} clips · ${project.subtitles.size} captions · $dateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBadge(project.aspectRatio.label, CoralContainer, CoralPulse)
                        MiniBadge(
                            formatDurationShort((project.totalDurationMs / 1000L).toInt()),
                            AcidLimeContainer,
                            AcidLime
                        )
                        if (project.ttsSettings.enabled) {
                            MiniBadge("TTS", CyanContainer, CyanWave)
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = InkMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceElevated,
                        contentColor = InkPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Cut", style = MaterialTheme.typography.labelMedium)
                }

                if (!project.lastExportedUri.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenInGallery(project.lastExportedUri) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("project_open_gallery_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                    ) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (!project.lastExportedUri.isNullOrBlank()) {
                QuickShareTargetStrip(
                    onShareTarget = onQuickShare,
                    includeMoreOption = true
                )
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
