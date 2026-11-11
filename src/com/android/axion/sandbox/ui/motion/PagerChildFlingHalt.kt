/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity
import java.util.concurrent.CancellationException

/**
 * NestedScroll + [PagerAxisOwnership] Initial observe + same-gesture fallback.
 *
 * Quan sat MOI gesture (khong early-exit nonbusy DOWN). Vertical lock van
 * tiep tuc observe — horizontal takeover 1.5× cung gesture: halt fling, clear
 * stretch, drive pager (ke ca child da consume doc). Horizontal native thuan
 * (chua Vertical) → nhuong HorizontalPager.
 * [PagerAxisOwnership.childHorizontalClaim] / claimChildHorizontal: chan pager
 * lock (chua wire Slider call site).
 */
internal class PagerChildFlingHaltConnection : NestedScrollConnection {
    var childVerticalFling by mutableStateOf(false)
        private set

    var haltChild by mutableStateOf(false)
        private set

    var pointerArbitrationActive by mutableStateOf(false)

    fun setHalt(halt: Boolean) {
        haltChild = halt
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        haltChild = false
        childVerticalFling = available.y != 0f
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val claim =
            if (haltChild) {
                Velocity(0f, available.y)
            } else {
                Velocity.Zero
            }
        childVerticalFling = false
        haltChild = false
        return claim
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val y = available.y
        if (source == NestedScrollSource.UserInput && y != 0f) {
            if (pointerArbitrationActive) return Offset.Zero
            haltChild = false
            childVerticalFling = false
            return Offset.Zero
        }
        if (source == NestedScrollSource.SideEffect &&
            y != 0f &&
            childVerticalFling &&
            haltChild
        ) {
            childVerticalFling = false
            throw CancellationException("Child fling halted by pager gesture")
        }
        return Offset.Zero
    }
}

internal val LocalPagerChildFlingHalt =
    staticCompositionLocalOf<PagerChildFlingHaltConnection?> { null }

@Composable
internal fun Modifier.pagerChildFlingHalt(
    halt: PagerChildFlingHaltConnection = remember { PagerChildFlingHaltConnection() },
    pagerState: PagerState,
    mainTabFling: TargetedFlingBehavior,
    /** HorizontalPager reverseLayout — mac dinh false. */
    reverseScrolling: Boolean = false,
): Modifier {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val arbitration = LocalContentScrollArbitration.current
    val layoutDirection = LocalLayoutDirection.current
    val reverseDirection =
        SameGesturePagerOrchestration.horizontalReverseDirection(
            layoutDirection,
            reverseScrolling,
        )
    val reverseDirectionState = rememberUpdatedState(reverseDirection)
    val scope = rememberCoroutineScope()
    val sameGestureOwner =
        remember(pagerState, mainTabFling, scope) {
            SameGesturePagerScrollOwner(
                scope = scope,
                pagerState = pagerState,
                flingBehavior = mainTabFling,
                reverseDirection = { reverseDirectionState.value },
            )
        }

    return this
        .nestedScroll(halt)
        .pointerInput(touchSlop, arbitration, halt, sameGestureOwner) {
            try {
                awaitEachGesture {
                    val down =
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                    // Gesture moi: cat session/fling cu truoc ownership.
                    sameGestureOwner.cancelActiveSession()

                    val pointerId = down.id
                    val downPosition = down.position
                    val axisConfig = PagerAxisOwnership.Config(touchSlopPx = touchSlop)
                    var axisState = PagerAxisOwnership.down(
                        PagerAxisOwnership.reset(),
                        pointerId = pointerId.value,
                        x = downPosition.x,
                        y = downPosition.y,
                    )

                    val captured = arbitration?.captureActiveOwner()
                    val contentBusy = arbitration?.isOwnerBusy(captured) == true
                    val flingAtDown = halt.childVerticalFling
                    var hadVerticalLock = false
                    var deferred = false
                    var horizontalTaken = false
                    var fallbackGen = -1L
                    var lastX = downPosition.x
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPointerInputChange(down)

                    if (StretchBusyLifecycle.shouldArmHaltOnDown(flingAtDown)) {
                        halt.setHalt(true)
                    }
                    if (contentBusy) {
                        arbitration?.deferBusyPreserveTension(captured)
                        deferred = true
                    }
                    // Quan sat toan gesture — PreScroll khong xoa fling flag.
                    halt.pointerArbitrationActive = true
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }

                            // Missing pointer = cancel (KHONG fling bang velocity cu).
                            if (change == null) {
                                if (fallbackGen >= 0L) {
                                    sameGestureOwner.cancelGeneration(fallbackGen)
                                    fallbackGen = -1L
                                } else if (deferred) {
                                    arbitration?.endDeferPreserveTension(captured)
                                    arbitration?.resumeSettleIfNeeded(captured)
                                    deferred = false
                                }
                                halt.setHalt(false)
                                axisState = PagerAxisOwnership.cancel(axisState)
                                break
                            }

                            if (!change.pressed) {
                                velocityTracker.addPointerInputChange(change)
                                if (fallbackGen >= 0L) {
                                    val x = change.position.x
                                    val finalDx = x - lastX
                                    if (finalDx != 0f) {
                                        sameGestureOwner.dragFingerDeltaX(finalDx, fallbackGen)
                                        lastX = x
                                    }
                                    val vx = velocityTracker.calculateVelocity().x
                                    sameGestureOwner.releaseFingerVelocityX(vx, fallbackGen)
                                    fallbackGen = -1L
                                } else if (deferred) {
                                    arbitration?.endDeferPreserveTension(captured)
                                    arbitration?.resumeSettleIfNeeded(captured)
                                    deferred = false
                                }
                                halt.setHalt(false)
                                axisState = PagerAxisOwnership.up(axisState, pointerId.value)
                                break
                            }

                            velocityTracker.addPointerInputChange(change)

                            if (fallbackGen >= 0L) {
                                change.consume()
                                val x = change.position.x
                                sameGestureOwner.dragFingerDeltaX(x - lastX, fallbackGen)
                                lastX = x
                                continue
                            }

                            val prev = axisState
                            axisState = PagerAxisOwnership.move(
                                axisState,
                                pointerId = pointerId.value,
                                x = change.position.x,
                                y = change.position.y,
                                config = axisConfig,
                            )
                            if (PagerAxisOwnership.locksVertical(axisState)) {
                                hadVerticalLock = true
                                halt.setHalt(false)
                                if (deferred) {
                                    arbitration?.endDeferPreserveTension(captured)
                                    deferred = false
                                }
                            }

                            if (PagerAxisOwnership.shouldClearTensionOnHorizontalTakeover(
                                    prev,
                                    axisState,
                                )
                            ) {
                                halt.setHalt(true)
                                arbitration?.confirmHorizontalTakeover(captured)
                                deferred = false
                                horizontalTaken = true

                                val driveFallback =
                                    SameGesturePagerOrchestration.shouldUseFallbackOwner(
                                        flingAtDown,
                                        contentBusy,
                                        hadVerticalLock,
                                    )
                                if (driveFallback) {
                                    val curX = change.position.x
                                    val overSlop =
                                        SameGesturePagerOrchestration.overSlopFingerDelta(
                                            downPosition.x,
                                            curX,
                                            touchSlop,
                                        )
                                    fallbackGen = sameGestureOwner.beginSession()
                                    if (overSlop != 0f) {
                                        sameGestureOwner.dragFingerDeltaX(
                                            overSlop,
                                            fallbackGen,
                                        )
                                    }
                                    lastX = curX
                                    change.consume()
                                } else {
                                    // Horizontal native thuan — nhuong HorizontalPager.
                                    return@awaitEachGesture
                                }
                            }
                            // Vertical / Undecided: khong consume, khong exit —
                            // tiep tuc observe de cho phep takeover 1.5×.
                        }
                    } finally {
                        halt.pointerArbitrationActive = false
                        if (fallbackGen >= 0L) {
                            sameGestureOwner.cancelGeneration(fallbackGen)
                            fallbackGen = -1L
                        } else if (deferred && !horizontalTaken) {
                            arbitration?.endDeferPreserveTension(captured)
                            arbitration?.resumeSettleIfNeeded(captured)
                        }
                    }
                }
            } finally {
                sameGestureOwner.cancelActiveSession()
            }
        }
}
