/**
 * Host check — kc2 horizontal takeover 1.5× threshold (pure).
 * Sequence Vertical→Horizontal: xem HostPagerAxisOwnershipCheck.
 */
package com.android.axion.sandbox.tools

import com.android.axion.sandbox.ui.motion.Kc2AngleCommit
import com.android.axion.sandbox.ui.motion.Kc2HorizontalTakeoverRatio
import com.android.axion.sandbox.ui.motion.PagerAxisOwnership
import com.android.axion.sandbox.ui.motion.absoluteDragAbs
import com.android.axion.sandbox.ui.motion.kc2AngleCommit

import kotlin.system.exitProcess

object HostPagerKc2Check {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        check(Kc2HorizontalTakeoverRatio == 1.5f, "ratio const 1.5")

        val slop = 16f
        // GameSpace fixture: dx=80, dy=35 → 80 >= 1.5*35=52.5 → Horizontal
        // (80 > 2*35=70 nen old 2x CUNG chap nhan — dung fixture 50/30 de phan biet)
        check(
            kc2AngleCommit(80f, 35f, slop, false) == Kc2AngleCommit.Horizontal,
            "takeover dx=80 dy=35 → Horizontal",
        )
        check(
            kc2AngleCommit(45f, 30f, slop, false) == Kc2AngleCommit.Horizontal,
            "dx == 1.5 dy → Horizontal (>=)",
        )
        check(
            kc2AngleCommit(40f, 30f, slop, false) == null,
            "dx=40 dy=30 < 1.5 → undecided",
        )
        // Phan biet 1.5 vs old 2×: 50 >= 45, nhung 50 < 60
        check(
            kc2AngleCommit(50f, 30f, slop, false) == Kc2AngleCommit.Horizontal,
            "dx=50 dy=30 (>=1.5, <2×) → Horizontal",
        )
        check(
            kc2AngleCommit(10f, 40f, slop, false) == Kc2AngleCommit.VerticalClear,
            "vertical clear angle helper",
        )
        check(
            kc2AngleCommit(10f, 10f, slop, false) == null,
            "inside slop undecided",
        )
        check(
            kc2AngleCommit(0f, 0f, slop, true) == Kc2AngleCommit.Horizontal,
            "alreadyCommitted sticky Horizontal",
        )

        val (ax, ay) = absoluteDragAbs(100f, 140f, 20f, 100f)
        check(ax == 80f && ay == 40f, "absoluteDragAbs")

        if (fails.isNotEmpty()) {
            System.err.println("HostPagerKc2Check FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostPagerKc2Check PASS")
    }
}
