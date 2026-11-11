/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Mot NestedScrollConnection owner: tension → header (LSPosed li0/ni0 =
 * ExitUntilCollapsed) → content unconsumed → short stretch.
 *
 * Provenance APK LSPosed manager 2.2.1 SHA256
 * 9da041436708491e3767098984c8f4391241c7c491f0c8d051167ad3afa8c137:
 * - Header: jq3.a → ni0 + li0(g=1) R0/D0/I0 (khop M3 ExitUntilCollapsed)
 * - z1/rb2: chi gate overscroll vs pull (hm2), KHONG arbitrage header/list
 *
 * Thay bridge short *ngoai* exitUntilCollapsed.nestedScroll — PreScroll
 * applyToScroll(perform=0) vua reverse vua v1 → an delta UP truoc header
 * (content/stretch khi header chua collapse xong); DOWN expand le.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberMiuixHeaderContentScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    overscrollEffect: OverscrollEffect?,
    canContentScroll: () -> Boolean,
    /**
     * true: PostScroll/Fling short feed cung effect khi !canContentScroll
     * (jb2.b short). false: busy/AlwaysStretch — effect nam trong scrollable.
     */
    shortStretch: Boolean = true,
    canHeaderScroll: () -> Boolean = { true },
): TopAppBarScrollBehavior {
    // Snap/fling/settle giu dung exitUntilCollapsed (khong doi motion constants).
    val base = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = state,
        canScroll = canHeaderScroll,
    )
    val canContentUpdated = rememberUpdatedState(canContentScroll)
    val canHeaderUpdated = rememberUpdatedState(canHeaderScroll)
    val shortUpdated = rememberUpdatedState(shortStretch)
    return remember(base, overscrollEffect) {
        MiuixHeaderContentScrollBehavior(
            delegate = base,
            overscrollEffect = overscrollEffect,
            canContentScroll = { canContentUpdated.value.invoke() },
            canHeaderScroll = { canHeaderUpdated.value.invoke() },
            shortStretch = { shortUpdated.value },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class MiuixHeaderContentScrollBehavior(
    private val delegate: TopAppBarScrollBehavior,
    private val overscrollEffect: OverscrollEffect?,
    private val canContentScroll: () -> Boolean,
    private val canHeaderScroll: () -> Boolean,
    private val shortStretch: () -> Boolean,
) : TopAppBarScrollBehavior {
    override val state: TopAppBarState
        get() = delegate.state
    override val isPinned: Boolean
        get() = delegate.isPinned
    override val snapAnimationSpec: AnimationSpec<Float>?
        get() = delegate.snapAnimationSpec
    override val flingAnimationSpec: DecayAnimationSpec<Float>?
        get() = delegate.flingAnimationSpec

    override val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            private val header = delegate.nestedScrollConnection

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                var remainingY = available.y
                var claimedY = 0f

                // 1) Tension reverse ONLY (rb2 R0 opposite) — khong v1 stretch o Pre.
                // Gate UserInput intrinsic trong releaseOppositeTension (SideEffect
                // fling settle khong tu-huy).
                val effect = overscrollEffect
                if (effect is MiuixVerticalStretchOverscrollEffect &&
                    remainingY != 0f
                ) {
                    val released =
                        effect.releaseOppositeTension(remainingY, source)
                    if (released != 0f) {
                        claimedY += released
                        remainingY -= released
                    }
                }

                if (remainingY == 0f) {
                    return if (claimedY == 0f) Offset.Zero else Offset(0f, claimedY)
                }

                // 2) Header li0.R0 / M3 ExitUntilCollapsed PreScroll.
                // M3 ExitUntilCollapsed.onPreScroll da tra ve available.y (full Y)
                // khi heightOffset doi — khong can branch claim FULL rieng.
                if (!canHeaderScroll()) {
                    return if (claimedY == 0f) Offset.Zero else Offset(0f, claimedY)
                }
                val headerConsumed =
                    header.onPreScroll(Offset(0f, remainingY), source)
                claimedY += headerConsumed.y
                return if (claimedY == 0f) Offset.Zero else Offset(0f, claimedY)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // 3) Header li0.D0: UP track consumed; DOWN expand tu available.
                var headerClaim = Offset.Zero
                if (canHeaderScroll()) {
                    headerClaim = header.onPostScroll(consumed, available, source)
                }
                var remainingY = available.y - headerClaim.y
                var claimedY = headerClaim.y

                // 4) Short stretch Post (bridge cu) — chi khi !canContentScroll.
                val effect = overscrollEffect
                if (shortStretch() &&
                    effect != null &&
                    !canContentScroll() &&
                    remainingY != 0f
                ) {
                    val yOnly = Offset(0f, remainingY)
                    val applied = effect.applyToScroll(yOnly, source) { Offset.Zero }
                    claimedY += applied.y
                }
                return if (claimedY == 0f) Offset.Zero else Offset(0f, claimedY)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return header.onPreFling(available)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                // Header settle (I0 path qua exitUntilCollapsed onPostFling).
                val headerClaim = header.onPostFling(consumed, available)
                var remainingY = available.y - headerClaim.y
                var claimedY = headerClaim.y

                val effect = overscrollEffect
                if (shortStretch() && effect != null && !canContentScroll()) {
                    val vy = remainingY
                    if (vy != 0f || effect.isInProgress) {
                        effect.applyToFling(Velocity(0f, vy)) { Velocity.Zero }
                        claimedY += vy
                        remainingY = 0f
                    }
                }
                return if (claimedY == 0f) {
                    Velocity.Zero
                } else {
                    Velocity(0f, claimedY)
                }
            }
        }
}
