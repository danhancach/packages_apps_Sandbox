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

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.android.axion.sandbox.ui.motion.LocalContentScrollArbitration
import com.android.axion.sandbox.ui.motion.RememberStretchArbitrationRegistration
import com.android.axion.sandbox.ui.motion.MiuixSubmenuTopAppBar
import com.android.axion.sandbox.ui.motion.rememberMiuixHeaderContentScrollBehavior
import com.android.axion.sandbox.ui.motion.rememberMiuixVerticalStretchOverscrollFactory
import androidx.compose.foundation.LocalOverscrollFactory

/**
 * Submenu shared (Settings / App detail): Folme title + header/list chung 1 overscroll.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SandboxSubmenuScaffold(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    content: @Composable SandboxSubmenuScrollScope.() -> Unit,
) {
    // Factory Miuix rieng cho submenu (ngoai pager shell) — khong de Local null.
    val overscrollFactory = rememberMiuixVerticalStretchOverscrollFactory()
    CompositionLocalProvider(
        LocalOverscrollFactory provides overscrollFactory,
        LocalContentScrollArbitration provides remember { com.android.axion.sandbox.ui.motion.ContentScrollArbitration() },
    ) {
        val overscroll = rememberOverscrollEffect()
        RememberStretchArbitrationRegistration(overscroll)
        val canScrollForward = listState.canScrollForward
        val canScrollBackward = listState.canScrollBackward
        val scrollBehavior = rememberMiuixHeaderContentScrollBehavior(
            state = rememberTopAppBarState(),
            overscrollEffect = overscroll,
            canContentScroll = { canScrollForward || canScrollBackward },
            shortStretch = true,
        )
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            // Man hinh submenu khong co bottom bar: nen thanh cu chi dieu huong trong suot theo chuan edge-to-edge
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = {
                MiuixSubmenuTopAppBar(
                    title = { Text(title) },
                    onBack = onBackClick,
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            ) {
                val scope = remember(listState, overscroll) {
                    SandboxSubmenuScrollScope(listState, overscroll)
                }
                scope.content()
            }
        }
    }
}

/** Slot scroll: child LazyColumn dung chung state + effect voi header. */
class SandboxSubmenuScrollScope internal constructor(
    val listState: LazyListState,
    val listOverscrollEffect: OverscrollEffect?,
) {
    val contentPadding: PaddingValues = PaddingValues()
}
