/**
 * Host check — MiuixTitleMotion Folme / 1/3 threshold (pure).
 */
package com.android.axion.sandbox.tools

import kotlin.math.PI
import kotlin.math.abs
import kotlin.system.exitProcess
import com.android.axion.sandbox.ui.motion.MiuixTitleMotion

object HostMiuixTitleMotionCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        check(!MiuixTitleMotion.isSmallTitleVisible(0f), "hidden at 0")
        check(!MiuixTitleMotion.isSmallTitleVisible(0.2f), "hidden at 0.2")
        check(!MiuixTitleMotion.isSmallTitleVisible(0.33f), "hidden at 0.33")
        check(
            MiuixTitleMotion.isSmallTitleVisible(1f / 3f),
            "visible at exact 1/3",
        )
        check(MiuixTitleMotion.isSmallTitleVisible(0.5f), "visible at 0.5")

        check(MiuixTitleMotion.largeTitleAlpha(0f) == 1f, "large@0=1")
        check(
            abs(MiuixTitleMotion.largeTitleAlpha(1f / 6f) - 0.5f) < 0.001f,
            "large@1/6=0.5",
        )
        check(MiuixTitleMotion.largeTitleAlpha(1f / 3f) == 0f, "large@1/3=0")
        check(MiuixTitleMotion.largeTitleAlpha(1f) == 0f, "large@1=0")

        val expectedShow = ((2.0 * PI / 0.3) * (2.0 * PI / 0.3)).toFloat()
        val expectedHide = ((2.0 * PI / 0.15) * (2.0 * PI / 0.15)).toFloat()
        check(
            abs(MiuixTitleMotion.StiffnessShow - expectedShow) < 0.01f,
            "StiffnessShow Folme",
        )
        check(
            abs(MiuixTitleMotion.StiffnessHide - expectedHide) < 0.01f,
            "StiffnessHide Folme",
        )
        check(
            abs(MiuixTitleMotion.StiffnessHide - MiuixTitleMotion.StiffnessShow * 4f) < 0.01f,
            "hide ≈ 4× show",
        )
        check(
            abs(
                MiuixTitleMotion.stiffnessFromResponse(MiuixTitleMotion.ResponseShow) -
                    MiuixTitleMotion.StiffnessShow,
            ) < 0.01f,
            "stiffnessFromResponse show",
        )
        check(
            MiuixTitleMotion.stiffness(true) == MiuixTitleMotion.StiffnessShow,
            "stiffness(visible)",
        )
        check(
            MiuixTitleMotion.stiffness(false) == MiuixTitleMotion.StiffnessHide,
            "stiffness(hidden)",
        )
        check(
            MiuixTitleMotion.TransitionDistancePx == 20f,
            "translation distance 20px",
        )
        check(
            abs(MiuixTitleMotion.collapseFraction(-50f, 100f) - 0.5f) < 1e-6f,
            "collapseFraction",
        )

        if (fails.isNotEmpty()) {
            System.err.println("HostMiuixTitleMotionCheck FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostMiuixTitleMotionCheck PASS")
    }
}
