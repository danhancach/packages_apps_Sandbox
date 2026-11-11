/**
 * Host check — StretchBusyDeferGate + StretchBusyLifecycle (pure Kotlin).
 * Cover: defer / end-defer / horizontal takeover; B1 reacquire rawF=0 + E; shouldArmHaltOnDown.
 */
package com.android.axion.sandbox.tools

import com.android.axion.sandbox.ui.motion.MiuixStretchMath
import com.android.axion.sandbox.ui.motion.MiuixStretchReacquire
import com.android.axion.sandbox.ui.motion.StretchBusyDeferGate
import com.android.axion.sandbox.ui.motion.StretchBusyLifecycle
import kotlin.math.abs
import kotlin.system.exitProcess

object HostStretchBusyCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    private fun nearly(a: Float, b: Float, eps: Float = 0.5f): Boolean = abs(a - b) <= eps

    @JvmStatic
    fun main(args: Array<String>) {
        val eps = MiuixStretchMath.VisualEpsilonPx
        val gate = StretchBusyDeferGate(eps)

        // Defer: an isInProgress, giu E.
        gate.setVisualE(120f)
        check(gate.isInProgress(), "busy before defer")
        gate.deferBusyPreserveTension()
        check(gate.suppressInProgress, "defer sets suppress")
        check(!gate.isInProgress(), "defer hides isInProgress")
        check(nearly(gate.visualE, 120f, 0f), "defer preserves E")

        // End-defer: bo suppress, E giu.
        gate.endDeferPreserveTension()
        check(!gate.suppressInProgress, "endDefer clears suppress")
        check(gate.isInProgress(), "endDefer restores isInProgress")
        check(nearly(gate.visualE, 120f, 0f), "endDefer preserves E")

        // Horizontal takeover: E=0, bo suppress.
        gate.deferBusyPreserveTension()
        gate.confirmHorizontalTakeoverClearTension()
        check(nearly(gate.visualE, 0f, 0f), "horizontal clears E")
        check(!gate.suppressInProgress, "horizontal clears suppress")
        check(!gate.isInProgress(), "horizontal not in progress")

        // B1: rawF=0 + E nonzero → reacquire; rawF!=0 / E~0 → null.
        val g = 3000f
        val e0 = 1000f
        val b1 = StretchBusyLifecycle.reacquireAfterSettleInterrupt(0f, e0, g)
        check(b1 != null, "B1 returns state for rawF=0 E nonzero")
        val expected = MiuixStretchReacquire.afterInterruptB1(e0, g)
        check(nearly(b1!!.rawF, expected.rawF), "B1 rawF matches afterInterruptB1")
        check(nearly(b1.visualE, expected.visualE), "B1 visualE matches")
        check(b1.unbound == expected.unbound, "B1 unbound matches")
        check(
            StretchBusyLifecycle.reacquireAfterSettleInterrupt(1f, e0, g) == null,
            "B1 null when rawF nonzero",
        )
        check(
            StretchBusyLifecycle.reacquireAfterSettleInterrupt(0f, 0f, g) == null,
            "B1 null when E zero",
        )

        // shouldArmHaltOnDown
        check(StretchBusyLifecycle.shouldArmHaltOnDown(true), "arm halt when child fling")
        check(!StretchBusyLifecycle.shouldArmHaltOnDown(false), "no arm when not fling")

        if (fails.isNotEmpty()) {
            System.err.println("HostStretchBusyCheck FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostStretchBusyCheck PASS")
    }
}
