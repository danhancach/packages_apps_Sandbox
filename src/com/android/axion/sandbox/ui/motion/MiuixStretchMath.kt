/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.withSign

/**
 * g(t)/kh2.y — pure Kotlin (test JVM khong Compose).
 */
internal object MiuixStretchMath {
    const val VisualEpsilonPx = 1.0f

    /** g(t)=t - t^2 + t^3/3; E = sign(F)*g(|F|/G)*G */
    fun stretchVisual(rawF: Float, boundG: Float): Float {
        if (rawF == 0f || boundG <= 0f) return 0f
        val t = (abs(rawF) / boundG).coerceIn(0f, 1f).toDouble()
        val g = t - t.pow(2.0) + t.pow(3.0) / 3.0
        return (sign(rawF) * g * boundG).toFloat()
    }

    /** kh2.y(E, G) * sign(E) — nghich dao de B1. */
    fun inverseStretch(visualE: Float, boundG: Float): Float {
        if (visualE == 0f || boundG <= 0f) return 0f
        val maxVisual = abs(stretchVisual(boundG, boundG))
        val clamped = abs(visualE).coerceAtMost(maxVisual).toDouble()
        val d3 = boundG.toDouble() - clamped * 3.0
        val rawAbs =
            boundG.toDouble() -
                abs(d3).pow(1.0 / 3.0) * sign(d3) * boundG.toDouble().pow(2.0 / 3.0)
        return rawAbs.toFloat().withSign(visualE)
    }

    fun maxDragVisual(boundG: Float): Float = abs(stretchVisual(boundG, boundG))
}
