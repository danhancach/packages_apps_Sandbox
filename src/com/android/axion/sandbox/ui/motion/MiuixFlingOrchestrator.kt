/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs

/**
 * F0→performFling→I0 voi live E + operation ownership.
 * Obsolete child (generation cu) khong duoc cancel/restart settle moi.
 */
internal object MiuixFlingOrchestrator {
    data class Plan(
        val preConsumedY: Float,
        val toChildY: Float,
        val f0SettleVy: Float?,
    )

    fun plan(visualE: Float, flingVy: Float): Plan {
        val pre = MiuixFlingHandoff.flingPreConsumedY(visualE, flingVy)
        val toChild = flingVy - pre
        val f0 =
            if (abs(visualE) > MiuixFlingHandoff.VisualEpsilonPx) {
                flingVy
            } else {
                null
            }
        return Plan(pre, toChild, f0)
    }

    /**
     * @param readVisualE doc E hien tai (sau child co the doi)
     * @param beginOperation cap generation moi; tra opId
     * @param isCurrentOperation opId con so huu
     */
    suspend fun runApplyToFling(
        readVisualE: () -> Float,
        flingVy: Float,
        beginOperation: () -> Long,
        isCurrentOperation: (Long) -> Boolean,
        startSettle: (Float) -> Unit,
        cancelSettle: () -> Unit,
        performFlingChild: suspend (toChildY: Float) -> Float,
    ): Float? {
        val entryE = readVisualE()
        val p = plan(entryE, flingVy)
        val opId = beginOperation()
        cancelSettle()
        var childConsumedY = 0f
        try {
            if (p.f0SettleVy != null) {
                startSettle(p.f0SettleVy)
            }
            childConsumedY = performFlingChild(p.toChildY)
        } catch (t: Throwable) {
            if (isCurrentOperation(opId)) {
                cancelSettle()
            }
            throw t
        }
        // Obsolete: gesture moi / detach da invalidate — khong I0, khong cancel settle moi.
        if (!isCurrentOperation(opId)) {
            return null
        }
        cancelSettle()
        val liveE = readVisualE()
        val leftoverY = p.toChildY - childConsumedY
        val i0 = MiuixFlingHandoff.flingSettleVelocity(liveE, leftoverY)
        if (i0 != null) {
            startSettle(i0)
        }
        return i0
    }
}
