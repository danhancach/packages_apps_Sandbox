/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs

/**
 * Ownership stretch vs pager: suppress isInProgress khi defer, giu E;
 * horizontal takeover clear E. Production state dung chung effect + JVM test.
 */
internal class StretchBusyDeferGate(
    private val epsilon: Float = 0.5f,
) {
    var visualE: Float = 0f
        private set

    var suppressInProgress: Boolean = false
        private set

    fun isInProgress(): Boolean = !suppressInProgress && abs(visualE) > epsilon

    fun setVisualE(e: Float) {
        visualE = e
    }

    /** DOWN busy: an isInProgress, giu E. */
    fun deferBusyPreserveTension() {
        suppressInProgress = true
    }

    /** Vertical / UP chua takeover: bo suppress, E giu. */
    fun endDeferPreserveTension() {
        suppressInProgress = false
    }

    /** Horizontal xac nhan: E=0, bo suppress. */
    fun confirmHorizontalTakeoverClearTension() {
        visualE = 0f
        suppressInProgress = false
    }
}
