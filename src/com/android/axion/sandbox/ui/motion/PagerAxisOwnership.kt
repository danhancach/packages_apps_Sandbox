/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs

/**
 * Per-pointer axis ownership (GameSpace 658f023 → Compose pager).
 *
 * Vertical intent khoa doc den UP/CANCEL (ke ca X drift nhe).
 * Horizontal intent ro → pager tab — TRU khi [childHorizontalClaim].
 * Horizontal takeover: absDx > slop && absDx >= 1.5 * absDy (ke ca sau Vertical lock).
 * Pure — host sequence test dung chung production reducer.
 */
internal object PagerAxisOwnership {
    enum class Axis {
        Undecided,
        Vertical,
        Horizontal,
    }

    data class Config(
        val touchSlopPx: Float = 16f,
        /** Diagonal: |dominant| >= |secondary| * ratio moi lock (bang → uu tien doc). */
        val dominantRatio: Float = 1.0f,
        val horizontalTakeoverRatio: Float = Kc2HorizontalTakeoverRatio,
    )

    data class State(
        val pointerId: Long = -1L,
        val downX: Float = 0f,
        val downY: Float = 0f,
        val axis: Axis = Axis.Undecided,
        val active: Boolean = false,
        /** Slider/track / child requestDisallow — khong release pager horizontal. */
        val childHorizontalClaim: Boolean = false,
    )

    fun down(
        state: State,
        pointerId: Long,
        x: Float,
        y: Float,
        childHorizontalClaim: Boolean = false,
    ): State =
        State(
            pointerId = pointerId,
            downX = x,
            downY = y,
            axis = Axis.Undecided,
            active = true,
            childHorizontalClaim = childHorizontalClaim,
        )

    fun claimChildHorizontal(state: State): State {
        if (!state.active || state.childHorizontalClaim) return state
        return state.copy(childHorizontalClaim = true)
    }

    fun move(
        state: State,
        pointerId: Long,
        x: Float,
        y: Float,
        config: Config = Config(),
    ): State {
        if (!state.active || pointerId != state.pointerId) return state
        if (state.axis == Axis.Horizontal) return state

        val dx = x - state.downX
        val dy = y - state.downY
        val adx = abs(dx)
        val ady = abs(dy)
        val slop = config.touchSlopPx.coerceAtLeast(0f)
        if (adx <= slop && ady <= slop) return state

        val ratio = config.dominantRatio.coerceAtLeast(0.01f)
        val takeover = config.horizontalTakeoverRatio.coerceAtLeast(0.01f)
        val axis = when {
            adx > slop && adx >= ady * takeover -> Axis.Horizontal
            state.axis == Axis.Vertical -> Axis.Vertical
            ady >= adx * ratio && ady > slop -> Axis.Vertical
            adx >= ady * ratio && adx > slop -> Axis.Horizontal
            else -> Axis.Undecided
        }
        return state.copy(axis = axis)
    }

    fun up(state: State, pointerId: Long): State {
        if (!state.active) return state
        return if (pointerId == state.pointerId) reset() else state
    }

    fun cancel(state: State): State = reset()

    fun reset(): State = State()

    fun locksVertical(state: State): Boolean =
        state.active && state.axis == Axis.Vertical

    fun locksHorizontal(state: State): Boolean =
        state.active && state.axis == Axis.Horizontal && !state.childHorizontalClaim

    fun shouldClearTensionOnHorizontalTakeover(prevState: State, nextState: State): Boolean =
        locksHorizontal(nextState) && !locksHorizontal(prevState)

    /**
     * Same-gesture pager driver can thiet khi native HorizontalPager khong
     * nhan gesture (fling/busy/stretch hoac da khoa Vertical roi takeover).
     */
    fun shouldDriveSameGesturePager(
        flingAtDown: Boolean,
        contentBusyAtDown: Boolean,
        hadVerticalLock: Boolean,
    ): Boolean = flingAtDown || contentBusyAtDown || hadVerticalLock
}
