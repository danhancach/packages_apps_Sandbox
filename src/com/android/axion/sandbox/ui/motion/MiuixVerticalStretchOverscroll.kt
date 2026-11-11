/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.scrollableArea
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Port Miuix OverscrollElement (hb2) / rb2 stretch cho list doc.
 *
 * Provenance APK LSPosed manager 2.2.1: jb2.b → hb2(isVertical=true,
 * nestedScrollToParent=true) → rb2. Chi stretch truc Y — khong stretch ngang pager.
 *
 * Compose Scrollable gate shouldDispatchOverscroll = canScrollF||canScrollB:
 * short/empty (maxValue==0) khong goi LocalOverscrollFactory — khop jb2.a khi
 * !canScroll. jb2.b luon gan hb2 bat ke length → Updater bu bang
 * [miuixVerticalScroll] / [miuixAlwaysVerticalStretch] (mot effect/node owner).
 * Khong claim 1:1 EdgeEffect hay toan bo nested handoff Miuix.
 */

/** rb2.B — nguong |E| px (alias handoff SSOT). */
private const val VisualEpsilonPx = MiuixFlingHandoff.VisualEpsilonPx

/** od3.a: |v| > 5000 → period 0.55, else 0.4. */
private const val SettlePeriodFastVelocity = 5000.0
private const val SettlePeriodHighVelocity = 0.55
private const val SettlePeriodNormal = 0.4

@Composable
fun rememberMiuixVerticalStretchOverscrollFactory(): OverscrollFactory {
    return remember { MiuixVerticalStretchOverscrollFactory }
}

/**
 * verticalScroll + short-content stretch.
 *
 * Mac dinh gan bridge *sat* scroll (parent gan nhat) khi KHONG co header
 * collapse. Man large title: dung [rememberMiuixHeaderContentScrollBehavior]
 * (mot owner) + [attachShortBridge]=false — khong stack bridge ngoai AppBar.
 * Long: bridge no-op; stretch boundary qua Scrollable gate.
 * enabled=false: bridge passive (khong claim nested).
 */
@Composable
fun Modifier.miuixVerticalScroll(
    state: ScrollState,
    enabled: Boolean = true,
    flingBehavior: FlingBehavior? = null,
    reverseScrolling: Boolean = false,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    attachShortBridge: Boolean = true,
): Modifier {
    if (overscrollEffect == null) {
        return this.verticalScroll(
            state = state,
            enabled = enabled,
            flingBehavior = flingBehavior,
            reverseScrolling = reverseScrolling,
        )
    }
    // remember luon (khong dat trong if) — tranh doi thu tu Compose.
    val enabledState = rememberUpdatedState(enabled)
    val bridge = remember(state, overscrollEffect) {
        ShortContentStretchNestedScrollConnection(
            canScroll = { state.canScrollForward || state.canScrollBackward },
            overscrollEffect = overscrollEffect,
            enabled = { enabledState.value },
        )
    }
    RememberStretchArbitrationRegistration(overscrollEffect)
    val base = if (attachShortBridge) this.nestedScroll(bridge) else this
    return base.verticalScroll(
        state = state,
        overscrollEffect = overscrollEffect,
        enabled = enabled,
        flingBehavior = flingBehavior,
        reverseScrolling = reverseScrolling,
    )
}

/**
 * Page short/empty/loading/error khong co verticalScroll: scrollableArea +
 * ScrollableState consume 0, canScroll mac dinh true → luon dispatch overscroll.
 * Khong fake height; khong phantom list offset (chi place qua effect.node).
 *
 * [overscrollEffect] bat buoc hoist cung coordinator (mot effect+node /
 * surface). Mac dinh LocalOverscrollFactory tao instance khac → PreScroll
 * releaseOppositeTension / settle lech node.
 */
@Composable
fun Modifier.miuixAlwaysVerticalStretch(
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
): Modifier {
    // Consume 0: moi delta leftover → stretch; canScrollF/B default true.
    val stretchState = rememberScrollableState { 0f }
    RememberStretchArbitrationRegistration(overscrollEffect)
    return this.scrollableArea(
        state = stretchState,
        orientation = Orientation.Vertical,
        overscrollEffect = overscrollEffect,
    )
}

/**
 * NestedScroll parent cua scrollable: chi an unconsumed khi child !canScroll.
 * Chi feed/claim truc Y — giu X cho HorizontalPager.
 * Pointer-up short: Scrollable shouldDispatchOverscroll=false → khong goi
 * applyToFling trong Scrollable; onDragStopped → onScrollStopped → performFling
 * → dispatchPostFling toi bridge (foundation Scrollable.kt). available.y==0
 * (slow drag) van goi effect khi isInProgress de settle — tranh stale stretch.
 * Khong khoa input cho den khi settle xong — effect van interrupt tren UserInput.
 */
internal class ShortContentStretchNestedScrollConnection(
    private val canScroll: () -> Boolean,
    private val overscrollEffect: OverscrollEffect,
    private val enabled: () -> Boolean = { true },
) : NestedScrollConnection {
    private fun bridgeActive(): Boolean = enabled() && !canScroll()

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        // Chi reverse tension (khong v1 stretch o Pre) — tranh an delta truoc child.
        // Large header: dung MiuixHeaderContentScroll (owner chung), khong bridge ngoai.
        // Gate UserInput nam trong releaseOppositeTension.
        if (!bridgeActive()) return Offset.Zero
        if (available.y == 0f) return Offset.Zero
        val effect = overscrollEffect
        if (effect is MiuixVerticalStretchOverscrollEffect) {
            val released = effect.releaseOppositeTension(available.y, source)
            return if (released == 0f) Offset.Zero else Offset(0f, released)
        }
        if (!effect.isInProgress) return Offset.Zero
        val yOnly = Offset(0f, available.y)
        val consumed = effect.applyToScroll(yOnly, source) { Offset.Zero }
        return Offset(0f, consumed.y)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!bridgeActive()) return Offset.Zero
        if (available.y == 0f) return Offset.Zero
        // performScroll = 0: leftover Y thanh stretch (jb2.b short). Chi feed Y.
        val yOnly = Offset(0f, available.y)
        val applied = overscrollEffect.applyToScroll(yOnly, source) { Offset.Zero }
        return Offset(0f, applied.y)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!bridgeActive()) return Velocity.Zero
        val vy = available.y
        // Zero-vel + dang tension: van settle (slow drag release).
        if (vy == 0f && !overscrollEffect.isInProgress) return Velocity.Zero
        val yOnly = Velocity(0f, vy)
        overscrollEffect.applyToFling(yOnly) { Velocity.Zero }
        // Chi claim Y — khong an fling ngang pager.
        return Velocity(0f, vy)
    }
}

/** Factory on dinh cho LocalOverscrollFactory. */
object MiuixVerticalStretchOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect =
        MiuixVerticalStretchOverscrollEffect()

    // Singleton: equals/hashCode theo hop dong OverscrollFactory.
    override fun hashCode(): Int = 0x4d495558

    override fun equals(other: Any?): Boolean = other === this
}

/**
 * Stretch doc: F raw clamp [-G,G], E = sign(F)*g(|F|/G)*G,
 * g(t)=t - t^2 + t^3/3 (rb2.v1). Ve: place (0, Math.rint(E)) — nb2/rm1/y1.
 */
internal class MiuixVerticalStretchOverscrollEffect : OverscrollEffect, StretchBusyOwner {
    private val stretchNode = MiuixStretchLayoutNode(this)

    /** Visual E (px). */
    internal var visualOffset by mutableFloatStateOf(0f)
        private set

    /** Raw F (px). */
    private var rawF = 0f

    /** Bounds G = height container (rb2.D1). */
    private var boundG = 0f

    /**
     * od3 overshoot: |E| > G/3 — reacquire lien tuc (khong snap E).
     * Xem [MiuixStretchReacquire].
     */
    private var unboundReacquire = false

    /** Fling op generation — child cu khong cancel/I0 de len gesture moi. */
    private var flingGeneration = 0L

    private var settleJob: Job? = null

    /** Pager DOWN defer / horizontal takeover — SSOT [StretchBusyDeferGate]. */
    internal val busyDefer = StretchBusyDeferGate(VisualEpsilonPx)

    override val isInProgress: Boolean
        get() = busyDefer.isInProgress()

    override val stretchIsInProgress: Boolean
        get() = isInProgress

    override val node: DelegatableNode
        get() = stretchNode

    /** Doc E hien tai — test preserve/takeover. */
    override fun peekVisualE(): Float = visualOffset

    private fun publishVisualE(e: Float) {
        visualOffset = e
        busyDefer.setVisualE(e)
    }

    /**
     * DOWN/busy: interrupt settle voi B1 (rawF=0 + E!=0 → reacquire),
     * an isInProgress, giu E. Khong cancelSettle thrang (mat B1 → nhay).
     */
    override fun deferBusyPreserveTension() {
        flingGeneration += 1
        val job = settleJob
        if (job != null) {
            settleJob = null
            job.cancel()
        }
        // B1 / unbound — ke ca khi settleJob==null (rawF da 0 sau od3).
        StretchBusyLifecycle.reacquireAfterSettleInterrupt(rawF, visualOffset, boundG)?.let { s ->
            rawF = s.rawF
            publishVisualE(s.visualE)
            unboundReacquire = s.unbound
        }
        busyDefer.deferBusyPreserveTension()
    }

    /** Vertical thang / huy arbitration: cho isInProgress theo E that. */
    override fun endPagerDeferPreserveTension() {
        busyDefer.endDeferPreserveTension()
    }

    /**
     * Horizontal takeover xac nhan: clear E/F (roi trang) + bo suppress.
     */
    override fun confirmHorizontalTakeoverClearTension() {
        flingGeneration += 1
        cancelSettle()
        clearTension()
        busyDefer.confirmHorizontalTakeoverClearTension()
    }

    /** UP/tap sau defer: neu con E va khong con settle → startSettle(0). */
    override fun resumeSettleIfNeeded() {
        if (settleJob != null) return
        if (abs(visualOffset) <= VisualEpsilonPx) return
        startSettle(0f)
    }

    /**
     * Test hook: prime trang thai settle-like (rawF=0, E!=0) truoc defer B1.
     */
    internal fun debugPrimeSettleLikeTensionForTest(e: Float, g: Float) {
        boundG = g
        rawF = 0f
        unboundReacquire = false
        publishVisualE(e)
    }

    /** Test hook: rawF sau B1/defer. */
    internal fun debugPeekRawFForTest(): Float = rawF

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (boundG <= VisualEpsilonPx) {
            return performScroll(delta)
        }

        // rb2.R0/D0: gate i==1 (UserInput) + truc doc; SideEffect/zero/Y=0
        // khong cancel settle dang chay (Scrollable fling -> SideEffect).
        // Residual: pointer C / z1 chua port - UserInput ~ i==1 (+C).
        val isVerticalUserInput =
            source == NestedScrollSource.UserInput && delta.y != 0f

        // Ngat settle + B1 chi khi user drag doc (truoc C1/v1 / R0 reverse).
        if (isVerticalUserInput &&
            (abs(visualOffset) > VisualEpsilonPx || settleJob?.isActive == true)
        ) {
            interruptSettleKeepTension()
        }

        // R0: reverse-before-child chi UserInput doc + dang stretch doi dau F.
        var remaining = delta
        var preReleasedY = 0f
        if (isVerticalUserInput && abs(visualOffset) > VisualEpsilonPx) {
            ensureRawFFromVisual()
            if (rawF != 0f && sign(remaining.y) != sign(rawF)) {
                preReleasedY = reverseReleaseAmount(remaining.y)
                remaining = Offset(remaining.x, remaining.y - preReleasedY)
            }
        }

        // Bat buoc goi dung 1 lan (OverscrollEffect 1.11.4).
        val consumedByScroll = performScroll(remaining)
        val leftoverY = remaining.y - consumedByScroll.y

        // D0 C1(consumed):
        // - UserInput: sau interrupt (neu co) -> C1
        // - SideEffect + settle active: skip (khong pha A1)
        // - SideEffect + settle idle: C1 giong D0 else (ee3 null/!b)
        val settleStillActive = settleJob?.isActive == true
        val allowC1 =
            source == NestedScrollSource.UserInput || !settleStillActive
        if (allowC1 &&
            abs(visualOffset) > VisualEpsilonPx &&
            consumedByScroll.y != 0f
        ) {
            reverseReleaseFromConsumed(consumedByScroll.y)
        }

        // D0/R0: v1(leftover) chi UserInput; SideEffect khong nap stretch.
        var postConsumed = 0f
        if (source == NestedScrollSource.UserInput && leftoverY != 0f) {
            applyStretchDelta(leftoverY)
            postConsumed = leftoverY
        }

        return Offset(
            consumedByScroll.x,
            consumedByScroll.y + postConsumed + preReleasedY,
        )
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        if (boundG <= VisualEpsilonPx) {
            performFling(velocity)
            return
        }

        val vy = velocity.y
        // Live E + generation ownership (MiuixFlingOrchestrator).
        MiuixFlingOrchestrator.runApplyToFling(
            readVisualE = { visualOffset },
            flingVy = vy,
            beginOperation = {
                flingGeneration += 1
                flingGeneration
            },
            isCurrentOperation = { it == flingGeneration },
            startSettle = { startSettle(it) },
            cancelSettle = { cancelSettle() },
            performFlingChild = { toChildY ->
                performFling(Velocity(velocity.x, toChildY)).y
            },
        )
    }

    /**
     * rb2.l1 detach: clear C + x1(). Huy settle (khong clobber job moi),
     * xoa F/E/G de reattach khong giu stale isInProgress.
     */
    internal fun onLayoutDetached() {
        flingGeneration += 1
        val job = settleJob
        settleJob = null
        job?.cancel()
        clearTension()
        boundG = 0f
    }

    internal fun updateBoundG(heightPx: Float) {
        // Zero-height: reset — tranh place pixel stale / remasure cycle.
        if (heightPx <= 0f) {
            if (boundG != 0f || rawF != 0f || visualOffset != 0f) {
                cancelSettle()
                clearTension()
                boundG = 0f
            }
            return
        }
        if (boundG == heightPx) return
        boundG = heightPx
        val next =
            MiuixStretchReacquire.afterBoundChange(
                MiuixStretchReacquire.State(rawF, visualOffset, unboundReacquire),
                boundG,
            )
        rawF = next.rawF
        publishVisualE(next.visualE)
        unboundReacquire = next.unbound
    }

    /** rb2.v1 + unbound reacquire (lien tuc). */
    private fun applyStretchDelta(delta: Float) {
        if (delta == 0f || boundG <= VisualEpsilonPx) return
        val next =
            MiuixStretchReacquire.applyDelta(
                MiuixStretchReacquire.State(rawF, visualOffset, unboundReacquire),
                delta,
                boundG,
            )
        rawF = next.rawF
        publishVisualE(next.visualE)
        unboundReacquire = next.unbound
    }

    /** Dam bao F khoi E (B1) truoc khi so sanh dau. */
    private fun ensureRawFFromVisual() {
        if (unboundReacquire) return
        if (rawF == 0f && abs(visualOffset) > VisualEpsilonPx) {
            rawF = MiuixStretchMath.inverseStretch(visualOffset, boundG)
        }
    }

    /**
     * PreScroll owner: chi reverse tension doi dau, KHONG v1(leftover).
     * Dung khi header/list can nhan delta sau khi tha stretch (tranh
     * applyToScroll(perform=0) vua reverse vua stretch het UP).
     *
     * Gate: chi vertical UserInput — SideEffect / zero / X-only KHONG interrupt
     * settle (tranh fling A1 tu-huy). User drag: interrupt R0 ke ca cung dau;
     * reverse chi khi doi dau F.
     */
    internal fun releaseOppositeTension(
        deltaY: Float,
        source: NestedScrollSource,
    ): Float {
        // Chi UserInput + Y != 0 (khop applyToScroll isVerticalUserInput).
        if (source != NestedScrollSource.UserInput || deltaY == 0f) return 0f
        if (boundG <= VisualEpsilonPx) return 0f
        val tensionOrSettle =
            abs(visualOffset) > VisualEpsilonPx || settleJob?.isActive == true
        if (!tensionOrSettle) return 0f
        // R0: ngat settle + B1 truoc reverse (user drag, ke ca cung dau).
        interruptSettleKeepTension()
        return reverseReleaseAmount(deltaY)
    }

    /**
     * R0 opposite-sign — unbound dung E tuyen tinh (MiuixStretchReacquire.oppositeRelease).
     * @return luong delta da dung de giai tension (cung dau voi delta).
     */
    private fun reverseReleaseAmount(deltaY: Float): Float {
        ensureRawFFromVisual()
        val result =
            MiuixStretchReacquire.oppositeRelease(
                MiuixStretchReacquire.State(rawF, visualOffset, unboundReacquire),
                deltaY,
                boundG,
            )
        rawF = result.state.rawF
        publishVisualE(result.state.visualE)
        unboundReacquire = result.state.unbound
        return result.consumedY
    }

    /** rb2.C1(consumed) — giai tension; unbound: E -= consumed lien tuc. */
    private fun reverseReleaseFromConsumed(consumed: Float) {
        if (abs(visualOffset) <= VisualEpsilonPx || consumed == 0f) return
        ensureRawFFromVisual()
        val next =
            MiuixStretchReacquire.releaseFromConsumed(
                MiuixStretchReacquire.State(rawF, visualOffset, unboundReacquire),
                consumed,
                boundG,
            )
        rawF = next.rawF
        publishVisualE(next.visualE)
        unboundReacquire = next.unbound
    }

    private fun clearTension() {
        rawF = 0f
        publishVisualE(0f)
        unboundReacquire = false
    }

    private fun interruptSettleKeepTension() {
        // Gesture moi invalidate fling op dang treo (tranh I0/cancel de len settle moi).
        flingGeneration += 1
        val job = settleJob ?: return
        settleJob = null
        job.cancel()
        // B1 / unbound reacquire — KHONG snap E (reject reconcileVisualAfterInterruptB1).
        if (rawF == 0f && abs(visualOffset) > VisualEpsilonPx) {
            val s = MiuixStretchReacquire.afterInterruptB1(visualOffset, boundG)
            rawF = s.rawF
            publishVisualE(s.visualE)
            unboundReacquire = s.unbound
        }
    }

    private fun cancelSettle() {
        val job = settleJob
        settleJob = null
        job?.cancel()
    }

    private fun startSettle(initialVelocity: Float) {
        cancelSettle()
        if (!stretchNode.isAttached) {
            if (abs(visualOffset) <= VisualEpsilonPx) clearTension()
            return
        }
        if (abs(visualOffset) <= VisualEpsilonPx && abs(initialVelocity) <= VisualEpsilonPx) {
            clearTension()
            return
        }
        val startE = visualOffset
        settleJob = stretchNode.coroutineScope.launch {
            try {
                runOd3Settle(startE, initialVelocity)
            } finally {
                // Chi clear neu van la job hien tai (tranh race cancel→start moi).
                if (settleJob === coroutineContext[Job]) {
                    settleJob = null
                }
            }
        }
    }

    /**
     * od3 + pd3 + lh2.D — tich phan ve 0; period theo |v|.
     * Anim ghi E truc tiep; rawF=0 de B1 khi interrupt.
     */
    private suspend fun runOd3Settle(from: Float, velocity: Float) {
        val period =
            if (abs(velocity.toDouble()) > SettlePeriodFastVelocity) {
                SettlePeriodHighVelocity
            } else {
                SettlePeriodNormal
            }
        val omega = (Math.PI * 2.0) / period
        val dampA = 2.0 * omega
        val stiffB = omega * omega
        var pos = from.toDouble()
        var vel = velocity.toDouble()
        val target = 0.0
        var prevPos = pos
        var lastFrame = -1L
        rawF = 0f
        unboundReacquire = false
        while (true) {
            val frame = withFrameNanos { it }
            if (lastFrame < 0L) {
                lastFrame = frame
                continue
            }
            val dt = ((frame - lastFrame) / 1_000_000_000.0).coerceIn(0.001, 0.016)
            lastFrame = frame
            vel = vel * (1.0 - dampA * dt) + (target - pos) * stiffB * dt
            pos += vel * dt
            val crossed =
                (prevPos >= target && pos <= target) ||
                    (prevPos <= target && pos >= target) ||
                    (prevPos == target)
            prevPos = pos
            if (crossed && abs(pos - target) < 1.0) {
                clearTension()
                return
            }
            if (abs(pos - target) < 1.0 && abs(vel) < 1.0) {
                clearTension()
                return
            }
            publishVisualE(pos.toFloat())
        }
    }

    companion object {
        /** Alias pure math — giu API test cu. */
        fun stretchVisual(rawF: Float, boundG: Float): Float =
            MiuixStretchMath.stretchVisual(rawF, boundG)

        fun inverseStretch(visualE: Float, boundG: Float): Float =
            MiuixStretchMath.inverseStretch(visualE, boundG)
    }
}

/**
 * Layout: D1 cap nhat G trong measure; place (0, Math.rint(E)).
 * Doc visualOffset o *placement* (khong measure) — stretch/settle moi frame
 * chi invalidate place, tranh remasure cay con (Options short + list dai).
 */
private class MiuixStretchLayoutNode(
    private val effect: MiuixVerticalStretchOverscrollEffect,
) : Modifier.Node(), LayoutModifierNode {
    override fun onDetach() {
        // rb2.l1: clear C + x1 — huy job + F/E/G (effect.onLayoutDetached).
        effect.onLayoutDetached()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        effect.updateBoundG(placeable.height.toFloat())
        return layout(placeable.width, placeable.height) {
            // rb2.y1 / nb2: java.lang.Math.rint (khong kotlin.math.rint).
            // Doc E trong placement scope — Snapshot chi invalidate place.
            val oy = Math.rint(effect.visualOffset.toDouble()).toInt()
            placeable.place(0, oy)
        }
    }
}
