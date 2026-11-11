/*
 * Copyright (C) 2025-2026 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.axion.sandbox.ui

/**
 * Pure SSOT cho bottom nav theo chuan Material 3 / Google Clock v9.2
 * (.docs/ui/chuan-thiet-ke-bottombar-material3.md).
 * - Chieu cao noi dung: 64dp co dinh (m3_comp_nav_bar_container_height)
 * - Active Indicator: 56dp x 32dp, bo goc 16dp (50%)
 * - Icon: 24dp x 24dp (can giua indicator, Y = 10dp)
 * - Khoang dem dinh: 6dp (Y indicator = 6dp)
 * - Khoang cach Indicator -> Label: 4dp (Y label = 42dp)
 * - Khoang dem day noi dung: 6.4dp
 * - Co chu: 12sp inactive / 14sp active
 */
object SandboxBottomNavGeometry {
    const val ContentHeightDp = 64f
    const val MinRowHeightDp = 64f
    const val PillHeightDp = 32f
    const val PillWidthDp = 56f
    const val PillCornerDp = 16f
    const val IconSizeDp = 24f
    const val TopSpaceDp = 6f
    const val IndicatorToLabelSpaceDp = 4f
    const val BottomSpaceDp = 6.4f
    const val LabelLineHeightSp = 15.6f
    const val LabelInactiveSp = 12f
    const val LabelActiveSp = 14f
    const val HorizontalPadDp = 0f
    const val TabCount = 3

    fun labelBandDp(fontScale: Float = 1f): Float =
        IndicatorToLabelSpaceDp + LabelLineHeightSp * fontScale.coerceAtLeast(0f)

    fun contentBlockDp(fontScale: Float = 1f): Float =
        TopSpaceDp + PillHeightDp + labelBandDp(fontScale)

    /** Chieu cao hang tab co dinh tuyet doi 64dp theo chuan AOSP / Google Clock SSOT. */
    fun rowHeightDp(fontScale: Float = 1f, verticalPadDp: Float = 0f): Float =
        MinRowHeightDp

    fun blockTopDp(rowHeightDp: Float = MinRowHeightDp, contentBlockDp: Float = 0f): Float =
        TopSpaceDp

    fun itemWidthDp(contentWidthDp: Float, tabCount: Int = TabCount): Float =
        contentWidthDp / tabCount.coerceAtLeast(1).toFloat()

    fun pillWidthDp(itemWidthDp: Float = 0f): Float = PillWidthDp

    fun pillOffsetXLtr(
        selectedPagePosition: Float,
        itemWidthDp: Float,
        pillWidthDp: Float = PillWidthDp,
    ): Float {
        val centerX = itemWidthDp * selectedPagePosition + itemWidthDp / 2f
        return centerX - pillWidthDp / 2f
    }

    /** Compose Modifier.offset RTL mirror: left' = contentWidth - left - width. */
    fun pillOffsetXRtlMirrored(
        selectedPagePosition: Float,
        itemWidthDp: Float,
        pillWidthDp: Float = PillWidthDp,
        contentWidthDp: Float,
    ): Float {
        val ltr = pillOffsetXLtr(selectedPagePosition, itemWidthDp, pillWidthDp)
        return contentWidthDp - ltr - pillWidthDp
    }

    fun contentFitsInRow(fontScale: Float = 1f, epsilon: Float = 1e-3f): Boolean {
        val row = rowHeightDp(fontScale)
        val block = contentBlockDp(fontScale)
        return block <= row + epsilon
    }

    fun rowIndependentOfViewportHeight(
        fontScale: Float,
        viewportHeightA: Float,
        viewportHeightB: Float,
    ): Boolean {
        if (viewportHeightA == viewportHeightB) return true
        val h = rowHeightDp(fontScale)
        return h == rowHeightDp(fontScale) &&
            h != viewportHeightA &&
            h != viewportHeightB
    }
}
