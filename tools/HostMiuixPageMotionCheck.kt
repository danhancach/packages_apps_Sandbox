/**
 * Host check — MiuixPageMotion geometry / r22 / spring / predictive release (pure).
 */
package com.android.axion.sandbox.tools

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.system.exitProcess
import com.android.axion.sandbox.ui.motion.MiuixPageMotion

object HostMiuixPageMotionCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        check(abs(MiuixPageMotion.easingR22(0f) - 0f) < 1e-5f, "r22(0)=0")
        check(abs(MiuixPageMotion.easingR22(1f) - 1f) < 2e-2f, "r22(1)~1")
        val mid = MiuixPageMotion.easingR22(0.5f)
        check(mid > 0.5f && mid < 1f, "r22(0.5) past linear got $mid")

        check(
            MiuixPageMotion.useFullPageTween(0f, 0f, 1f),
            "rest 0→1 uses tween",
        )
        check(
            !MiuixPageMotion.useFullPageTween(0.4f, 0.2f, 1f),
            "nonzero velocity → spring",
        )
        check(
            !MiuixPageMotion.useFullPageTween(0f, 0.5f, 1f),
            "partial delta → spring",
        )

        val w = 1080f
        val child0 = MiuixPageMotion.childLayer(0f, w, rtl = false)
        check(abs(child0.translationX - w) < 0.5f, "LTR child@0 = W")
        val child1 = MiuixPageMotion.childLayer(1f, w, rtl = false)
        check(abs(child1.translationX) < 0.5f, "LTR child@1 = 0")
        val parentHalf = MiuixPageMotion.parentLayer(0.5f, w, rtl = false)
        check(
            abs(parentHalf.translationX - (-w * 0.5f * 0.25f)) < 1f,
            "LTR parent@0.5 = -W/8",
        )
        check(
            abs(parentHalf.alpha - (1f - 0.1f * 0.5f)) < 1e-5f,
            "parent alpha 1-0.1p",
        )
        val childRtl = MiuixPageMotion.childLayer(0f, w, rtl = true)
        check(abs(childRtl.translationX - (-w)) < 0.5f, "RTL child@0 = -W")

        check(abs(MiuixPageMotion.scrimAlpha(0f)) < 1e-6f, "scrim@0")
        check(
            abs(MiuixPageMotion.scrimAlpha(1f) - MiuixPageMotion.NavScrimMaxAlpha) < 1e-6f,
            "scrim@1",
        )

        var v = 0.2f
        var vel = 0f
        var settled = false
        repeat(240) {
            val s = MiuixPageMotion.springStep(v, vel, target = 1f, dtSec = 1f / 60f)
            v = s.value
            vel = s.velocity
            settled = s.settled
            if (settled) return@repeat
        }
        check(settled, "spring settles toward 1")
        check(abs(v - 1f) < MiuixPageMotion.SettleThreshold, "spring value ~1")

        val mapped = MiuixPageMotion.pageProgressFromPredictiveBack(0.2f, 0.85f)
        check(abs(mapped - 0.85f * 0.8f) < 1e-5f, "predictive map from anchor")

        // max(saved, minVelocity): minV = -sqrt(k)*remaining khi remaining>0
        val minV = -sqrt(MiuixPageMotion.SettleStiffness) * 0.4f
        val atRest = MiuixPageMotion.predictiveReleaseVelocity(
            currentProgress = 0.4f,
            target = 0f,
            savedSwipeVelocity = 0f,
        )
        check(atRest == 0f, "rest swipe keeps 0 (max with minV)")
        val tooFast = MiuixPageMotion.predictiveReleaseVelocity(
            currentProgress = 0.4f,
            target = 0f,
            savedSwipeVelocity = -20f,
        )
        check(abs(tooFast - minV) < 1e-3f, "faster-than-min clamps up to minV")
        val mild = MiuixPageMotion.predictiveReleaseVelocity(
            currentProgress = 0.4f,
            target = 0f,
            savedSwipeVelocity = -1f,
        )
        check(mild == -1f, "mild closing swipe kept")

        if (fails.isNotEmpty()) {
            System.err.println("HostMiuixPageMotionCheck FAIL:")
            fails.forEach { System.err.println("  - $it") }
            exitProcess(1)
        }
        println("HostMiuixPageMotionCheck PASS")
    }
}
