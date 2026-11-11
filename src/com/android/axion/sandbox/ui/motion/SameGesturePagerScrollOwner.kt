/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Same-gesture pager fallback owner (Vertical lock / fling / stretch-busy DOWN
 * roi horizontal takeover qua [PagerAxisOwnership]).
 *
 * Pointer await chi trySend; [PagerState.scroll] + [TargetedFlingBehavior.performFling]
 * qua [SameGesturePagerSessionRunner] (SSOT — unit test goi cung runner).
 *
 * [reverseDirection]: [ScrollableDefaults.reverseDirection] (HorizontalPager
 * reverseLayout → reverseScrolling).
 */
internal class SameGesturePagerScrollOwner(
    private val scope: CoroutineScope,
    private val pagerState: PagerState,
    private val flingBehavior: TargetedFlingBehavior,
    private val reverseDirection: () -> Boolean,
) {
    private val commands = Channel<SameGesturePagerCmd>(Channel.UNLIMITED)
    private var sessionJob: Job? = null
    private var generation = 0L

    val activeGeneration: Long
        get() = generation

    val isSessionActive: Boolean
        get() = sessionJob?.isActive == true

    fun beginSession(): Long {
        cancelGeneration(generation)
        val gen = ++generation
        sessionJob =
            scope.launch {
                try {
                    pagerState.scroll(MutatePriority.UserInput) {
                        SameGesturePagerSessionRunner.run(
                            gen = gen,
                            receive = { commands.receive() },
                            scrollBy = { scrollBy(it) },
                            performFling = { v ->
                                with(flingBehavior) { performFling(v) }
                            },
                        )
                    }
                } finally {
                    if (generation == gen) {
                        sessionJob = null
                    }
                }
            }
        return gen
    }

    fun dragFingerDeltaX(fingerDeltaX: Float, gen: Long) {
        if (gen != generation) return
        commands.trySend(
            SameGesturePagerCmd.Drag(
                gen,
                SameGesturePagerOrchestration.fingerDeltaToScrollDelta(
                    fingerDeltaX,
                    reverseDirection(),
                ),
            ),
        )
    }

    fun releaseFingerVelocityX(fingerVelocityX: Float, gen: Long) {
        if (gen != generation) return
        commands.trySend(
            SameGesturePagerCmd.Release(
                gen,
                SameGesturePagerOrchestration.fingerVelocityToFlingVelocity(
                    fingerVelocityX,
                    reverseDirection(),
                ),
            ),
        )
    }

    /** Huy dung generation — khong cancel job gen moi hon. */
    fun cancelGeneration(gen: Long) {
        if (gen <= 0L) return
        commands.trySend(SameGesturePagerCmd.Cancel(gen))
        if (generation == gen) {
            sessionJob?.cancel()
            sessionJob = null
        }
    }

    fun cancelActiveSession() {
        cancelGeneration(generation)
    }
}

internal sealed class SameGesturePagerCmd {
    data class Drag(val gen: Long, val scrollDelta: Float) : SameGesturePagerCmd()
    data class Release(val gen: Long, val flingVelocity: Float) : SameGesturePagerCmd()
    data class Cancel(val gen: Long) : SameGesturePagerCmd()
}

/**
 * SSOT session loop — owner production + unit test dung chung.
 */
internal object SameGesturePagerSessionRunner {
    suspend fun run(
        gen: Long,
        receive: suspend () -> SameGesturePagerCmd,
        scrollBy: (Float) -> Unit,
        performFling: suspend (Float) -> Unit,
    ) {
        while (true) {
            when (val cmd = receive()) {
                is SameGesturePagerCmd.Drag -> {
                    if (cmd.gen != gen) continue
                    scrollBy(cmd.scrollDelta)
                }
                is SameGesturePagerCmd.Release -> {
                    if (cmd.gen != gen) continue
                    performFling(cmd.flingVelocity)
                    return
                }
                is SameGesturePagerCmd.Cancel -> {
                    if (cmd.gen != gen) continue
                    return
                }
            }
        }
    }
}

internal object SameGesturePagerOrchestration {
    /**
     * Goi thang library — khong nhan doi cong thuc.
     * HorizontalPager: reverseScrolling = reverseLayout.
     */
    fun horizontalReverseDirection(
        layoutDirection: LayoutDirection,
        reverseScrolling: Boolean = false,
    ): Boolean =
        ScrollableDefaults.reverseDirection(
            layoutDirection,
            Orientation.Horizontal,
            reverseScrolling,
        )

    fun fingerDeltaToScrollDelta(fingerDeltaX: Float, reverseDirection: Boolean): Float =
        if (reverseDirection) -fingerDeltaX else fingerDeltaX

    fun fingerVelocityToFlingVelocity(fingerVelocityX: Float, reverseDirection: Boolean): Float =
        if (reverseDirection) -fingerVelocityX else fingerVelocityX

    /**
     * Excess qua touchSlop (cung dau total) — native-equivalent sau commit goc.
     */
    fun overSlopFingerDelta(downX: Float, currentX: Float, touchSlop: Float): Float {
        val total = currentX - downX
        if (abs(total) <= touchSlop) return 0f
        return total - touchSlop * sign(total)
    }

    /**
     * Overload 1-arg (unit-test / legacy): true chi khi fling doc luc DOWN.
     * Production dung overload 3-arg.
     */
    fun shouldUseFallbackOwner(childVerticalFlingAtDown: Boolean): Boolean =
        childVerticalFlingAtDown

    /**
     * Production driver cung gesture: fling/busy luc DOWN, hoac da khoa Vertical
     * roi horizontal takeover ([PagerAxisOwnership]).
     */
    fun shouldUseFallbackOwner(
        childVerticalFlingAtDown: Boolean,
        contentBusyAtDown: Boolean,
        hadVerticalLock: Boolean,
    ): Boolean =
        PagerAxisOwnership.shouldDriveSameGesturePager(
            flingAtDown = childVerticalFlingAtDown,
            contentBusyAtDown = contentBusyAtDown,
            hadVerticalLock = hadVerticalLock,
        )
}
