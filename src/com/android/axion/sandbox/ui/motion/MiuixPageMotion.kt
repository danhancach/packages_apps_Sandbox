/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Miuix submenu page motion (LSPosed Manager nav).
 *
 * Provenance: GameSpace 658f023 [MiuixPageMotion] +
 * .docs/ui/lsposed-miuix-rea.md muc 4B; Compose host [MiuixPageTransition].
 */
object MiuixPageMotion {
    const val FullPageDurationMs = 500
    const val SettleDampingRatio = 1f
    const val SettleStiffness = 146f
    const val SettleThreshold = 0.0025f
    const val ParentFadeFactor = 0.1f
    const val ParentParallaxFactor = 0.25f
    const val NavScrimMaxAlpha = 0.5f
    const val FullPageDeltaThreshold = 0.999f

    private val easingG = -7.4612823f
    private val easingH = (sqrt(24.05716559930144) / 2.0).toFloat()
    private val easingI = easingG / easingH

    data class LayerTransform(
        val translationX: Float,
        val alpha: Float,
    )

    data class SpringSample(
        val value: Float,
        val velocity: Float,
        val settled: Boolean,
    )

    fun easingR22(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f).toDouble()
        return ((sin(easingH * t) * easingI - cos(easingH * t)) * exp(easingG * t) + 1.0)
            .toFloat()
    }

    fun useFullPageTween(velocity: Float, current: Float, target: Float): Boolean =
        velocity == 0f && kotlin.math.abs(target - current) >= FullPageDeltaThreshold

    fun childLayer(progress: Float, widthPx: Float, rtl: Boolean): LayerTransform {
        val p = progress.coerceIn(0f, 1f)
        val direction = if (rtl) -1f else 1f
        val dx = if (widthPx <= 0f) {
            0f
        } else {
            (direction * widthPx * (1f - p)).roundToInt().toFloat()
        }
        return LayerTransform(translationX = dx, alpha = 1f)
    }

    fun parentLayer(progress: Float, widthPx: Float, rtl: Boolean): LayerTransform {
        val p = progress.coerceIn(0f, 1f)
        val direction = if (rtl) -1f else 1f
        val dx = if (widthPx <= 0f) {
            0f
        } else {
            (-direction * widthPx * p * ParentParallaxFactor).roundToInt().toFloat()
        }
        return LayerTransform(
            translationX = dx,
            alpha = 1f - ParentFadeFactor * p,
        )
    }

    fun scrimAlpha(progress: Float): Float =
        NavScrimMaxAlpha * progress.coerceIn(0f, 1f)

    fun springStep(
        value: Float,
        velocity: Float,
        target: Float,
        dtSec: Float,
        stiffness: Float = SettleStiffness,
        threshold: Float = SettleThreshold,
    ): SpringSample {
        if (dtSec <= 0f) {
            return SpringSample(value, velocity, settled = false)
        }
        val omega = sqrt(stiffness.coerceAtLeast(0f))
        val x0 = value - target
        if (kotlin.math.abs(x0) < threshold && kotlin.math.abs(velocity) < threshold) {
            return SpringSample(target, 0f, settled = true)
        }
        val t = dtSec
        val envelope = exp((-omega * t).toDouble()).toFloat()
        val b = velocity + omega * x0
        val displacement = (x0 + b * t) * envelope
        val newValue = target + displacement
        val newVelocity = (velocity - omega * b * t) * envelope
        val settled =
            kotlin.math.abs(newValue - target) < threshold &&
                kotlin.math.abs(newVelocity) < threshold
        return if (settled) {
            SpringSample(target, 0f, settled = true)
        } else {
            SpringSample(newValue, newVelocity, settled = false)
        }
    }

    fun pageProgressFromPredictiveBack(
        backProgress: Float,
        anchorPageProgress: Float,
    ): Float {
        val t = backProgress.coerceIn(0f, 1f)
        val anchor = anchorPageProgress.coerceIn(0f, 1f)
        return anchor * (1f - t)
    }

    /**
     * Van toc tha tay Predictive Back (y12 / LSPosed).
     * minVelocity = remaining > 0 ? -sqrt(k) * remaining : 0
     * initial = max(savedSwipe, minVelocity) — dam bao spring ve target.
     */
    fun predictiveReleaseVelocity(
        currentProgress: Float,
        target: Float,
        savedSwipeVelocity: Float,
        stiffness: Float = SettleStiffness,
    ): Float {
        val remaining = currentProgress - target
        val minVelocity = if (remaining > 0f) {
            -sqrt(stiffness.coerceAtLeast(0f)) * remaining
        } else {
            0f
        }
        return if (savedSwipeVelocity < minVelocity) minVelocity else savedSwipeVelocity
    }
}
