/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs

/**
 * Legacy kc2 angle helper + [Kc2HorizontalTakeoverRatio] SSOT.
 *
 * Live Compose path dung [PagerAxisOwnership] (khong goi [kc2AngleCommit]).
 * Giu helper cho host fixtures / unit-test doi chieu 1.5x vs 2x cu.
 *
 * Horizontal takeover: absDx > slop && absDx >= 1.5 * absDy
 * (GameSpace PagerAxisOwnership / Miuix §4A.3).
 */
internal enum class Kc2AngleCommit {
    Horizontal,
    VerticalClear,
}

/** Ty le ap dao ngang sau vertical bias — khop GameSpace 658f023. */
internal const val Kc2HorizontalTakeoverRatio = 1.5f

internal fun kc2AngleCommit(
    absDx: Float,
    absDy: Float,
    touchSlop: Float,
    alreadyCommitted: Boolean,
): Kc2AngleCommit? {
    if (alreadyCommitted) return Kc2AngleCommit.Horizontal
    return when {
        absDx > touchSlop && absDx >= Kc2HorizontalTakeoverRatio * absDy ->
            Kc2AngleCommit.Horizontal
        absDy > touchSlop && absDy > absDx -> Kc2AngleCommit.VerticalClear
        else -> null
    }
}

/** absDx/absDy tu packed position hien tai tru down (kc2.j). */
internal fun absoluteDragAbs(curX: Float, curY: Float, downX: Float, downY: Float): Pair<Float, Float> =
    abs(curX - downX) to abs(curY - downY)
