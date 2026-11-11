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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.android.axion.sandbox.R
import com.android.axion.sandbox.ui.motion.ContentScrollArbitration
import com.android.axion.sandbox.ui.motion.LocalContentScrollArbitration
import com.android.axion.sandbox.ui.motion.LocalPagerChildFlingHalt
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerPageIndex
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerSettled
import com.android.axion.sandbox.ui.motion.LocalSandboxPagerVisiblePage
import com.android.axion.sandbox.ui.motion.PagerChildFlingHaltConnection
import com.android.axion.sandbox.ui.motion.RememberStretchArbitrationRegistration
import com.android.axion.sandbox.ui.motion.SequentialFadeLargeFlexibleTopAppBar
import com.android.axion.sandbox.ui.motion.pagerChildFlingHalt
import com.android.axion.sandbox.ui.motion.rememberMiuixHeaderContentScrollBehavior
import com.android.axion.sandbox.ui.motion.rememberMiuixVerticalStretchOverscrollFactory
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.rememberOverscrollEffect
import kotlinx.coroutines.launch

/** Chieu cao bottom chrome (nav + inset) do layout do — content dung gia tri nay. */
val LocalSandboxBottomChromeHeight = staticCompositionLocalOf { 0.dp }

/** Parent shell dang interactive (khong settings/detail overlay). */
val LocalSandboxRootInteractive = staticCompositionLocalOf { true }

/**
 * Shell 3 tab: moi trang so huu Scaffold/app bar + scroll/stretch owner;
 * bottom nav o slot do chieu cao that; pager chi nhan phan viewport con lai.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SandboxMainShell(
    apps: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    isPrivateUnlocked: Boolean,
    onUnlockRequest: () -> Unit,
    isPrivateAreaExpanded: Boolean,
    onPrivateAreaExpandChange: (Boolean) -> Unit,
    isSecuritySetup: Boolean,
    onSetupSecurity: () -> Unit,
    selectedTabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    notificationCount: Int,
    onPickingFilesChange: (Boolean) -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    searchBackEnabled: Boolean = true,
    rootInteractive: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex.coerceIn(0, 2),
        pageCount = { 3 },
    )
    LaunchedEffect(pagerState.currentPage) {
        onTabIndexChange(pagerState.currentPage)
    }
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != pagerState.currentPage &&
            selectedTabIndex in 0..2 &&
            !pagerState.isScrollInProgress
        ) {
            pagerState.scrollToPage(selectedTabIndex)
        }
    }

    // Mot flag interactive tu parent + search expanded.
    BackHandler(enabled = rootInteractive && searchBackEnabled && isSearchExpanded) {
        onSearchExpandedChange(false)
        onSearchQueryChange("")
    }

    val pageBg = MaterialTheme.colorScheme.surfaceContainer
    val fling = PagerDefaults.flingBehavior(state = pagerState)
    val halt = remember { PagerChildFlingHaltConnection() }
    val arbitration = remember { ContentScrollArbitration() }
    val overscrollFactory = rememberMiuixVerticalStretchOverscrollFactory()
    var bottomChromeHeight by remember { mutableStateOf(SandboxBottomTabRowHeight) }
    val pagerSettled = !pagerState.isScrollInProgress

    CompositionLocalProvider(
        LocalPagerChildFlingHalt provides halt,
        LocalContentScrollArbitration provides arbitration,
        LocalSandboxPagerVisiblePage provides pagerState.currentPage,
        LocalSandboxPagerSettled provides pagerSettled,
        LocalOverscrollFactory provides overscrollFactory,
        LocalSandboxBottomChromeHeight provides bottomChromeHeight,
        LocalSandboxRootInteractive provides rootInteractive,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fade overlay trong viewport content (khong Column child 48dp).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pagerChildFlingHalt(
                            halt = halt,
                            pagerState = pagerState,
                            mainTabFling = fling,
                        ),
                    beyondViewportPageCount = 1,
                    flingBehavior = fling,
                ) { page ->
                    CompositionLocalProvider(LocalSandboxPagerPageIndex provides page) {
                        when (page) {
                            0 -> AppsPage(
                                apps = apps,
                                isLoading = isLoading,
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSearchQueryChange,
                                isSearchExpanded = isSearchExpanded,
                                onSearchExpandedChange = onSearchExpandedChange,
                                onSettingsClick = onSettingsClick,
                                isPrivateUnlocked = isPrivateUnlocked,
                                onUnlockRequest = onUnlockRequest,
                                isPrivateAreaExpanded = isPrivateAreaExpanded,
                                onPrivateAreaExpandChange = onPrivateAreaExpandChange,
                                isSecuritySetup = isSecuritySetup,
                                onSetupSecurity = onSetupSecurity,
                                onAppSelected = onAppSelected,
                            )
                            1 -> NotificationsPage(
                                isUnlocked = isPrivateUnlocked,
                                onUnlockRequest = onUnlockRequest,
                                onSettingsClick = onSettingsClick,
                            )
                            else -> VaultPage(
                                isUnlocked = isPrivateUnlocked,
                                onUnlockRequest = onUnlockRequest,
                                onSettingsClick = onSettingsClick,
                                onPickingFilesChange = onPickingFilesChange,
                            )
                        }
                    }
                }
            }
            SandboxBottomBar(
                selectedPagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                notificationCount = notificationCount,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        bottomChromeHeight = with(density) { size.height.toDp() }
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppsPage(
    apps: List<AppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    isPrivateUnlocked: Boolean,
    onUnlockRequest: () -> Unit,
    isPrivateAreaExpanded: Boolean,
    onPrivateAreaExpandChange: (Boolean) -> Unit,
    isSecuritySetup: Boolean,
    onSetupSecurity: () -> Unit,
    onAppSelected: (AppInfo) -> Unit,
) {
    // Mot effect/node owner: header → content → short stretch (cung LocalOverscrollFactory).
    val overscroll = rememberOverscrollEffect()
    RememberStretchArbitrationRegistration(overscroll)
    val listState = rememberLazyListState()
    val useAlwaysStretch = isLoading || apps.isEmpty()
    val canForward = listState.canScrollForward
    val canBackward = listState.canScrollBackward
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = rememberMiuixHeaderContentScrollBehavior(
        state = topBarState,
        overscrollEffect = overscroll,
        canContentScroll = {
            if (useAlwaysStretch) false else (canForward || canBackward)
        },
        shortStretch = true,
    )
    val collapsed = scrollBehavior.state.collapsedFraction
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Loai bo insets day vi SandboxBottomBar o duoi da quan ly navigation insets
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            SequentialFadeLargeFlexibleTopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    } else {
                        Text(stringResource(R.string.tab_apps))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    FilledIconToggleButton(
                        checked = isSearchExpanded,
                        onCheckedChange = {
                            onSearchExpandedChange(it)
                            if (!it) onSearchQueryChange("")
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_search),
                        )
                    }
                    FilledIconButton(
                        onClick = onSettingsClick,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppsTab(
                apps = apps,
                isLoading = isLoading,
                onAppClick = onAppSelected,
                isPrivateAreaExpanded = isPrivateAreaExpanded,
                onPrivateAreaExpandChange = onPrivateAreaExpandChange,
                onUnlockRequest = onUnlockRequest,
                isPrivateUnlocked = isPrivateUnlocked,
                isSecuritySetup = isSecuritySetup,
                onSetupSecurity = onSetupSecurity,
                contentPadding = PaddingValues(bottom = 0.dp),
                listOverscrollEffect = overscroll,
                listState = listState,
                useAlwaysStretch = useAlwaysStretch,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationsPage(
    isUnlocked: Boolean,
    onUnlockRequest: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val overscroll = rememberOverscrollEffect()
    RememberStretchArbitrationRegistration(overscroll)
    val listState = rememberLazyListState()
    // Locked/empty: short path; list state van hoist de share effect.
    val useAlwaysStretch = !isUnlocked
    val canForward = listState.canScrollForward
    val canBackward = listState.canScrollBackward
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = rememberMiuixHeaderContentScrollBehavior(
        state = topBarState,
        overscrollEffect = overscroll,
        canContentScroll = {
            if (useAlwaysStretch) false else (canForward || canBackward)
        },
        shortStretch = true,
    )
    val collapsed = scrollBehavior.state.collapsedFraction
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Loai bo insets day vi SandboxBottomBar o duoi da quan ly navigation insets
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            SequentialFadeLargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.tab_notifications)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    FilledIconButton(
                        onClick = onSettingsClick,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NotificationsTab(
                isUnlocked = isUnlocked,
                onUnlockRequest = onUnlockRequest,
                contentPadding = PaddingValues(bottom = 0.dp),
                listOverscrollEffect = overscroll,
                listState = listState,
                useAlwaysStretch = useAlwaysStretch,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VaultPage(
    isUnlocked: Boolean,
    onUnlockRequest: () -> Unit,
    onSettingsClick: () -> Unit,
    onPickingFilesChange: (Boolean) -> Unit,
) {
    val overscroll = rememberOverscrollEffect()
    RememberStretchArbitrationRegistration(overscroll)
    val gridState = rememberLazyGridState()
    val useAlwaysStretch = !isUnlocked
    val canForward = gridState.canScrollForward
    val canBackward = gridState.canScrollBackward
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = rememberMiuixHeaderContentScrollBehavior(
        state = topBarState,
        overscrollEffect = overscroll,
        canContentScroll = {
            if (useAlwaysStretch) false else (canForward || canBackward)
        },
        shortStretch = true,
    )
    val collapsed = scrollBehavior.state.collapsedFraction
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Loai bo insets day vi SandboxBottomBar o duoi da quan ly navigation insets
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            SequentialFadeLargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.tab_vault)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    FilledIconButton(
                        onClick = onSettingsClick,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            VaultTab(
                isUnlocked = isUnlocked,
                onUnlockRequest = onUnlockRequest,
                onPickingFilesChange = onPickingFilesChange,
                contentPadding = PaddingValues(bottom = 0.dp),
                listOverscrollEffect = overscroll,
                nestInsideOuterScaffold = true,
                gridState = gridState,
                useAlwaysStretch = useAlwaysStretch,
            )
        }
    }
}
