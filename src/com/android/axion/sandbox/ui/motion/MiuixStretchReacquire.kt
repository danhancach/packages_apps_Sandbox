/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import kotlin.math.abs
import kotlin.math.sign

/**
 * Reacquire / reverse lien tuc khi od3 de |E| > G/3.
 * Unbound: E += delta (1:1); reverse doi dau chi claim toi E=0, phan du forward.
 */
internal object MiuixStretchReacquire {
    const val VisualEpsilonPx = MiuixStretchMath.VisualEpsilonPx

    data class State(
        val rawF: Float,
        val visualE: Float,
        val unbound: Boolean,
    )

    data class ReleaseResult(
        val state: State,
        /** Luong deltaY da dung giai tension (cung dau voi input delta). */
        val consumedY: Float,
    )

    fun afterInterruptB1(visualE: Float, boundG: Float): State {
        if (abs(visualE) <= VisualEpsilonPx || boundG <= 0f) {
            return State(0f, 0f, unbound = false)
        }
        val maxE = MiuixStretchMath.maxDragVisual(boundG)
        return if (abs(visualE) > maxE + VisualEpsilonPx) {
            State(rawF = boundG * sign(visualE), visualE = visualE, unbound = true)
        } else {
            State(
                rawF = MiuixStretchMath.inverseStretch(visualE, boundG),
                visualE = visualE,
                unbound = false,
            )
        }
    }

    fun applyDelta(state: State, delta: Float, boundG: Float): State {
        if (delta == 0f || boundG <= VisualEpsilonPx) return state
        if (state.unbound) {
            var e = state.visualE + delta
            val maxE = MiuixStretchMath.maxDragVisual(boundG)
            if (abs(e) <= VisualEpsilonPx) {
                return State(0f, 0f, unbound = false)
            }
            if (abs(e) <= maxE + VisualEpsilonPx) {
                val f = MiuixStretchMath.inverseStretch(e, boundG)
                e = MiuixStretchMath.stretchVisual(f, boundG)
                return State(f, e, unbound = false)
            }
            return State(boundG * sign(e), e, unbound = true)
        }
        val f = (state.rawF + delta).coerceIn(-boundG, boundG)
        val e = MiuixStretchMath.stretchVisual(f, boundG)
        if (abs(e) <= VisualEpsilonPx) {
            return State(0f, 0f, unbound = false)
        }
        return State(f, e, unbound = false)
    }

    /**
     * R0 / reverse doi dau — unbound dung E tuyen tinh, khong dung rawF=±G lam budget.
     * E=3000 delta=-1800 → E=1200 consumed=-1800.
     * E=1000 delta=-1200 → E=0 consumed=-1000 (du -200 forward cho caller).
     */
    fun oppositeRelease(state: State, deltaY: Float, boundG: Float): ReleaseResult {
        if (deltaY == 0f || boundG <= VisualEpsilonPx) {
            return ReleaseResult(state, 0f)
        }
        if (abs(state.visualE) <= VisualEpsilonPx && abs(state.rawF) <= VisualEpsilonPx) {
            return ReleaseResult(state, 0f)
        }
        // Cung dau voi E (hoac F khi bound) → khong reverse.
        val tensionSign =
            when {
                abs(state.visualE) > VisualEpsilonPx -> sign(state.visualE)
                else -> sign(state.rawF)
            }
        if (tensionSign == 0f || sign(deltaY) == tensionSign) {
            return ReleaseResult(state, 0f)
        }

        if (state.unbound) {
            val toZero = -state.visualE
            return if (abs(deltaY) <= abs(toZero) + VisualEpsilonPx) {
                ReleaseResult(applyDelta(state, deltaY, boundG), deltaY)
            } else {
                // Cat qua goc: chi claim den E=0.
                ReleaseResult(State(0f, 0f, unbound = false), toZero)
            }
        }

        // Bound: f2 = |F|<=|delta| ? -F : delta.
        if (state.rawF == 0f) return ReleaseResult(state, 0f)
        if (sign(deltaY) == sign(state.rawF)) return ReleaseResult(state, 0f)
        return if (abs(state.rawF) <= abs(deltaY)) {
            ReleaseResult(State(0f, 0f, unbound = false), -state.rawF)
        } else {
            ReleaseResult(applyDelta(state, deltaY, boundG), deltaY)
        }
    }

    /**
     * C1: scroll consume cung dau tension → chi giam ve 0, KHONG tao E doi dau.
     * E=1000 consumed=1200 → E=0 (khong -200).
     */
    fun releaseFromConsumed(state: State, consumedY: Float, boundG: Float): State {
        if (consumedY == 0f) return state
        if (abs(state.visualE) <= VisualEpsilonPx && abs(state.rawF) <= VisualEpsilonPx) {
            return state
        }
        val tensionSign =
            when {
                abs(state.visualE) > VisualEpsilonPx -> sign(state.visualE)
                else -> sign(state.rawF)
            }
        if (tensionSign == 0f || sign(consumedY) != tensionSign) return state
        // Dong oppositeRelease: delta doi dau, over-zero chi toi E=0.
        val reduceDelta = -tensionSign * abs(consumedY)
        return oppositeRelease(state, reduceDelta, boundG).state
    }

    /** Sau D1 doi G: unbound co the roi vao drag-range. */
    fun afterBoundChange(state: State, boundG: Float): State {
        if (!state.unbound || boundG <= 0f) {
            if (!state.unbound && state.rawF != 0f && boundG > 0f) {
                val f = state.rawF.coerceIn(-boundG, boundG)
                return State(f, MiuixStretchMath.stretchVisual(f, boundG), unbound = false)
            }
            return state
        }
        val maxE = MiuixStretchMath.maxDragVisual(boundG)
        if (abs(state.visualE) <= maxE + VisualEpsilonPx) {
            if (abs(state.visualE) <= VisualEpsilonPx) {
                return State(0f, 0f, unbound = false)
            }
            val f = MiuixStretchMath.inverseStretch(state.visualE, boundG)
            return State(f, MiuixStretchMath.stretchVisual(f, boundG), unbound = false)
        }
        return State(boundG * sign(state.visualE), state.visualE, unbound = true)
    }
}
