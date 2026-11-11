/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// r22 easing: uy quyen [MiuixPageMotion.easingR22]
private val MiuixPageEasing = Easing { fraction ->
    MiuixPageMotion.easingR22(fraction)
}

// Full navigation tu trang thai nghi: velocity == 0f va abs(target - current) >= 0.999f
private val MiuixFullPageSpec = tween<Float>(
    durationMillis = MiuixPageMotion.FullPageDurationMs,
    delayMillis = 0,
    easing = MiuixPageEasing,
)

// Interrupted partial navigation / settle / retarget: spring damping 1, stiffness 146, threshold 0.0025 trang
private val MiuixSettleSpec = spring<Float>(
    dampingRatio = MiuixPageMotion.SettleDampingRatio,
    stiffness = MiuixPageMotion.SettleStiffness,
    visibilityThreshold = MiuixPageMotion.SettleThreshold,
)

/** Coi p ~ 0: child khong con che parent (a11y + settled cleanup). */
private const val SettledEpsilon = MiuixPageMotion.SettleThreshold

/**
 * Anh xa BackEvent.progress (0..1 he thong) -> page progress Miuix.
 * APPROXIMATION (chua verify runtime): linear tren [0, anchor].
 */
internal fun pageProgressFromPredictiveBack(
    backProgress: Float,
    anchorPageProgress: Float,
): Float = MiuixPageMotion.pageProgressFromPredictiveBack(backProgress, anchorPageProgress)

/**
 * Scrub predictive Back mang session identity qua boundary handler → transition.
 */
data class PredictiveBackScrub(
    val sessionId: Long,
    /** 0..1 = [androidx.activity.BackEventCompat.progress]. */
    val progress: Float,
)

private data class MiuixDrive(
    val route: SandboxSubRoute,
    /** null = khong scrub; non-null = session + BackEvent.progress dang predictive. */
    val predictive: PredictiveBackScrub?,
)

/**
 * Host chuyen trang submenu chuan Miuix:
 * - Mot Animatable progress (owner duy nhat); tween500 / spring146 retarget + velocity.
 * - Parent/child dung absoluteOffset (hit-test theo visual) — child khong full-size ghost block.
 *   Khong dung Modifier.offset + direction: offset da RTL-aware → invert 2 lan.
 * - Pop ve ROOT: parent pointer mo (vung lo); child zIndex cao + offset → khong click-through
 *   duoi phan con ve; a11y parent an khi child chua settled.
 * - Handoff child khac: queue pending, pop p→0, swap, push p→1 (khong abrupt replace).
 * - Same-child reopen: giu activeChild / Saveable key — khong reset scroll.
 * - Predictive release: [MiuixPageMotion.predictiveReleaseVelocity] sau scrub.
 */
@Composable
fun MiuixPageTransition(
    targetState: SandboxSubRoute,
    modifier: Modifier = Modifier,
    /** null khi khong co predictive gesture; sessionId + progress khi dang keo. */
    predictiveBack: PredictiveBackScrub? = null,
    /** Goi khi pop settled ve ROOT (activeChild cleared) — clear snapshot UI. */
    onSettledAtRoot: (() -> Unit)? = null,
    rootContent: @Composable () -> Unit,
    childContent: @Composable (SandboxSubRoute) -> Unit,
) {
    var activeChild by remember {
        mutableStateOf(if (targetState != SandboxSubRoute.ROOT) targetState else null)
    }
    // Child moi cho sau khi outgoing pop xong (khong thay content giua slide).
    var pendingChild by remember { mutableStateOf<SandboxSubRoute?>(null) }
    val progress = remember {
        Animatable(if (targetState == SandboxSubRoute.ROOT) 0f else 1f)
    }
    var layoutWidthPx by remember { mutableFloatStateOf(0f) }
    val latestTarget by rememberUpdatedState(targetState)
    val latestPredictive by rememberUpdatedState(predictiveBack)
    val latestOnSettledAtRoot by rememberUpdatedState(onSettledAtRoot)

    val progressValue by progress.asState()
    val settledNearRoot = progressValue <= SettledEpsilon

    // Pointer parent: block khi day toi child / predictive / dang handoff.
    // Pop ve ROOT: parent nhan hit o vung da lo (child offset da dich).
    val parentPointerBlocked =
        predictiveBack != null ||
            targetState != SandboxSubRoute.ROOT ||
            pendingChild != null
    // A11y/focus parent: an khi child con ve / scrub / chua settled.
    val parentA11yBlocked =
        predictiveBack != null ||
            activeChild != null ||
            pendingChild != null ||
            !settledNearRoot ||
            targetState != SandboxSubRoute.ROOT

    LaunchedEffect(Unit) {
        var animationJob: Job? = null
        var anchoredSessionId: Long? = null
        var predictiveAnchor: Float? = null
        var lastScrubProgress = progress.value
        var lastScrubNanos = 0L
        var lastScrubVelocity = 0f

        fun consumeScrubReleaseVelocity(target: Float): Float {
            if (lastScrubNanos == 0L) return progress.velocity
            val swipe = lastScrubVelocity
            lastScrubNanos = 0L
            lastScrubVelocity = 0f
            return MiuixPageMotion.predictiveReleaseVelocity(
                currentProgress = progress.value,
                target = target,
                savedSwipeVelocity = swipe,
            )
        }

        /**
         * Sau khi p ve ~0: swap pending handoff trong CUNG coroutine (tranh
         * cancelAndJoin chinh minh), hoac don activeChild.
         */
        suspend fun completeTowardRoot(retainedVel: Float) {
            if (latestPredictive != null) return
            if (progress.value > SettledEpsilon) return

            val pending = pendingChild
            when {
                pending != null && latestTarget == pending -> {
                    activeChild = pending
                    pendingChild = null
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = MiuixSettleSpec,
                        initialVelocity = retainedVel,
                    )
                }
                pending != null && latestTarget != SandboxSubRoute.ROOT &&
                    latestTarget != pending -> {
                    activeChild = latestTarget
                    pendingChild = null
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = MiuixSettleSpec,
                        initialVelocity = retainedVel,
                    )
                }
                pending != null && latestTarget == SandboxSubRoute.ROOT -> {
                    pendingChild = null
                    activeChild = null
                    latestOnSettledAtRoot?.invoke()
                }
                latestTarget == SandboxSubRoute.ROOT -> {
                    activeChild = null
                    latestOnSettledAtRoot?.invoke()
                }
            }
        }

        fun launchDrive(block: suspend () -> Unit) {
            animationJob = launch {
                block()
            }
        }

        snapshotFlow {
            MiuixDrive(
                route = latestTarget,
                predictive = latestPredictive,
            )
        }
            .distinctUntilChanged()
            .collect { drive ->
                val isRoot = drive.route == SandboxSubRoute.ROOT
                val scrub = drive.predictive

                if (scrub != null) {
                    animationJob?.cancelAndJoin()
                    animationJob = null
                    if (anchoredSessionId != scrub.sessionId) {
                        predictiveAnchor = progress.value.coerceIn(0f, 1f)
                        anchoredSessionId = scrub.sessionId
                        lastScrubNanos = 0L
                        lastScrubVelocity = 0f
                    }
                    val anchor = predictiveAnchor
                        ?: progress.value.coerceIn(0f, 1f).also { predictiveAnchor = it }
                    val mapped = pageProgressFromPredictiveBack(scrub.progress, anchor)
                    val now = System.nanoTime()
                    if (lastScrubNanos != 0L && now > lastScrubNanos) {
                        val dt = (now - lastScrubNanos) / 1_000_000_000f
                        if (dt > 0.0001f) {
                            lastScrubVelocity =
                                ((mapped - lastScrubProgress) / dt).coerceIn(-50f, 50f)
                        }
                    }
                    lastScrubProgress = mapped
                    lastScrubNanos = now
                    progress.snapTo(mapped)
                    return@collect
                }

                anchoredSessionId = null
                predictiveAnchor = null

                // Huy job cu; giu velocity truoc cancel (Animatable cancel reset velocity).
                val retainedVel = progress.velocity
                animationJob?.cancelAndJoin()
                animationJob = null

                if (!isRoot) {
                    val current = activeChild
                    when {
                        current == null -> {
                            activeChild = drive.route
                            pendingChild = null
                            launchDrive {
                                val fromScrub = lastScrubNanos != 0L
                                val vel = if (fromScrub) {
                                    consumeScrubReleaseVelocity(1f)
                                } else {
                                    retainedVel
                                }
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = if (
                                        !fromScrub &&
                                        MiuixPageMotion.useFullPageTween(
                                            vel,
                                            progress.value,
                                            1f,
                                        )
                                    ) {
                                        MiuixFullPageSpec
                                    } else {
                                        MiuixSettleSpec
                                    },
                                    initialVelocity = vel,
                                )
                            }
                        }
                        current == drive.route -> {
                            pendingChild = null
                            launchDrive {
                                // Cancel predictive: spring ve mo (p=1) + release velocity.
                                val vel = if (lastScrubNanos != 0L) {
                                    consumeScrubReleaseVelocity(1f)
                                } else {
                                    retainedVel
                                }
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = MiuixSettleSpec,
                                    initialVelocity = vel,
                                )
                            }
                        }
                        else -> {
                            pendingChild = drive.route
                            launchDrive {
                                val vel = if (lastScrubNanos != 0L) {
                                    consumeScrubReleaseVelocity(0f)
                                } else {
                                    retainedVel
                                }
                                progress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = MiuixSettleSpec,
                                    initialVelocity = vel,
                                )
                                completeTowardRoot(progress.velocity)
                            }
                        }
                    }
                    return@collect
                }

                pendingChild = null
                launchDrive {
                    val fromScrub = lastScrubNanos != 0L
                    val vel = if (fromScrub) {
                        consumeScrubReleaseVelocity(0f)
                    } else {
                        retainedVel
                    }
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = if (
                            !fromScrub &&
                            MiuixPageMotion.useFullPageTween(
                                vel,
                                progress.value,
                                0f,
                            )
                        ) {
                            MiuixFullPageSpec
                        } else {
                            MiuixSettleSpec
                        },
                        initialVelocity = vel,
                    )
                    completeTowardRoot(progress.velocity)
                }
            }
    }

    // Huong vat ly: LTR child vao tu phai (+x), RTL tu trai (-x).
    // Bat buoc absoluteOffset: Modifier.offset (rtlAware) da doi dau x theo LD —
    // neu van nhan direction=-1 o RTL thi invert 2 lan (API:
    // foundation.layout.offset / absoluteOffset).
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val p = progressValue.coerceIn(0f, 1f)
    val widthPx = layoutWidthPx
    val parent = MiuixPageMotion.parentLayer(p, widthPx, isRtl)
    val child = MiuixPageMotion.childLayer(p, widthPx, isRtl)
    val scrimA = MiuixPageMotion.scrimAlpha(p)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { layoutWidthPx = it.width.toFloat() },
    ) {
        // z0 parent: parallax -W/4 + ParentFadeFactor (ek1/h32.a). Plate surfaceContainer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                .absoluteOffset {
                    IntOffset(parent.translationX.roundToInt(), 0)
                }
                .graphicsLayer {
                    alpha = parent.alpha
                }
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .gatePageInteraction(
                    pointerBlocked = parentPointerBlocked,
                    a11yBlocked = parentA11yBlocked,
                ),
        ) {
            rootContent()
        }

        // z0.5 scrim: m7.h black Box truoc top page; ub26 / q12.d = NavScrimMaxAlpha * p.
        // Dung yen (khong absoluteOffset). Chi khi stack con child / p>0.
        if (activeChild != null || p > SettledEpsilon) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f)
                    .graphicsLayer {
                        alpha = scrimA
                    }
                    .background(Color.Black),
            )
        }

        val currentChild = activeChild
        if (currentChild != null) {
            val childInteractive =
                targetState == currentChild &&
                    predictiveBack == null &&
                    pendingChild == null
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    // Child: W*(1-p) vat ly — hit-test theo bounds da dich, khong click-through.
                    .absoluteOffset {
                        IntOffset(child.translationX.roundToInt(), 0)
                    }
                    .gatePageInteraction(
                        pointerBlocked = !childInteractive,
                        a11yBlocked = !childInteractive,
                    ),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                key(currentChild) {
                    childContent(currentChild)
                }
            }
        }
    }
}

/**
 * Tach pointer consume vs a11y/focus:
 * - pointerBlocked: consume Initial (khong click-through).
 * - a11yBlocked: clear semantics + khong focus.
 */
private fun Modifier.gatePageInteraction(
    pointerBlocked: Boolean,
    a11yBlocked: Boolean,
): Modifier {
    var result = this
    if (a11yBlocked) {
        result = result
            .focusProperties { canFocus = false }
            .clearAndSetSemantics { }
    }
    if (pointerBlocked) {
        result = result.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
    return result
}
