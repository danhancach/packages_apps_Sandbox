/**
 * Host check — PagerAxisOwnership production reducer sequences (pure).
 * Vertical lock roi horizontal takeover 1.5× trong CUNG gesture.
 */
package com.android.axion.sandbox.tools

import com.android.axion.sandbox.ui.motion.Kc2AngleCommit
import com.android.axion.sandbox.ui.motion.Kc2HorizontalTakeoverRatio
import com.android.axion.sandbox.ui.motion.PagerAxisOwnership
import com.android.axion.sandbox.ui.motion.absoluteDragAbs
import com.android.axion.sandbox.ui.motion.kc2AngleCommit

import kotlin.system.exitProcess

object HostPagerAxisOwnershipCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val cfg = PagerAxisOwnership.Config(touchSlopPx = 16f)

        // --- Sequence: vertical first, then dx>=1.5 dy takeover ---
        var s = PagerAxisOwnership.down(PagerAxisOwnership.reset(), 0L, 100f, 100f)
        s = PagerAxisOwnership.move(s, 0L, 105f, 160f, cfg) // dy=60, dx=5 → Vertical
        check(
            s.axis == PagerAxisOwnership.Axis.Vertical,
            "vertical lock after intentional dy",
        )
        check(PagerAxisOwnership.locksVertical(s), "locksVertical")
        val pre = s
        // Mild X drift while vertical — stay Vertical
        s = PagerAxisOwnership.move(s, 0L, 130f, 170f, cfg) // dx=30, dy=70
        check(
            s.axis == PagerAxisOwnership.Axis.Vertical,
            "vertical holds under mild X drift",
        )
        // Decisive horizontal: dx=80, dy=35 from down(100,100) → (180,135)
        s = PagerAxisOwnership.move(s, 0L, 180f, 135f, cfg)
        check(
            s.axis == PagerAxisOwnership.Axis.Horizontal,
            "horizontal takeover after vertical: dx=80 dy=35 (>=1.5)",
        )
        check(
            PagerAxisOwnership.shouldClearTensionOnHorizontalTakeover(pre, s),
            "clear tension on Vertical→Horizontal edge",
        )
        check(
            PagerAxisOwnership.shouldDriveSameGesturePager(
                flingAtDown = false,
                contentBusyAtDown = false,
                hadVerticalLock = true,
            ),
            "same-gesture driver after vertical lock",
        )
        check(
            !PagerAxisOwnership.shouldDriveSameGesturePager(
                flingAtDown = false,
                contentBusyAtDown = false,
                hadVerticalLock = false,
            ),
            "native horizontal without vertical → no forced driver",
        )

        // Boundary 1.5 after vertical: dx=45 dy=30 from (0,0)
        var b = PagerAxisOwnership.down(PagerAxisOwnership.reset(), 2L, 0f, 0f)
        b = PagerAxisOwnership.move(b, 2L, 5f, 40f, cfg)
        check(b.axis == PagerAxisOwnership.Axis.Vertical, "pre vertical for boundary")
        b = PagerAxisOwnership.move(b, 2L, 45f, 30f, cfg)
        check(
            b.axis == PagerAxisOwnership.Axis.Horizontal,
            "boundary dx==1.5*dy takeover after vertical",
        )

        // Below 1.5 after vertical stays vertical
        var soft = PagerAxisOwnership.down(PagerAxisOwnership.reset(), 3L, 0f, 0f)
        soft = PagerAxisOwnership.move(soft, 3L, 2f, 40f, cfg)
        soft = PagerAxisOwnership.move(soft, 3L, 40f, 30f, cfg) // 40 < 45
        check(
            soft.axis == PagerAxisOwnership.Axis.Vertical,
            "sub-1.5 after vertical stays Vertical",
        )

        // childHorizontalClaim blocks locksHorizontal / tension clear
        var claim = PagerAxisOwnership.down(
            PagerAxisOwnership.reset(),
            4L,
            0f,
            0f,
            childHorizontalClaim = true,
        )
        claim = PagerAxisOwnership.move(claim, 4L, 80f, 10f, cfg)
        check(claim.axis == PagerAxisOwnership.Axis.Horizontal, "claim still classifies H")
        check(
            !PagerAxisOwnership.locksHorizontal(claim),
            "childHorizontalClaim blocks pager lock",
        )

        // Fixture: 50/30 accepted by 1.5, rejected by old 2× (50 < 60)
        check(
            kc2AngleCommit(50f, 30f, 16f, false) == Kc2AngleCommit.Horizontal,
            "1.5 accepts 50/30 (old 2x would reject: 50<60)",
        )
        // 80/35: both 1.5 and old 2× accept (80>70) — do not claim 2x rejects
        check(
            kc2AngleCommit(80f, 35f, 16f, false) == Kc2AngleCommit.Horizontal,
            "80/35 Horizontal under 1.5 (also >2*35)",
        )

        if (fails.isNotEmpty()) {
            System.err.println("HostPagerAxisOwnershipCheck FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostPagerAxisOwnershipCheck PASS")
    }
}
