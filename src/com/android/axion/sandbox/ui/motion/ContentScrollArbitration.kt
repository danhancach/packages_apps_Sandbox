/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** Active-page stretch owner cho pager arbitration. */
internal interface StretchBusyOwner {
    val stretchIsInProgress: Boolean
    fun peekVisualE(): Float
    fun deferBusyPreserveTension()
    fun endPagerDeferPreserveTension()
    fun confirmHorizontalTakeoverClearTension()
    /** UP/tap sau defer: resume settle neu con E (khong de sticky tension). */
    fun resumeSettleIfNeeded()
}

/**
 * Chi tac dong **captured** owner (DOWN bind). Horizontal: clear E.
 * Vertical/UP: end defer + resume settle.
 */
internal class ContentScrollArbitration {
    private var active: StretchBusyOwner? = null

    fun setActive(owner: StretchBusyOwner?) {
        active = owner
    }

    fun clearActive(owner: StretchBusyOwner) {
        if (active === owner) active = null
    }

    /** Snapshot owner tai DOWN — cleanup khong theo active moi sau tab switch. */
    fun captureActiveOwner(): StretchBusyOwner? = active

    fun isOwnerBusy(owner: StretchBusyOwner?): Boolean =
        owner?.stretchIsInProgress == true

    fun isActiveContentBusy(): Boolean = active?.stretchIsInProgress == true

    fun peekActiveVisualE(): Float? = active?.peekVisualE()

    fun deferBusyPreserveTension(owner: StretchBusyOwner? = active) {
        owner?.deferBusyPreserveTension()
    }

    fun endDeferPreserveTension(owner: StretchBusyOwner? = active) {
        owner?.endPagerDeferPreserveTension()
    }

    fun confirmHorizontalTakeover(owner: StretchBusyOwner? = active) {
        owner?.confirmHorizontalTakeoverClearTension()
    }

    fun resumeSettleIfNeeded(owner: StretchBusyOwner? = active) {
        owner?.resumeSettleIfNeeded()
    }
}

internal val LocalContentScrollArbitration =
    staticCompositionLocalOf<ContentScrollArbitration?> { null }

/** Page index trong HorizontalPager (null = ngoai pager). */
internal val LocalSandboxPagerPageIndex =
    staticCompositionLocalOf<Int?> { null }

/** currentPage visible — chi page khop moi setActive. */
internal val LocalSandboxPagerVisiblePage =
    staticCompositionLocalOf<Int> { 0 }

/** Pager khong dang scroll ngang — Back selection chi khi settled. */
internal val LocalSandboxPagerSettled =
    staticCompositionLocalOf { true }

@Composable
internal fun RememberStretchArbitrationRegistration(
    effect: OverscrollEffect?,
) {
    val gate = LocalContentScrollArbitration.current
    val owner = effect as? StretchBusyOwner
    val pageIndex = LocalSandboxPagerPageIndex.current
    val visiblePage = LocalSandboxPagerVisiblePage.current
    // Ngoai pager (pageIndex null): active nhu cu. Trong pager: chi page dang visible.
    val active = pageIndex == null || pageIndex == visiblePage
    DisposableEffect(gate, owner, active, pageIndex, visiblePage) {
        if (gate == null || owner == null) {
            return@DisposableEffect onDispose { }
        }
        if (active) {
            gate.setActive(owner)
        } else {
            gate.clearActive(owner)
        }
        onDispose {
            gate.clearActive(owner)
        }
    }
}
