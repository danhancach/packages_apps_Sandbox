/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs

/**
 * Lifecycle stretch busy ↔ pager (khong Compose) — B1 interrupt truoc defer.
 * Production effect goi [reacquireAfterSettleInterrupt]; JVM harness cung path.
 */
internal object StretchBusyLifecycle {
    /**
     * Sau cancel settle (rawF thuong = 0 trong od3): neu con E → B1 reacquire.
     * Tra null neu khong can doi state.
     */
    fun reacquireAfterSettleInterrupt(
        rawF: Float,
        visualE: Float,
        boundG: Float,
        epsilon: Float = MiuixStretchMath.VisualEpsilonPx,
    ): MiuixStretchReacquire.State? {
        if (rawF != 0f) return null
        if (abs(visualE) <= epsilon) return null
        return MiuixStretchReacquire.afterInterruptB1(visualE, boundG)
    }

    /** DOWN + childVerticalFling → arm halt ngay (truoc angle). */
    fun shouldArmHaltOnDown(childVerticalFling: Boolean): Boolean = childVerticalFling
}
