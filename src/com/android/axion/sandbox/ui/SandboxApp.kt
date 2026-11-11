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

import android.app.AxSandboxManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.util.IconDrawableFactory
import android.util.Log
import android.util.LruCache
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.axion.compose.applist.AppEntry
import com.android.axion.compose.applist.AppFilter
import com.android.axion.compose.applist.rememberAppList
import com.android.axion.compose.preferences.SettingsType
import com.android.axion.compose.preferences.rememberSettingsFlow
import com.android.axion.compose.scaffold.AxionLargeTopAppBar
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.compose.sheet.BottomSheetDialog
import com.android.axion.sandbox.R
import com.android.axion.sandbox.ui.motion.miuixAlwaysVerticalStretch
import com.android.axion.sandbox.ui.motion.MiuixTitleMotion
import com.android.internal.app.HiddenNotificationInfo
import com.android.internal.app.IHiddenNotificationListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.OptIn
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SandboxApp"
private const val SANDBOX_CONFIG_KEY = "sandbox_config"
private const val SHEET_HEIGHT_FRACTION = 0.75f

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val iconPainter: Painter = EmptyPainter,
    val uid: Int,
    val isLocked: Boolean,
    val isHidden: Boolean,
    val isLauncherHidden: Boolean,
    val isSandboxed: Boolean,
    val isSystem: Boolean = false,
    val userId: Int = 0,
    val isClone: Boolean = false
) {
    val key: String get() = "${packageName}_$userId"
}

private data class UserSandboxPackages(
    val locked: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val launcherHidden: Set<String> = emptySet(),
    val sandboxed: Set<String> = emptySet()
)

private fun AxSandboxManager?.getUserPackages(userId: Int): UserSandboxPackages {
    if (this == null) return UserSandboxPackages()
    try {
        val locked = getLockedPackages(userId)?.toSet() ?: emptySet()
        val hidden = getHiddenPackages(userId)?.toSet() ?: emptySet()
        val launcherHidden = getHiddenFromLauncherPackages(userId)?.toSet() ?: emptySet()
        val sandboxed = getSandboxedPackages(userId)?.toSet() ?: emptySet()
        return UserSandboxPackages(locked, hidden, launcherHidden, sandboxed)
    } catch (e: Exception) {
        return UserSandboxPackages()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SandboxApp(
    onSettingsClick: () -> Unit = {},
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    isPrivateUnlocked: Boolean = false,
    onUnlockRequest: () -> Unit = {},
    isPrivateAreaExpanded: Boolean = false,
    onPrivateAreaExpandChange: (Boolean) -> Unit = {},
    isSecuritySetup: Boolean = false,
    onSetupSecurity: () -> Unit = {},
    selectedTabIndex: Int = 0,
    onTabIndexChange: (Int) -> Unit = {},
    isPickingFiles: Boolean = false,
    onPickingFilesChange: (Boolean) -> Unit = {},
    searchBackEnabled: Boolean = true,
    /** Parent shell interactive (MAIN settled, khong settings). */
    rootInteractive: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val motionScheme = MaterialTheme.motionScheme

    val appsState = rememberAppList(AppFilter.ALL, AppFilter.NO_OVERLAYS, AppFilter.INCLUDE_DUAL_APPS)
    var refreshKey by remember { mutableStateOf(0) }
    val sandboxManager = remember { context.getSystemService(Context.AX_SANDBOX_SERVICE) as? AxSandboxManager }

    val density = LocalDensity.current
    val iconTargetPx = remember(density) { with(density) { 52.dp.roundToPx() } }

    val apps by produceState<List<AppInfo>>(
        initialValue = SandboxAppListCache.cachedApps ?: emptyList(),
        appsState.value,
        refreshKey
    ) {
        if (appsState.value.isEmpty()) {
            if (SandboxAppListCache.cachedApps == null) {
                value = emptyList()
            }
            return@produceState
        }
        withContext(Dispatchers.Default) {
            val userPackagesMap = appsState.value
                .map { it.userId }
                .distinct()
                .associateWith { userId -> sandboxManager.getUserPackages(userId) }

            val iconDrawableFactory = IconDrawableFactory.newInstance(context)

            suspend fun processEntries(entries: List<AppEntry>): List<AppInfo> = coroutineScope {
                // chunked(0) throws; a list with no user (or no system) apps is valid.
                if (entries.isEmpty()) return@coroutineScope emptyList()
                val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                val chunkSize = (entries.size + parallelism - 1) / parallelism
                entries.chunked(chunkSize).map { chunk ->
                    async(Dispatchers.Default) {
                        chunk.map { entry ->
                            val userPkgs = userPackagesMap[entry.userId] ?: UserSandboxPackages()
                            val painter = AppIconCache.loadIconPainter(
                                packageName = entry.packageName,
                                userId = entry.userId,
                                icon = entry.icon,
                                iconDrawableFactory = iconDrawableFactory,
                                targetPx = iconTargetPx
                            )
                            AppInfo(
                                packageName = entry.packageName,
                                label = entry.label,
                                icon = entry.icon,
                                iconPainter = painter,
                                uid = 0,
                                isLocked = userPkgs.locked.contains(entry.packageName),
                                isHidden = userPkgs.hidden.contains(entry.packageName),
                                isLauncherHidden = userPkgs.launcherHidden.contains(entry.packageName),
                                isSandboxed = userPkgs.sandboxed.contains(entry.packageName),
                                isSystem = entry.isSystem,
                                userId = entry.userId,
                                isClone = entry.isClone
                            )
                        }
                    }
                }.awaitAll().flatten()
            }

            val (regularEntries, systemEntries) = appsState.value.partition { !it.isSystem }
            val regularApps = processEntries(regularEntries)
            val systemApps = processEntries(systemEntries)
            val sorted = (regularApps + systemApps).sortedBy { it.label.lowercase() }
            SandboxAppListCache.cachedApps = sorted
            value = sorted
        }
    }
    val isLoading = apps.isEmpty()
    // Saveable root state song song detail/settings (khong reset khi overlay).
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    // Snapshot UI detail giu den khi transition settled; relock clear ngay.
    var detailSnapshot by remember { mutableStateOf<AppInfo?>(null) }

    val reloadApps: () -> Unit = {
        refreshKey++
        val live = selectedApp ?: detailSnapshot
        live?.let { selected ->
            val updated = apps.find {
                it.packageName == selected.packageName && it.userId == selected.userId
            }
            if (updated != null) {
                if (selectedApp != null) selectedApp = updated
                detailSnapshot = updated
            }
        }
    }

    val settingsFlow = rememberSettingsFlow(SettingsType.SECURE)
    LaunchedEffect(settingsFlow) {
        settingsFlow.observe(SANDBOX_CONFIG_KEY).collect {
            reloadApps()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reloadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    var notificationCount by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? android.app.AxSandboxManager

        val listener = object : IHiddenNotificationListener.Stub() {
            override fun onHiddenNotificationPosted(info: HiddenNotificationInfo) {
                scope.launch(Dispatchers.Main) {
                    notificationCount++
                }
            }

            override fun onHiddenNotificationRemoved(key: String) {
                scope.launch(Dispatchers.Main) {
                    if (notificationCount > 0) notificationCount--
                }
            }
        }

        try {
            sandboxManager?.registerHiddenNotificationListener(listener)
            scope.launch(Dispatchers.IO) {
                val count = sandboxManager?.hiddenNotifications?.size ?: 0
                withContext(Dispatchers.Main) {
                    notificationCount = count
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register notification listener", e)
        }

        onDispose {
            try {
                sandboxManager?.unregisterHiddenNotificationListener(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister notification listener", e)
            }
        }
    }

    // Relock: clear ngay — khong giu UI/semantics nhay cam qua transition.
    LaunchedEffect(isPrivateUnlocked, selectedApp, detailSnapshot) {
        val app = selectedApp ?: detailSnapshot ?: return@LaunchedEffect
        if (!isPrivateUnlocked && (app.isLocked || app.isHidden)) {
            selectedApp = null
            detailSnapshot = null
        }
    }

    LaunchedEffect(selectedApp) {
        if (selectedApp != null) detailSnapshot = selectedApp
    }

    var detailPredictive by remember { mutableStateOf<com.android.axion.sandbox.ui.motion.PredictiveBackScrub?>(null) }
    val detailSessionSeq = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val detailOpen = selectedApp != null
    val detailBackEnabled = rootInteractive && searchBackEnabled && detailOpen
    androidx.activity.compose.PredictiveBackHandler(enabled = detailBackEnabled) { progress ->
        val sessionId = detailSessionSeq.incrementAndGet()
        try {
            progress.collect { event ->
                detailPredictive = com.android.axion.sandbox.ui.motion.PredictiveBackScrub(
                    sessionId,
                    event.progress,
                )
            }
            selectedApp = null
        } catch (_: kotlinx.coroutines.CancellationException) {
        } finally {
            if (detailPredictive?.sessionId == sessionId) detailPredictive = null
        }
    }

    com.android.axion.sandbox.ui.motion.MiuixPageTransition(
        targetState = if (detailOpen) {
            com.android.axion.sandbox.ui.motion.SandboxSubRoute.APP_DETAIL
        } else {
            com.android.axion.sandbox.ui.motion.SandboxSubRoute.ROOT
        },
        predictiveBack = detailPredictive,
        onSettledAtRoot = { detailSnapshot = null },
        rootContent = {
            SandboxMainShell(
                apps = filteredApps,
                isLoading = isLoading,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isSearchExpanded = isSearchExpanded,
                onSearchExpandedChange = onSearchExpandedChange,
                onSettingsClick = onSettingsClick,
                isPrivateUnlocked = isPrivateUnlocked,
                onUnlockRequest = onUnlockRequest,
                isPrivateAreaExpanded = isPrivateAreaExpanded,
                onPrivateAreaExpandChange = onPrivateAreaExpandChange,
                isSecuritySetup = isSecuritySetup,
                onSetupSecurity = onSetupSecurity,
                selectedTabIndex = selectedTabIndex,
                onTabIndexChange = onTabIndexChange,
                notificationCount = notificationCount,
                onPickingFilesChange = onPickingFilesChange,
                onAppSelected = { app ->
                    selectedApp = app
                    detailSnapshot = app
                },
                searchBackEnabled = searchBackEnabled && !detailOpen,
                rootInteractive = rootInteractive && !detailOpen,
            )
        },
        childContent = {
            detailSnapshot?.let { app ->
                AppDetailScreen(
                    app = app,
                    onBackClick = { selectedApp = null },
                    interactionsEnabled = detailOpen,
                    onLockToggle = { target ->
                        if (!detailOpen) return@AppDetailScreen
                        scope.launch {
                            toggleAppLock(context, target.packageName, target.userId, !target.isLocked)
                            reloadApps()
                        }
                    },
                    onHideToggle = { target ->
                        if (!detailOpen) return@AppDetailScreen
                        scope.launch {
                            val nextHideState = !target.isHidden
                            toggleAppHidden(context, target.packageName, target.userId, nextHideState)
                            if (nextHideState) {
                                toggleAppLauncherHidden(context, target.packageName, target.userId, false)
                                selectedApp = target.copy(isHidden = true, isLauncherHidden = false)
                            } else {
                                selectedApp = target.copy(isHidden = false)
                            }
                            reloadApps()
                        }
                    },
                    onLauncherHideToggle = { target ->
                        if (!detailOpen) return@AppDetailScreen
                        scope.launch {
                            val nextLauncherHideState = !target.isLauncherHidden
                            toggleAppLauncherHidden(
                                context,
                                target.packageName,
                                target.userId,
                                nextLauncherHideState,
                            )
                            if (nextLauncherHideState) {
                                toggleAppHidden(context, target.packageName, target.userId, false)
                                selectedApp = target.copy(isLauncherHidden = true, isHidden = false)
                            } else {
                                selectedApp = target.copy(isLauncherHidden = false)
                            }
                            reloadApps()
                        }
                    },
                    onSandboxToggle = { target ->
                        if (!detailOpen) return@AppDetailScreen
                        scope.launch {
                            toggleAppSandboxed(
                                context,
                                target.packageName,
                                target.userId,
                                !target.isSandboxed,
                            )
                            reloadApps()
                        }
                    },
                    onLaunch = { target ->
                        if (!detailOpen) return@AppDetailScreen
                        launchApp(context, target.packageName, target.userId)
                    },
                    isSecuritySetup = isSecuritySetup,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SandboxOptionsDropdown(
    app: AppInfo,
    sandboxManager: AxSandboxManager?,
    onSandboxToggle: (AppInfo) -> Unit,
    interactionsEnabled: Boolean = true,
    groupTitleRes: Int = R.string.sandbox_options_title,
) {
    val scope = rememberCoroutineScope()

    var restrictInternet by remember(app.packageName, app.userId) {
        val gids = sandboxManager?.getRestrictedGids(app.packageName, app.userId)
        mutableStateOf(gids != null && gids.contains(AxSandboxManager.GID_INET))
    }

    var restrictStorage by remember(app.packageName, app.userId) {
        val gids = sandboxManager?.getRestrictedGids(app.packageName, app.userId)
        val storageGids = AxSandboxManager.STORAGE_GIDS
        mutableStateOf(gids != null && storageGids.any { gids.contains(it) })
    }

    var dataIsolation by remember(app.packageName, app.userId) {
        mutableStateOf(sandboxManager?.isSandboxDataIsolationEnabled(app.packageName, app.userId) == true)
    }

    LocalPreferenceGroup(title = stringResource(groupTitleRes)) {
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.action_isolate),
                summary = stringResource(R.string.isolate_app_description),
                position = pos,
                checked = app.isSandboxed,
                onCheckedChange = { if (interactionsEnabled) onSandboxToggle(app) },
                enabled = interactionsEnabled,
            )
        }
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.restrict_internet_title),
                summary = stringResource(R.string.restrict_internet_description),
                position = pos,
                checked = restrictInternet,
                enabled = interactionsEnabled && sandboxManager != null,
                onCheckedChange = {
                    if (!interactionsEnabled || sandboxManager == null) return@LocalSwitchPreference
                    restrictInternet = !restrictInternet
                    scope.launch(Dispatchers.IO) {
                        val current = sandboxManager.getRestrictedGids(app.packageName, app.userId)
                            ?.toMutableList() ?: mutableListOf()
                        if (restrictInternet) {
                            if (!current.contains(AxSandboxManager.GID_INET)) {
                                current.add(AxSandboxManager.GID_INET)
                            }
                        } else {
                            current.remove(AxSandboxManager.GID_INET)
                        }
                        sandboxManager.setRestrictedGids(
                            app.packageName,
                            current.toIntArray(),
                            app.userId,
                        )
                    }
                },
            )
        }
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.restrict_storage_title),
                summary = stringResource(R.string.restrict_storage_description),
                position = pos,
                checked = restrictStorage,
                enabled = interactionsEnabled && sandboxManager != null,
                onCheckedChange = {
                    if (!interactionsEnabled || sandboxManager == null) return@LocalSwitchPreference
                    restrictStorage = !restrictStorage
                    scope.launch(Dispatchers.IO) {
                        val current = sandboxManager.getRestrictedGids(app.packageName, app.userId)
                            ?.toMutableList() ?: mutableListOf()
                        val storageGids = AxSandboxManager.STORAGE_GIDS
                        if (restrictStorage) {
                            for (gid in storageGids) {
                                if (!current.contains(gid)) current.add(gid)
                            }
                        } else {
                            current.removeAll(storageGids.toSet())
                        }
                        sandboxManager.setRestrictedGids(
                            app.packageName,
                            current.toIntArray(),
                            app.userId,
                        )
                    }
                },
            )
        }
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.data_isolation_title),
                summary = stringResource(R.string.data_isolation_description),
                position = pos,
                checked = dataIsolation,
                enabled = interactionsEnabled && sandboxManager != null,
                onCheckedChange = {
                    if (!interactionsEnabled || sandboxManager == null) return@LocalSwitchPreference
                    dataIsolation = !dataIsolation
                    scope.launch(Dispatchers.IO) {
                        sandboxManager.setSandboxDataIsolationEnabled(
                            app.packageName,
                            dataIsolation,
                            app.userId,
                        )
                    }
                },
            )
        }
    }
}

private data class SpoofSettingEntry(
    val key: String,
    val titleRes: Int,
    val descriptionRes: Int
)

private val SPOOF_SETTINGS = listOf(
    SpoofSettingEntry("adb_enabled",
            R.string.spoof_adb_enabled, R.string.spoof_adb_enabled_description),
    SpoofSettingEntry("development_settings_enabled",
            R.string.spoof_dev_settings_enabled, R.string.spoof_dev_settings_enabled_description),
    SpoofSettingEntry("adb_wifi_enabled",
            R.string.spoof_adb_wifi_enabled, R.string.spoof_adb_wifi_enabled_description),
    SpoofSettingEntry("package_verifier_user_consent",
            R.string.spoof_package_verifier, R.string.spoof_package_verifier_description),
    SpoofSettingEntry("verify_apps_over_usb",
            R.string.spoof_verify_apps_usb, R.string.spoof_verify_apps_usb_description)
)

private val SPOOF_ACCESSIBILITY_KEYS = listOf(
    "accessibility_enabled",
    "enabled_accessibility_services",
    "accessibility_display_inversion_enabled"
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SpoofSettingsDropdown(
    app: AppInfo,
    sandboxManager: AxSandboxManager?,
    interactionsEnabled: Boolean = true,
    groupTitleRes: Int = R.string.spoof_settings_title,
) {
    if (sandboxManager == null) return

    val scope = rememberCoroutineScope()

    val enabledSettings = remember(app.packageName, app.userId) {
        mutableStateMapOf<String, Boolean>().apply {
            SPOOF_SETTINGS.forEach { entry ->
                put(
                    entry.key,
                    sandboxManager.isSpoofSettingEnabled(app.packageName, entry.key, app.userId),
                )
            }
            put(
                "accessibility_spoof",
                SPOOF_ACCESSIBILITY_KEYS.all { key ->
                    sandboxManager.isSpoofSettingEnabled(app.packageName, key, app.userId)
                },
            )
        }
    }

    LocalPreferenceGroup(title = stringResource(groupTitleRes)) {
        SPOOF_SETTINGS.forEach { entry ->
            row { pos ->
                val isEnabled = enabledSettings[entry.key] ?: false
                LocalSwitchPreference(
                    title = stringResource(entry.titleRes),
                    summary = stringResource(entry.descriptionRes),
                    position = pos,
                    checked = isEnabled,
                    enabled = interactionsEnabled,
                    onCheckedChange = {
                        if (!interactionsEnabled) return@LocalSwitchPreference
                        val newValue = !isEnabled
                        enabledSettings[entry.key] = newValue
                        scope.launch(Dispatchers.IO) {
                            sandboxManager.setSpoofSettingEnabled(
                                app.packageName,
                                entry.key,
                                newValue,
                                app.userId,
                            )
                        }
                    },
                )
            }
        }
        row { pos ->
            val accessibilityEnabled = enabledSettings["accessibility_spoof"] ?: false
            LocalSwitchPreference(
                title = stringResource(R.string.spoof_accessibility),
                summary = stringResource(R.string.spoof_accessibility_description),
                position = pos,
                checked = accessibilityEnabled,
                enabled = interactionsEnabled,
                onCheckedChange = {
                    if (!interactionsEnabled) return@LocalSwitchPreference
                    val newValue = !accessibilityEnabled
                    enabledSettings["accessibility_spoof"] = newValue
                    scope.launch(Dispatchers.IO) {
                        SPOOF_ACCESSIBILITY_KEYS.forEach { key ->
                            sandboxManager.setSpoofSettingEnabled(
                                app.packageName,
                                key,
                                newValue,
                                app.userId,
                            )
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HideSettingsDropdown(
    app: AppInfo,
    onHideToggle: (AppInfo) -> Unit,
    onLauncherHideToggle: (AppInfo) -> Unit,
    interactionsEnabled: Boolean = true,
    onLockToggle: ((AppInfo) -> Unit)? = null,
    groupTitleRes: Int = R.string.hide_settings_title,
) {
    LocalPreferenceGroup(title = stringResource(groupTitleRes)) {
        if (onLockToggle != null) {
            row { pos ->
                LocalSwitchPreference(
                    title = stringResource(R.string.lock_app_title),
                    summary = stringResource(R.string.lock_app_description),
                    position = pos,
                    checked = app.isLocked,
                    onCheckedChange = { if (interactionsEnabled) onLockToggle(app) },
                    icon = if (app.isLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                    enabled = interactionsEnabled,
                )
            }
        }
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.hide_app_title),
                summary = if (app.isHidden) {
                    stringResource(R.string.hide_app_description_on)
                } else {
                    stringResource(R.string.hide_app_description_off)
                },
                position = pos,
                checked = app.isHidden,
                enabled = interactionsEnabled,
                onCheckedChange = { if (interactionsEnabled) onHideToggle(app) },
            )
        }
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.hide_from_launcher_title),
                summary = if (app.isLauncherHidden) {
                    stringResource(R.string.hide_from_launcher_description_on)
                } else {
                    stringResource(R.string.hide_from_launcher_description_off)
                },
                position = pos,
                checked = app.isLauncherHidden,
                enabled = interactionsEnabled,
                onCheckedChange = { if (interactionsEnabled) onLauncherHideToggle(app) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDetailScreen(
    app: AppInfo,
    onBackClick: () -> Unit,
    onLockToggle: (AppInfo) -> Unit,
    onHideToggle: (AppInfo) -> Unit,
    onLauncherHideToggle: (AppInfo) -> Unit,
    onSandboxToggle: (AppInfo) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    isSecuritySetup: Boolean = false,
    interactionsEnabled: Boolean = true,
) {
    SandboxSubmenuScaffold(
        title = app.label,
        onBackClick = onBackClick,
        actions = {
            IconButton(
                onClick = { if (interactionsEnabled) onLaunch(app) },
                enabled = interactionsEnabled,
                modifier = Modifier.size(MiuixTitleMotion.BackTouchMinDp.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(MiuixTitleMotion.BackCircleDp.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.action_launch),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(MiuixTitleMotion.BackArrowDp.dp),
                    )
                }
            }
        },
    ) {
        LazyColumn(
            state = listState,
            overscrollEffect = listOverscrollEffect,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = interactionsEnabled,
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceBright),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = app.iconPainter,
                        contentDescription = app.label,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            item {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (isSecuritySetup) {
                item {
                    HideSettingsDropdown(
                        app = app,
                        onHideToggle = onHideToggle,
                        onLauncherHideToggle = onLauncherHideToggle,
                        interactionsEnabled = interactionsEnabled,
                        onLockToggle = onLockToggle,
                        groupTitleRes = R.string.detail_section_lock_hide,
                    )
                }
            }

            item {
                val context = LocalContext.current
                val sandboxManager = remember {
                    context.getSystemService(android.app.AxSandboxManager::class.java)
                }
                SandboxOptionsDropdown(
                    app = app,
                    sandboxManager = sandboxManager,
                    onSandboxToggle = onSandboxToggle,
                    interactionsEnabled = interactionsEnabled,
                    groupTitleRes = R.string.detail_section_isolation,
                )
            }

            item {
                val context = LocalContext.current
                val sandboxManager = remember {
                    context.getSystemService(android.app.AxSandboxManager::class.java)
                }
                SpoofSettingsDropdown(
                    app = app,
                    sandboxManager = sandboxManager,
                    interactionsEnabled = interactionsEnabled,
                    groupTitleRes = R.string.detail_section_spoofing,
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsTab(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onAppClick: (AppInfo) -> Unit,
    isPrivateAreaExpanded: Boolean = false,
    onPrivateAreaExpandChange: (Boolean) -> Unit = {},
    onUnlockRequest: () -> Unit = {},
    isPrivateUnlocked: Boolean = false,
    isSecuritySetup: Boolean = false,
    onSetupSecurity: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listOverscrollEffect: androidx.compose.foundation.OverscrollEffect? = null,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState =
        androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    listState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState(),
    useAlwaysStretch: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showSystemApps by rememberSaveable { mutableStateOf(false) }

    val privateApps = remember(apps) { apps.filter { it.isLocked || it.isHidden } }
    val sandboxedApps = remember(apps) { apps.filter { it.isSandboxed && !it.isLocked && !it.isHidden } }
    val regularApps = remember(apps) { apps.filter { !it.isLocked && !it.isHidden && !it.isSandboxed && !it.isSystem } }
    val systemApps = remember(apps) { apps.filter { !it.isLocked && !it.isHidden && !it.isSandboxed && it.isSystem } }
    val displayedApps = remember(regularApps, systemApps, showSystemApps) {
        if (showSystemApps) regularApps + systemApps else regularApps
    }

    val effectiveExpanded = isPrivateUnlocked && isPrivateAreaExpanded

    val onLockToggle: (AppInfo) -> Unit = { app ->
        scope.launch {
            toggleAppLock(context, app.packageName, app.userId, !app.isLocked)
            selectedApp = app.copy(isLocked = !app.isLocked)
        }
    }

    val onHideToggle: (AppInfo) -> Unit = { app ->
        scope.launch {
            val nextHideState = !app.isHidden
            toggleAppHidden(context, app.packageName, app.userId, nextHideState)
            if (nextHideState) {
                toggleAppLauncherHidden(context, app.packageName, app.userId, false)
                selectedApp = app.copy(isHidden = true, isLauncherHidden = false)
            } else {
                selectedApp = app.copy(isHidden = false)
            }
        }
    }

    val onLauncherHideToggle: (AppInfo) -> Unit = { app ->
        scope.launch {
            val nextLauncherHideState = !app.isLauncherHidden
            toggleAppLauncherHidden(context, app.packageName, app.userId, nextLauncherHideState)
            if (nextLauncherHideState) {
                toggleAppHidden(context, app.packageName, app.userId, false)
                selectedApp = app.copy(isLauncherHidden = true, isHidden = false)
            } else {
                selectedApp = app.copy(isLauncherHidden = false)
            }
        }
    }

    val onSandboxToggle: (AppInfo) -> Unit = { app ->
        scope.launch {
            toggleAppSandboxed(context, app.packageName, app.userId, !app.isSandboxed)
            selectedApp = app.copy(isSandboxed = !app.isSandboxed)
        }
    }

    val onSheetDismiss = {
        showBottomSheet = false
        selectedApp = null
    }

    // Relock: dong sheet private, khong giu noi dung nhay cam.
    LaunchedEffect(isPrivateUnlocked) {
        if (!isPrivateUnlocked && selectedApp != null &&
            (selectedApp!!.isLocked || selectedApp!!.isHidden)
        ) {
            showBottomSheet = false
            selectedApp = null
        }
    }

    if (showBottomSheet && selectedApp != null) {
        BottomSheetDialog(
            onDismiss = onSheetDismiss,
            heightFraction = SHEET_HEIGHT_FRACTION,
        ) {
            AppQuickActionsSheet(
                app = selectedApp!!,
                onLockToggle = onLockToggle,
                onHideToggle = onHideToggle,
                onLauncherHideToggle = onLauncherHideToggle,
                onSandboxToggle = onSandboxToggle,
                onLaunch = { app -> launchApp(context, app.packageName, app.userId) },
                onOpenDetails = { app ->
                    onSheetDismiss()
                    onAppClick(app)
                },
                onDismiss = onSheetDismiss,
            )
        }
    }

    val shortStretchModifier = if (useAlwaysStretch || isLoading || apps.isEmpty()) {
        Modifier
            .fillMaxSize()
            .then(
                if (listOverscrollEffect != null) {
                    Modifier.miuixAlwaysVerticalStretch(listOverscrollEffect)
                } else {
                    Modifier
                },
            )
    } else {
        Modifier.fillMaxSize()
    }

    if (isLoading) {
        Box(
            modifier = shortStretchModifier,
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    } else if (apps.isEmpty()) {
        Box(
            modifier = shortStretchModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_apps_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Private content khong compose khi locked / chua expand.
        val privateVisible = effectiveExpanded
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 4.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            overscrollEffect = listOverscrollEffect,
        ) {
            // 1. The noi bat "Ung dung rieng tu" theo dung layout goi y trong anh
            item(
                key = "card_private",
                contentType = "card_private",
            ) {
                PrivateAppsCard(
                    isSecuritySetup = isSecuritySetup,
                    isUnlocked = isPrivateUnlocked,
                    count = privateApps.size,
                    isExpanded = effectiveExpanded,
                    onSetupSecurity = onSetupSecurity,
                    onUnlockRequest = onUnlockRequest,
                    onExpandChange = onPrivateAreaExpandChange,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (effectiveExpanded && privateApps.isNotEmpty()) {
                itemsIndexed(
                    items = privateApps,
                    key = { _, app -> "private_${app.key}" },
                    contentType = { _, _ -> "app" },
                ) { index, app ->
                    val pos = localPrefPosition(index, privateApps.size)
                    AppListPreferenceItem(
                        app = app,
                        position = pos,
                        onClick = {
                            selectedApp = app
                            showBottomSheet = true
                        },
                    )
                }
            }

            if (sandboxedApps.isNotEmpty()) {
                item(
                    key = "hdr_sandboxed",
                    contentType = "section_header",
                ) {
                    SectionHeader(
                        icon = Icons.Filled.Security,
                        title = stringResource(R.string.section_isolated_apps),
                        count = sandboxedApps.size,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                itemsIndexed(
                    items = sandboxedApps,
                    key = { _, app -> "sandboxed_${app.key}" },
                    contentType = { _, _ -> "app" },
                ) { index, app ->
                    val pos = localPrefPosition(index, sandboxedApps.size)
                    AppListPreferenceItem(
                        app = app,
                        position = pos,
                        onClick = {
                            selectedApp = app
                            showBottomSheet = true
                        },
                    )
                }
            }

            // 2. Tieu de "Tat ca ung dung" voi bo loc "He thong v" ben phai theo anh goi y
            item(
                key = "hdr_regular",
                contentType = "section_header",
            ) {
                AllAppsSectionHeader(
                    count = displayedApps.size,
                    showSystemApps = showSystemApps,
                    onShowSystemAppsChange = { showSystemApps = it },
                )
            }

            // 3. Danh sach tat ca ung dung dang the nhom bo goc
            itemsIndexed(
                items = displayedApps,
                key = { _, app -> "app_${app.key}" },
                contentType = { _, _ -> "app" },
            ) { index, app ->
                val pos = localPrefPosition(index, displayedApps.size)
                AppListPreferenceItem(
                    app = app,
                    position = pos,
                    onClick = {
                        selectedApp = app
                        showBottomSheet = true
                    },
                )
            }
        }
    }
}

/**
 * The ung dung rieng tu noi bat theo bo cuc de xuat trong anh:
 * Container xanh reu nhe, icon khoa dong khung bo, tieu de va so luong, chevron phai.
 */
@Composable
private fun PrivateAppsCard(
    isSecuritySetup: Boolean,
    isUnlocked: Boolean,
    count: Int,
    isExpanded: Boolean,
    onSetupSecurity: () -> Unit,
    onUnlockRequest: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val title = stringResource(R.string.section_private_apps)
    val subtitle = if (!isSecuritySetup) {
        stringResource(R.string.setup_private_apps_description)
    } else {
        stringResource(R.string.private_apps_count_format, count)
    }
    val onClick = {
        if (!isSecuritySetup) {
            onSetupSecurity()
        } else if (!isUnlocked) {
            onUnlockRequest()
        } else {
            onExpandChange(!isExpanded)
        }
    }

    val cardBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    val iconBadgeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val iconTint = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .miuixPressScale(interactionSource)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isUnlocked && isExpanded) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_settings_expressive_chevron),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Hang tieu de "Tat ca ung dung" voi bo loc dropdown "He thong v" ben phai theo anh goi y.
 */
@Composable
private fun AllAppsSectionHeader(
    count: Int,
    showSystemApps: Boolean,
    onShowSystemAppsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.section_all_apps),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onShowSystemAppsChange(!showSystemApps) }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_system_apps),
                style = MaterialTheme.typography.bodyMedium,
                color = if (showSystemApps) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (showSystemApps) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (showSystemApps) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = stringResource(R.string.show_system_apps),
                modifier = Modifier.size(18.dp),
                tint = if (showSystemApps) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    color: Color,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.height(22.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollapsibleSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    color: Color,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "chevron",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .miuixPressScale(interactionSource)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) { onExpandChange(!isExpanded) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = color,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val protectionStates = remember(app.isLocked, app.isHidden, app.isSandboxed, primaryColor, tertiaryColor, secondaryColor) {
        listOfNotNull(
            if (app.isLocked) Pair(Icons.Filled.Lock, primaryColor) else null,
            if (app.isHidden) Pair(Icons.Filled.VisibilityOff, tertiaryColor) else null,
            if (app.isSandboxed) Pair(Icons.Filled.Security, secondaryColor) else null
        )
    }
    val hasAnyProtection = protectionStates.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Image(
                painter = app.iconPainter,
                contentDescription = app.label,
                modifier = Modifier.size(52.dp)
            )

            if (hasAnyProtection) {
                if (protectionStates.size == 1) {
                    val (badgeIcon, badgeColor) = protectionStates[0]
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(badgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${protectionStates.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 4.dp, y = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy((-2).dp)
                    ) {
                        protectionStates.take(3).forEachIndexed { _, (_, color) ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val protectionDesc = buildString {
            if (app.isLocked) append(stringResource(R.string.action_lock)).append(' ')
            if (app.isHidden) append(stringResource(R.string.action_hide)).append(' ')
            if (app.isSandboxed) append(stringResource(R.string.action_isolate))
        }.trim()
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = if (protectionDesc.isNotEmpty()) {
                    "${app.label}, $protectionDesc"
                } else {
                    app.label
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppQuickActionsSheet(
    app: AppInfo,
    onLockToggle: (AppInfo) -> Unit,
    onHideToggle: (AppInfo) -> Unit,
    onLauncherHideToggle: (AppInfo) -> Unit,
    onSandboxToggle: (AppInfo) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onOpenDetails: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sandboxManager = remember {
        context.getSystemService(AxSandboxManager::class.java)
    }
    // Dem day theo thanh dieu huong de noi dung cuon xuyen qua nen trong suot
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp + navBarsBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            LocalExpressivePreferenceRow(
                title = app.label,
                summary = app.packageName,
                position = LocalPrefPosition.Single,
                leading = {
                    Image(
                        painter = app.iconPainter,
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp),
                    )
                },
                trailing = {
                    FilledTonalIconButton(onClick = { onLaunch(app) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = stringResource(R.string.action_launch),
                        )
                    }
                },
            )
        }

        item {
            LocalPreferenceGroup {
                row { pos ->
                    LocalClickablePreference(
                        title = stringResource(R.string.action_app_details),
                        icon = Icons.Filled.Settings,
                        position = pos,
                        onClick = { onOpenDetails(app) },
                    )
                }
            }
        }

        item {
            LocalPreferenceGroup {
                row { pos ->
                    LocalSwitchPreference(
                        title = stringResource(R.string.action_lock),
                        position = pos,
                        checked = app.isLocked,
                        onCheckedChange = { onLockToggle(app) },
                        icon = if (app.isLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                    )
                }
            }
        }

        item {
            HideSettingsDropdown(
                app = app,
                onHideToggle = onHideToggle,
                onLauncherHideToggle = onLauncherHideToggle,
            )
        }

        item {
            SandboxOptionsDropdown(
                app = app,
                sandboxManager = sandboxManager,
                onSandboxToggle = onSandboxToggle,
            )
        }

        item {
            SpoofSettingsDropdown(
                app = app,
                sandboxManager = sandboxManager,
            )
        }
    }
}

data class HiddenNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val icon: Drawable? = null,
    val contentIntent: PendingIntent? = null,
    val userId: Int = 0
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsTab(
    isUnlocked: Boolean = false,
    onUnlockRequest: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listOverscrollEffect: androidx.compose.foundation.OverscrollEffect? = null,
    listState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState(),
    useAlwaysStretch: Boolean = false,
) {
    val stretchMod = if (listOverscrollEffect != null && (useAlwaysStretch || !isUnlocked)) {
        Modifier.fillMaxSize().miuixAlwaysVerticalStretch(listOverscrollEffect)
    } else {
        Modifier.fillMaxSize()
    }
    if (!isUnlocked) {
        Box(
            modifier = stretchMod,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceBright),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.notifications_locked_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.notifications_locked_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = onUnlockRequest,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_unlock))
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<HiddenNotification>>(emptyList()) }

    DisposableEffect(Unit) {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? android.app.AxSandboxManager

        val listener = object : IHiddenNotificationListener.Stub() {
            override fun onHiddenNotificationPosted(info: HiddenNotificationInfo) {
                scope.launch(Dispatchers.Main) {
                    val pm = context.packageManager
                    val icon = info.appIcon?.loadDrawable(context) ?: try {
                         pm.getApplicationIcon(info.packageName)
                    } catch (e: Exception) { null }

                    val notif = HiddenNotification(
                        key = info.key,
                        packageName = info.packageName,
                        title = info.title?.toString(),
                        text = info.text?.toString(),
                        timestamp = info.postTime,
                        icon = icon,
                        contentIntent = info.contentIntent,
                        userId = info.userId
                    )
                    notifications = (listOf(notif) + notifications.filter { it.key != info.key })
                }
            }

            override fun onHiddenNotificationRemoved(key: String) {
                scope.launch(Dispatchers.Main) {
                    notifications = notifications.filter { it.key != key }
                }
            }
        }

        try {
            sandboxManager?.registerHiddenNotificationListener(listener)
            scope.launch(Dispatchers.IO) {
                val current = sandboxManager?.hiddenNotifications ?: emptyList()
                withContext(Dispatchers.Main) {
                    val pm = context.packageManager
                    notifications = current.map { info ->
                         val icon = info.appIcon?.loadDrawable(context) ?: try {
                             pm.getApplicationIcon(info.packageName)
                        } catch (e: Exception) { null }

                        HiddenNotification(
                            key = info.key,
                            packageName = info.packageName,
                            title = info.title?.toString(),
                            text = info.text?.toString(),
                            timestamp = info.postTime,
                            icon = icon,
                            contentIntent = info.contentIntent,
                            userId = info.userId
                        )
                    }.sortedByDescending { it.timestamp }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register notification listener", e)
        }

        onDispose {
            try {
                sandboxManager?.unregisterHiddenNotificationListener(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister notification listener", e)
            }
        }
    }

    if (notifications.isEmpty()) {
        val emptyStretch = if (listOverscrollEffect != null) {
            Modifier.fillMaxSize().miuixAlwaysVerticalStretch(listOverscrollEffect)
        } else {
            Modifier.fillMaxSize()
        }
        Box(
            modifier = emptyStretch,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.no_private_notifications_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.no_private_notifications_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 4.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            overscrollEffect = listOverscrollEffect,
        ) {
            items(notifications, key = { it.key }) { notification ->
                NotificationItem(
                    notification = notification,
                    onLaunch = {
                        try {
                            notification.contentIntent?.send()
                                ?: launchApp(context, notification.packageName, notification.userId)
                        } catch (e: Exception) {
                            launchApp(context, notification.packageName, notification.userId)
                        }
                    },
                    onDismiss = {
                        notifications = notifications.filter { it.key != notification.key }
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: HiddenNotification,
    onLaunch: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLaunch)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (notification.icon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = rememberDrawablePainter(notification.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = notification.title ?: notification.packageName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!notification.text.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_dismiss))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onLaunch,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_open))
                }
            }
        }
    }
}

private suspend fun toggleAppLock(context: Context, packageName: String, userId: Int, lock: Boolean) = withContext(Dispatchers.IO) {
    try {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? AxSandboxManager ?: return@withContext
        if (lock) sandboxManager.addLockedApp(packageName, userId) else sandboxManager.removeLockedApp(packageName, userId)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling app lock for $packageName", e)
    }
}

private suspend fun toggleAppHidden(context: Context, packageName: String, userId: Int, hidden: Boolean) = withContext(Dispatchers.IO) {
    try {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? AxSandboxManager ?: return@withContext
        sandboxManager.setPackageHidden(packageName, hidden, userId)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling app hidden for $packageName", e)
    }
}

private suspend fun toggleAppLauncherHidden(context: Context, packageName: String, userId: Int, hidden: Boolean) = withContext(Dispatchers.IO) {
    try {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? AxSandboxManager ?: return@withContext
        sandboxManager.setPackageHiddenFromLauncher(packageName, hidden, userId)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling app launcher hidden for $packageName", e)
    }
}

private suspend fun toggleAppSandboxed(context: Context, packageName: String, userId: Int, sandboxed: Boolean) = withContext(Dispatchers.IO) {
    try {
        val sandboxManager = context.getSystemService(Context.AX_SANDBOX_SERVICE) as? AxSandboxManager ?: return@withContext
        if (sandboxed) sandboxManager.addSandboxedPackage(packageName, userId) else sandboxManager.removeSandboxedPackage(packageName, userId)
    } catch (e: Exception) {
        Log.e(TAG, "Error toggling app sandboxed for $packageName", e)
    }
}

private fun launchApp(context: Context, packageName: String, userId: Int = 0) {
    try {
        if (userId != 0) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val activities = launcherApps?.getActivityList(packageName, UserHandle.of(userId))
            if (!activities.isNullOrEmpty()) {
                launcherApps.startMainActivity(activities[0].componentName, UserHandle.of(userId), null, null)
                return
            }
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error launching $packageName", e)
    }
}

/** Cache danh sach ung dung in-memory de chong load lai / flicker khi chuyen doi man hinh. */
internal object SandboxAppListCache {
    @Volatile
    var cachedApps: List<AppInfo>? = null
}

private object AppIconCache {
    private val cache = LruCache<String, Painter>(1024)

    fun get(key: String): Painter? = cache.get(key)
    fun put(key: String, painter: Painter) {
        cache.put(key, painter)
    }

    fun loadIconPainter(
        packageName: String,
        userId: Int,
        icon: Drawable,
        iconDrawableFactory: IconDrawableFactory,
        targetPx: Int
    ): Painter {
        val key = "${packageName}_$userId"
        val cached = cache.get(key)
        if (cached != null) return cached

        val shadowedDrawable = try {
            iconDrawableFactory.getShadowedIcon(icon)
        } catch (e: Exception) {
            icon
        }

        val bitmapWidth = if (shadowedDrawable.intrinsicWidth > 0) shadowedDrawable.intrinsicWidth else targetPx * 2
        val bitmapHeight = if (shadowedDrawable.intrinsicHeight > 0) shadowedDrawable.intrinsicHeight else targetPx * 2
        val bitmap = shadowedDrawable.toBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val painter = BitmapPainter(bitmap.asImageBitmap(), filterQuality = FilterQuality.High)
        cache.put(key, painter)
        return painter
    }
}

@Composable
internal fun rememberAppIconPainter(app: AppInfo): Painter = app.iconPainter

@Composable
internal fun rememberDrawablePainter(drawable: Drawable?): Painter = remember(drawable) {
    when (drawable) {
        null -> EmptyPainter
        is BitmapDrawable -> {
            drawable.isFilterBitmap = true
            BitmapPainter(drawable.bitmap.asImageBitmap(), filterQuality = FilterQuality.High)
        }
        is ColorDrawable -> ColorPainter(Color(drawable.color))
        else -> DrawablePainter(drawable.mutate())
    }
}

internal class DrawablePainter(
    val drawable: Drawable
) : Painter(), RememberObserver {
    private val drawInvalidateTick = mutableStateOf(0)
    private val drawableIntrinsicSize = mutableStateOf(drawable.intrinsicSize)

    private val callback: Drawable.Callback by lazy {
        object : Drawable.Callback {
            override fun invalidateDrawable(d: Drawable) {
                drawInvalidateTick.value++
                drawableIntrinsicSize.value = drawable.intrinsicSize
            }

            override fun scheduleDrawable(d: Drawable, what: Runnable, time: Long) {
                MAIN_HANDLER.postAtTime(what, time)
            }

            override fun unscheduleDrawable(d: Drawable, what: Runnable) {
                MAIN_HANDLER.removeCallbacks(what)
            }
        }
    }

    init {
        if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
    }

    override fun onRemembered() {
        drawable.callback = callback
        drawable.setVisible(true, true)
        if (drawable is Animatable) drawable.start()
    }

    override fun onAbandoned() = onForgotten()

    override fun onForgotten() {
        if (drawable is Animatable) drawable.stop()
        drawable.setVisible(false, false)
        drawable.callback = null
    }

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            return drawable.setLayoutDirection(
                when (layoutDirection) {
                    LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
                    LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
                }
            )
        }
        return false
    }

    override val intrinsicSize: Size get() = drawableIntrinsicSize.value

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawInvalidateTick.value

            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())

            canvas.withSave {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}

private val MAIN_HANDLER by lazy(LazyThreadSafetyMode.NONE) {
    Handler(Looper.getMainLooper())
}

private val Drawable.intrinsicSize: Size
    get() = when {
        intrinsicWidth >= 0 && intrinsicHeight >= 0 -> {
            Size(width = intrinsicWidth.toFloat(), height = intrinsicHeight.toFloat())
        }
        else -> Size.Unspecified
    }

internal object EmptyPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified
    override fun DrawScope.onDraw() {}
}
