/**
 * Host check — bottom nav row height / pill+label alignment (pure math).
 * Covers fontScale 1x/1.5x/2x, narrow viewport, RTL mirror bounds.
 */
package com.android.axion.sandbox.tools

import com.android.axion.sandbox.ui.SandboxBottomNavGeometry
import kotlin.math.abs
import kotlin.system.exitProcess

object HostBottomNavGeometryCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    private fun nearly(a: Float, b: Float, eps: Float = 1e-3f): Boolean = abs(a - b) < eps

    @JvmStatic
    fun main(args: Array<String>) {
        check(SandboxBottomNavGeometry.MinRowHeightDp == 64f, "min row 64dp")
        check(SandboxBottomNavGeometry.PillHeightDp == 32f, "pill 32dp")
        check(SandboxBottomNavGeometry.PillWidthDp == 56f, "pill width 56dp")
        check(SandboxBottomNavGeometry.IconSizeDp == 24f, "icon 24dp")
        check(SandboxBottomNavGeometry.TopSpaceDp == 6f, "top space 6dp")
        check(SandboxBottomNavGeometry.IndicatorToLabelSpaceDp == 4f, "indicator to label 4dp")
        check(SandboxBottomNavGeometry.TabCount == 3, "3 tabs")

        // fontScale 1.0: content 57.6 + 6.4 = 64 → row 64.
        check(nearly(SandboxBottomNavGeometry.labelBandDp(1f), 19.6f), "1x labelBand 19.6dp")
        check(nearly(SandboxBottomNavGeometry.contentBlockDp(1f), 57.6f), "1x contentBlock 57.6dp")
        check(nearly(SandboxBottomNavGeometry.rowHeightDp(1f), 64f), "1x rowHeight 64dp")
        check(SandboxBottomNavGeometry.contentFitsInRow(1f), "1x content fits row")
        val top1 = SandboxBottomNavGeometry.blockTopDp(64f, 57.6f)
        check(nearly(top1, 6f), "1x blockTop 6dp")
        check(top1 + 57.6f <= 64f + 1e-3f, "1x pill+label inside row")

        // fontScale 1.5: rowHeight is fixed at 64dp per AOSP / Google Clock SSOT.
        check(nearly(SandboxBottomNavGeometry.labelBandDp(1.5f), 27.4f), "1.5x labelBand 27.4dp")
        check(nearly(SandboxBottomNavGeometry.contentBlockDp(1.5f), 65.4f), "1.5x contentBlock 65.4dp")
        check(nearly(SandboxBottomNavGeometry.rowHeightDp(1.5f), 64f), "1.5x rowHeight 64dp")
        val top15 = SandboxBottomNavGeometry.blockTopDp(64f, 65.4f)
        check(nearly(top15, 6f), "1.5x blockTop 6dp")

        // fontScale 2.0: rowHeight is fixed at 64dp per AOSP / Google Clock SSOT.
        check(nearly(SandboxBottomNavGeometry.labelBandDp(2f), 35.2f), "2x labelBand 35.2dp")
        check(nearly(SandboxBottomNavGeometry.contentBlockDp(2f), 73.2f), "2x contentBlock 73.2dp")
        check(nearly(SandboxBottomNavGeometry.rowHeightDp(2f), 64f), "2x rowHeight 64dp")
        val top2 = SandboxBottomNavGeometry.blockTopDp(64f, 73.2f)
        check(nearly(top2, 6f), "2x blockTop 6dp")

        // Row height independent of host viewport height (bug: maxHeight = viewport).
        for (fs in floatArrayOf(1f, 1.5f, 2f)) {
            check(
                SandboxBottomNavGeometry.rowIndependentOfViewportHeight(fs, 800f, 2520f),
                "rowHeight independent of viewport @fs=$fs",
            )
            val row = SandboxBottomNavGeometry.rowHeightDp(fs)
            check(row < 800f && row < 2520f, "rowHeight finite << viewport @fs=$fs")
            check(row >= SandboxBottomNavGeometry.MinRowHeightDp, "rowHeight >= min @fs=$fs")
        }

        // Narrow content width: 240dp / 3 tabs = 80dp/tab.
        val narrowContent = 240f
        val itemW = SandboxBottomNavGeometry.itemWidthDp(narrowContent)
        check(nearly(itemW, 80f), "narrow itemWidth 80dp")
        val pillW = SandboxBottomNavGeometry.pillWidthDp(itemW)
        check(nearly(pillW, 56f), "narrow pillWidth 56dp")
        for (pos in floatArrayOf(0f, 1f, 2f)) {
            val ltr = SandboxBottomNavGeometry.pillOffsetXLtr(pos, itemW, pillW)
            check(ltr >= -1e-3f, "narrow LTR pill left >=0 @pos=$pos")
            check(ltr + pillW <= narrowContent + 1e-3f, "narrow LTR pill right <=width @pos=$pos")
            val rtl = SandboxBottomNavGeometry.pillOffsetXRtlMirrored(pos, itemW, pillW, narrowContent)
            check(rtl >= -1e-3f, "narrow RTL pill left >=0 @pos=$pos")
            check(rtl + pillW <= narrowContent + 1e-3f, "narrow RTL pill right <=width @pos=$pos")
        }

        // Wide + RTL: selected page 0 LTR left ↔ RTL right mirrored.
        val wide = 360f
        val wideItem = SandboxBottomNavGeometry.itemWidthDp(wide)
        val widePill = SandboxBottomNavGeometry.pillWidthDp(wideItem)
        val ltr0 = SandboxBottomNavGeometry.pillOffsetXLtr(0f, wideItem, widePill)
        val rtl0 = SandboxBottomNavGeometry.pillOffsetXRtlMirrored(0f, wideItem, widePill, wide)
        check(nearly(ltr0 + widePill + rtl0, wide), "RTL mirror conserves width @pos0")
        check(rtl0 > ltr0, "RTL pos0 pill shifts toward end")
        for (pos in floatArrayOf(0f, 0.5f, 1f, 1.5f, 2f)) {
            val l = SandboxBottomNavGeometry.pillOffsetXLtr(pos, wideItem, widePill)
            val r = SandboxBottomNavGeometry.pillOffsetXRtlMirrored(pos, wideItem, widePill, wide)
            check(l >= -1e-3f && l + widePill <= wide + 1e-3f, "wide LTR bounds @pos=$pos")
            check(r >= -1e-3f && r + widePill <= wide + 1e-3f, "wide RTL bounds @pos=$pos")
        }

        if (fails.isNotEmpty()) {
            System.err.println("HostBottomNavGeometryCheck FAIL:")
            fails.forEach { System.err.println(" - $it") }
            exitProcess(1)
        }
        println("HostBottomNavGeometryCheck PASS")
    }
}
