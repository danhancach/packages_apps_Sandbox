/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.PI
import kotlin.math.abs

/**
 * Miuix compact title motion (Folme Spring HyperOS/MIUI) — pure SSOT.
 *
 * Provenance: GameSpace 658f023 [MiuixTitleMotion] +
 * .docs/ui/lsposed-miuix-rea.md muc 3.
 * - largeTitle mo het trong 1/3 hanh trinh (collapsedFraction * 3f).
 * - smallTitleVisible khi collapsedFraction * 3f >= 1f.
 * - Folme Spring bat doi xung:
 *     Show: damping=1, response=0.3 → stiffness≈438.65; Y +20px→0, Alpha 0→1.
 *     Hide: damping=1, response=0.15 → stiffness≈1754.6; Y 0→+20px, Alpha 1→0.
 */
object MiuixTitleMotion {
    const val TransitionDistancePx = 20f

    const val ResponseShow = 0.3f
    const val ResponseHide = 0.15f
    const val DampingRatio = 1.0f

    const val StiffnessShow = 438.6491f
    const val StiffnessHide = 1754.5964f
    const val Threshold = 0.001f

    const val CollapseThreshold = 1f / 3f

    // Submenu Expressive back / collapsed title (SettingsLib + GameSpace 658f023)
    const val BackCircleDp = 40f
    const val BackArrowDp = 16f
    const val BackOpticalStartDp = 16f
    const val BackTouchMinDp = 48f
    const val CollapsedTitleNavGapDp = 22f
    const val CollapsedTitleSp = 22f
    const val ExpandedTitleHorizontalDp = 24f

    /** IconButton start padding de vong 40dp canh quang hoc 16dp trong hit 48dp. */
    fun backIconButtonStartPaddingDp(
        opticalStartDp: Float = BackOpticalStartDp,
        touchMinDp: Float = BackTouchMinDp,
        circleDp: Float = BackCircleDp,
    ): Float = opticalStartDp - (touchMinDp - circleDp) / 2f

    /**
     * LTR title start tu canh noi dung: opticalStart + circle + gap (= 78dp chuan).
     * Khong cong don titleMarginStart khi hasNavigation.
     */
    fun collapsedTitleStartDp(
        opticalStartDp: Float = BackOpticalStartDp,
        circleDp: Float = BackCircleDp,
        gapDp: Float = CollapsedTitleNavGapDp,
    ): Float = opticalStartDp + circleDp + gapDp

    fun collapsedTitleStartX(
        toolbarLeftPx: Float,
        navCenterXPx: Float,
        hasNavigation: Boolean,
        backIconSizePx: Float,
        gapPx: Float,
        contentInsetStartPx: Float,
        titleMarginStartPx: Float,
    ): Float {
        return if (hasNavigation && backIconSizePx > 0f) {
            navCenterXPx + backIconSizePx / 2f + gapPx
        } else {
            toolbarLeftPx + contentInsetStartPx + titleMarginStartPx
        }
    }

    fun collapseFraction(offsetPx: Float, totalScrollRangePx: Float): Float =
        if (totalScrollRangePx > 0f) {
            (abs(offsetPx) / totalScrollRangePx).coerceIn(0f, 1f)
        } else {
            0f
        }

    fun isSmallTitleVisible(collapseFraction: Float): Boolean =
        (collapseFraction * 3f) >= 1f

    fun largeTitleAlpha(collapseFraction: Float): Float =
        (1f - (collapseFraction * 3f)).coerceIn(0f, 1f)

    fun stiffness(visible: Boolean): Float =
        if (visible) StiffnessShow else StiffnessHide

    /** Folme: stiffness = (2π / response)^2 — dung de doi chieu hang so. */
    fun stiffnessFromResponse(response: Float): Float {
        val omega = (2.0 * PI) / response.toDouble()
        return (omega * omega).toFloat()
    }
}
