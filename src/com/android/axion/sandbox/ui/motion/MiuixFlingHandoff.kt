/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs
import kotlin.math.sign

/**
 * rb2.F0/I0 velocity allocate — dung chung production applyToFling + unit test.
 * Khong Compose; khong damping/position clamp.
 *
 * Sequence (NestedScroll F0 roi child roi I0):
 *  F0: ee3.c; A1(c) khi |E|>B (chay song song child fling)
 *  I0: ee3.c; A1(leftover/1.53333)
 * Same-sign stale: pre-claim full → child 0 → I0 leftover 0 → A1(0).
 */
internal object MiuixFlingHandoff {
    /** rb2.B */
    const val VisualEpsilonPx = 1.0f

    /** I0: leftover / 1.53333 → A1 */
    const val PostFlingVelocityDivisor = 1.53333f

    /** F0 doi dau: bao cao consume / 2.13333 (A1 net van I0) */
    const val PreFlingOppositeConsumeDivisor = 2.13333f

    /**
     * rb2.F0 bao cao consume truoc performFling / ah.f.
     * Cung dau E: full vy; doi dau: vy/2.13333; khong stretch: 0.
     */
    fun flingPreConsumedY(visualE: Float, flingVy: Float): Float {
        if (abs(visualE) <= VisualEpsilonPx || flingVy == 0f) return 0f
        return if (sign(flingVy) == sign(visualE)) {
            flingVy
        } else {
            flingVy / PreFlingOppositeConsumeDivisor
        }
    }

    /**
     * rb2.I0 → A1 settle velocity sau child/parent fling.
     * null = khong goi A1 (E~0 va leftover~0).
     * leftover==0 + dang E → 0f (huy stale F0 full-v).
     */
    fun flingSettleVelocity(visualE: Float, leftoverY: Float): Float? {
        when {
            leftoverY != 0f -> return leftoverY / PostFlingVelocityDivisor
            abs(visualE) > VisualEpsilonPx -> return 0f
            else -> return null
        }
    }

    /**
     * Mo phong merge applyToFling: F0 A1(vy) neu |E|>eps → child → I0.
     * [childConsumedY] = luong performFling bao cao (0 = short Zero).
     */
    data class FlingSequence(
        val preConsumedY: Float,
        val toChildY: Float,
        val f0SettleVy: Float?,
        val leftoverY: Float,
        val i0SettleVy: Float?,
    )

    fun simulateFlingSequence(
        visualE: Float,
        flingVy: Float,
        childConsumedY: Float = 0f,
    ): FlingSequence {
        val pre = flingPreConsumedY(visualE, flingVy)
        val toChild = flingVy - pre
        val f0 = if (abs(visualE) > VisualEpsilonPx) flingVy else null
        val leftover = toChild - childConsumedY
        val i0 = flingSettleVelocity(visualE, leftover)
        return FlingSequence(pre, toChild, f0, leftover, i0)
    }
}
