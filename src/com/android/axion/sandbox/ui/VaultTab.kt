/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.axion.sandbox.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.axion.sandbox.R
import com.android.axion.sandbox.io.FileVaultManager
import com.android.axion.sandbox.io.VaultFile
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerPageIndex
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerSettled
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerVisiblePage
import com.android.axion.sandbox.ui.motion.miuixAlwaysVerticalStretch
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun VaultTab(
    isUnlocked: Boolean = false,
    onUnlockRequest: () -> Unit = {},
    onPickingFilesChange: (Boolean) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listOverscrollEffect: androidx.compose.foundation.OverscrollEffect? = null,
    nestInsideOuterScaffold: Boolean = false,
    gridState: LazyGridState = rememberLazyGridState(),
    useAlwaysStretch: Boolean = false,
) {
    if (!isUnlocked) {
        val lockedMod = if (listOverscrollEffect != null) {
            Modifier.fillMaxSize().miuixAlwaysVerticalStretch(listOverscrollEffect)
        } else {
            Modifier.fillMaxSize()
        }
        Box(modifier = lockedMod) {
            LockedVaultScreen(onUnlockRequest)
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultManager = remember { FileVaultManager(context) }
    var files by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshFiles() {
        scope.launch(Dispatchers.IO) {
            try {
                vaultManager.migrateLegacyIfNeeded()
                val list = vaultManager.getVaultFiles()
                withContext(Dispatchers.Main) {
                    files = list
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPickingFilesChange(false)
                refreshFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedFiles by remember { mutableStateOf(setOf<String>()) }
    val isMultiSelectMode by remember { derivedStateOf { selectedFiles.isNotEmpty() } }
    var filter by rememberSaveable { mutableStateOf("all") } // all|images|videos|files

    val rootInteractive = LocalSandboxRootInteractive.current
    val pageIndex = LocalSandboxPagerPageIndex.current
    val visiblePage = LocalSandboxPagerVisiblePage.current
    val pagerSettled = LocalSandboxPagerSettled.current
    val pageActive = pageIndex == null || pageIndex == visiblePage
    // Chi bat Back selection khi Vault active + pager settled + root interactive.
    BackHandler(enabled = rootInteractive && pageActive && pagerSettled && isMultiSelectMode) {
        selectedFiles = emptySet()
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var restoredPopupMessage by remember { mutableStateOf<String?>(null) }

    fun toggleSelection(id: String) {
        selectedFiles = if (selectedFiles.contains(id)) selectedFiles - id else selectedFiles + id
    }

    LaunchedEffect(Unit) { refreshFiles() }

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            processingMessage = context.getString(R.string.vault_importing)
            scope.launch(Dispatchers.IO) {
                try {
                    uris.forEach { vaultManager.importFile(it) }
                } finally {
                    withContext(Dispatchers.Main) { 
                        isProcessing = false
                        refreshFiles() 
                    }
                }
            }
        }
    }

    fun executeRestore() {
        isProcessing = true
        processingMessage = context.getString(R.string.vault_restoring)
        scope.launch(Dispatchers.IO) {
            try {
                val toRestore = files.filter { selectedFiles.contains(it.id) }
                val count = vaultManager.restoreFiles(toRestore)
                withContext(Dispatchers.Main) {
                    if (count == 1) {
                        val file = toRestore.first()
                        val path = if (!file.originalPath.isNullOrEmpty()) {
                            file.originalPath
                        } else {
                            "Downloads/Vault_Restored/" + file.name
                        }
                        restoredPopupMessage = context.getString(R.string.vault_restored_to, path)
                    } else if (count > 1) {
                        restoredPopupMessage = context.getString(R.string.vault_restored_count, count)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    selectedFiles = emptySet()
                    refreshFiles()
                }
            }
        }
    }

    fun executeDelete() {
        isProcessing = true
        processingMessage = context.getString(R.string.vault_deleting)
        scope.launch(Dispatchers.IO) {
            try {
                val toDelete = files.filter { selectedFiles.contains(it.id) }
                vaultManager.deleteFiles(toDelete)
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    selectedFiles = emptySet()
                    refreshFiles()
                }
            }
        }
    }

    fun handleOpenFile(file: VaultFile) {
        onPickingFilesChange(true)
        openVaultFileInternal(context, file, vaultContentUri(file))
    }

    val videos = remember(files) { files.filter { it.mimeType.startsWith("video/") } }
    val images = remember(files) { files.filter { it.mimeType.startsWith("image/") } }
    val otherFiles = remember(files) { files.filter { !it.mimeType.startsWith("image/") && !it.mimeType.startsWith("video/") } }
    val showImages = filter == "all" || filter == "images"
    val showVideos = filter == "all" || filter == "videos"
    val showFiles = filter == "all" || filter == "files"

    // nestInsideOuterScaffold: FAB/selection nam trong viewport page (khong duoi bottom nav).
    val density = LocalDensity.current
    // Do chieu cao slot bottom theo layout thuc (FAB/toolbar wrap) — tranh de len navbar.
    var bottomSlotHeight by remember { mutableStateOf(72.dp) }
    val showFab = !isMultiSelectMode && !isLoading
    LaunchedEffect(showFab, isMultiSelectMode, isLoading) {
        if (!showFab && !isMultiSelectMode) {
            bottomSlotHeight = 72.dp
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val gridBottomPad = bottomSlotHeight
        if (isLoading) {
            val loadMod = if (listOverscrollEffect != null || useAlwaysStretch) {
                Modifier.fillMaxSize().then(
                    if (listOverscrollEffect != null) {
                        Modifier.miuixAlwaysVerticalStretch(listOverscrollEffect)
                    } else Modifier,
                )
            } else Modifier.fillMaxSize()
            Box(loadMod) { VaultLoadingScreen(PaddingValues(16.dp)) }
        } else if (files.isEmpty()) {
            val emptyMod = if (listOverscrollEffect != null) {
                Modifier.fillMaxSize().miuixAlwaysVerticalStretch(listOverscrollEffect)
            } else Modifier.fillMaxSize()
            Box(emptyMod) { EmptyVaultScreen(PaddingValues(16.dp)) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                state = gridState,
                overscrollEffect = listOverscrollEffect,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = gridBottomPad + contentPadding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "filters") {
                    // FlowRow: wrap khi hep / font lon — khong clip chip.
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "all" to R.string.vault_filter_all,
                            "images" to R.string.vault_filter_images,
                            "videos" to R.string.vault_filter_videos,
                            "files" to R.string.vault_filter_files,
                        ).forEach { (id, labelRes) ->
                            FilterChip(
                                selected = filter == id,
                                onClick = { filter = id },
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }
                }
                if (showImages && images.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.vault_section_images))
                    }
                    items(images, key = { it.id }) { file ->
                        MediaGridItem(
                            file, vaultManager, selectedFiles.contains(file.id), false,
                            onClick = {
                                if (isMultiSelectMode) toggleSelection(file.id) else handleOpenFile(file)
                            },
                            onLongClick = { toggleSelection(file.id) },
                        )
                    }
                }
                if (showVideos && videos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.vault_section_videos))
                    }
                    items(videos, key = { it.id }) { file ->
                        MediaGridItem(
                            file, vaultManager, selectedFiles.contains(file.id), true,
                            onClick = {
                                if (isMultiSelectMode) toggleSelection(file.id) else handleOpenFile(file)
                            },
                            onLongClick = { toggleSelection(file.id) },
                        )
                    }
                }
                if (showFiles && otherFiles.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(R.string.vault_section_files))
                    }
                    items(otherFiles, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { file ->
                        FileListItem(
                            file, selectedFiles.contains(file.id),
                            onClick = {
                                if (isMultiSelectMode) toggleSelection(file.id) else handleOpenFile(file)
                            },
                            onLongClick = { toggleSelection(file.id) },
                        )
                    }
                }
            }
        }

        // Slot do chung FAB/toolbar — onSizeChanged theo entry/exit/wrap.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize()
                .onSizeChanged { size ->
                    val measured = with(density) { size.height.toDp() }
                    bottomSlotHeight = if (measured > 0.dp) measured else 72.dp
                },
        ) {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onPickingFilesChange(true)
                        pickFilesLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.padding(16.dp),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.action_import_vault)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                )
            }

            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Hai tang / scroll ngang: Close+count + action khong clip o font 1.5x+.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(onClick = { selectedFiles = emptySet() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_cancel),
                                )
                            }
                            Text(
                                stringResource(R.string.vault_selected_count, selectedFiles.size),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        ) {
                            FilledTonalButton(onClick = { showRestoreConfirm = true }) {
                                Text(stringResource(R.string.action_restore), maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Text(stringResource(R.string.action_delete), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm || showRestoreConfirm) {
        val isDelete = showDeleteConfirm
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; showRestoreConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (isDelete) stringResource(R.string.action_delete) else stringResource(R.string.action_restore)) },
            text = {
                Text(
                    if (isDelete) stringResource(R.string.vault_delete_multi_confirm, selectedFiles.size)
                    else stringResource(R.string.vault_restore_confirm, selectedFiles.size)
                )
            },
            confirmButton = {
                TextButton(onClick = { if (isDelete) executeDelete() else executeRestore(); showDeleteConfirm = false; showRestoreConfirm = false }) {
                    Text(
                        if (isDelete) stringResource(R.string.action_delete) else stringResource(R.string.action_restore),
                        color = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false; showRestoreConfirm = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(min = 240.dp)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = processingMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    restoredPopupMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(4000)
            restoredPopupMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.95f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun VaultLoadingScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(file: VaultFile, vaultManager: FileVaultManager, isSelected: Boolean, isVideo: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    var bitmap by remember(file.id) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(file.id) {
        if (bitmap == null) {
            withContext(Dispatchers.IO) {
                val decoded = if (isVideo) vaultManager.decryptToVideoThumbnail(file) 
                              else vaultManager.decryptToBitmap(file)
                withContext(Dispatchers.Main) { bitmap = decoded }
            }
        }
    }

    Box(modifier = Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.large).combinedClickable(onClick = onClick, onLongClick = onLongClick).background(MaterialTheme.colorScheme.surfaceBright)) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(if (isVideo) Icons.Default.VideoFile else Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.Center))
        }
        if (isVideo) Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp).align(Alignment.Center))
        if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(24.dp).background(Color.White, CircleShape).clip(CircleShape).align(Alignment.TopEnd))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(file: VaultFile, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.large) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when {
                file.mimeType.startsWith("video/") -> Icons.Default.VideoFile
                file.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
                file.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                file.mimeType == "application/vnd.android.package-archive" -> Icons.Default.Android
                file.mimeType.contains("zip") -> Icons.Default.FolderZip
                else -> Icons.Default.InsertDriveFile
            }
            Box(modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatFileSize(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
    }
}

private fun vaultContentUri(file: VaultFile): Uri =
    Uri.parse("content://$VAULT_AUTHORITY/${file.id}")

private fun openVaultFileInternal(context: Context, file: VaultFile, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (uri.authority == VAULT_AUTHORITY) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.vault_open_with))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {}
}

private const val VAULT_AUTHORITY = "com.android.axion.sandbox.vault"

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LockedVaultScreen(onUnlockRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceBright), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.vault_locked_title), style = MaterialTheme.typography.titleMediumEmphasized)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.vault_locked_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            FilledTonalButton(onClick = onUnlockRequest, shape = MaterialTheme.shapes.extraLarge) {
                Icon(Icons.Filled.LockOpen, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_unlock))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyVaultScreen(paddingValues: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.vault_empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.vault_empty_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
