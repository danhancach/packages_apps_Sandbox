/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Shared Folme retarget cho compact title (main-tab + submenu).
 * Geometry / typography / Back spacing van nam o tung TopAppBar — khong gop layout.
 */
internal data class MiuixSmallTitleFolmeState(
    val alpha: State<Float>,
    val translationY: State<Float>,
)

@Composable
internal fun rememberMiuixSmallTitleFolme(
    scrollStateKey: Any?,
    collapsedFraction: () -> Float,
): MiuixSmallTitleFolmeState {
    val seedVisible = MiuixTitleMotion.isSmallTitleVisible(collapsedFraction())
    val smallAlpha = remember {
        Animatable(if (seedVisible) 1f else 0f)
    }
    val smallTranslationY = remember {
        Animatable(
            if (seedVisible) 0f else MiuixTitleMotion.TransitionDistancePx,
        )
    }

    LaunchedEffect(scrollStateKey) {
        var animJob: Job? = null
        snapshotFlow { collapsedFraction() }
            .map { MiuixTitleMotion.isSmallTitleVisible(it) }
            .distinctUntilChanged()
            .collect { visible ->
                val retainedA = smallAlpha.velocity
                val retainedY = smallTranslationY.velocity
                animJob?.cancelAndJoin()
                val spec = spring(
                    dampingRatio = MiuixTitleMotion.DampingRatio,
                    stiffness = MiuixTitleMotion.stiffness(visible),
                    visibilityThreshold = MiuixTitleMotion.Threshold,
                )
                val targetA = if (visible) 1f else 0f
                val targetY =
                    if (visible) 0f else MiuixTitleMotion.TransitionDistancePx
                animJob = launch {
                    coroutineScope {
                        launch {
                            smallAlpha.animateTo(
                                targetValue = targetA,
                                animationSpec = spec,
                                initialVelocity = retainedA,
                            )
                        }
                        launch {
                            smallTranslationY.animateTo(
                                targetValue = targetY,
                                animationSpec = spec,
                                initialVelocity = retainedY,
                            )
                        }
                    }
                }
            }
    }

    return MiuixSmallTitleFolmeState(
        alpha = smallAlpha.asState(),
        translationY = smallTranslationY.asState(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal suspend fun settleMiuixAppBarScroll(
    scrollBehavior: TopAppBarScrollBehavior,
    velocity: Float,
) {
    val state = scrollBehavior.state
    val snap = scrollBehavior.snapAnimationSpec ?: return
    val offset = state.heightOffset
    val limit = state.heightOffsetLimit
    if (offset == 0f || offset == limit) return
    val target = if (state.collapsedFraction < 0.5f) 0f else limit
    animate(
        initialValue = offset,
        targetValue = target,
        initialVelocity = velocity,
        animationSpec = snap,
    ) { value, _ ->
        state.heightOffset = value
    }
}
