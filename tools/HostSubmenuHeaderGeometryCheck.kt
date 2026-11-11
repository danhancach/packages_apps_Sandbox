/**
 * Host check — submenu Expressive back / collapsed title geometry (pure).
 */
package com.android.axion.sandbox.tools

import kotlin.math.abs
import kotlin.system.exitProcess
import com.android.axion.sandbox.ui.motion.MiuixTitleMotion

object HostSubmenuHeaderGeometryCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        check(MiuixTitleMotion.BackCircleDp == 40f, "circle 40dp")
        check(MiuixTitleMotion.BackArrowDp == 16f, "arrow 16dp")
        check(MiuixTitleMotion.BackOpticalStartDp == 16f, "optical start 16dp")
        check(MiuixTitleMotion.BackTouchMinDp == 48f, "touch >=48dp")
        check(MiuixTitleMotion.CollapsedTitleNavGapDp == 22f, "gap 22dp")
        check(MiuixTitleMotion.CollapsedTitleSp == 22f, "collapsed title 22sp")
        check(MiuixTitleMotion.ExpandedTitleHorizontalDp == 24f, "expanded pad 24dp")

        val pad = MiuixTitleMotion.backIconButtonStartPaddingDp()
        check(abs(pad - 12f) < 1e-4f, "IconButton start pad 12dp (16-(48-40)/2)")

        val titleStart = MiuixTitleMotion.collapsedTitleStartDp()
        check(abs(titleStart - 78f) < 1e-4f, "collapsed title start 78dp")

        // GameSpace collapsedTitleStartX with nav: center=36, back=40, gap=22 → 78
        check(
            abs(
                MiuixTitleMotion.collapsedTitleStartX(
                    toolbarLeftPx = 8f,
                    navCenterXPx = 36f,
                    hasNavigation = true,
                    backIconSizePx = 40f,
                    gapPx = 22f,
                    contentInsetStartPx = 72f,
                    titleMarginStartPx = 16f,
                ) - 78f,
            ) < 1e-4f,
            "collapsedTitleStartX hasNavigation ignores titleMarginStart",
        )
        check(
            abs(
                MiuixTitleMotion.collapsedTitleStartX(
                    toolbarLeftPx = 8f,
                    navCenterXPx = 0f,
                    hasNavigation = false,
                    backIconSizePx = 40f,
                    gapPx = 22f,
                    contentInsetStartPx = 72f,
                    titleMarginStartPx = 16f,
                ) - 96f,
            ) < 1e-4f,
            "collapsedTitleStartX fallback without nav",
        )

        // Folme contracts still hold for submenu header
        check(MiuixTitleMotion.isSmallTitleVisible(1f / 3f), "1/3 trigger")
        check(MiuixTitleMotion.largeTitleAlpha(1f / 3f) == 0f, "large gone at 1/3")
        check(MiuixTitleMotion.TransitionDistancePx == 20f, "20px translation")

        if (fails.isNotEmpty()) {
            System.err.println("HostSubmenuHeaderGeometryCheck FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostSubmenuHeaderGeometryCheck PASS")
    }
}
