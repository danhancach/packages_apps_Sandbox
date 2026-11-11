/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Large flexible app bar — Miuix Folme title (khong Settings sequential half/half).
 *
 * - Large: alpha = 1 - clamp(collapsedFraction * 3) (mo het trong 1/3 hanh trinh).
 * - Small: kich hoat khi collapsedFraction * 3 >= 1; Folme spring bat doi xung
 *   show 0.3s / hide 0.15s, translationY 20px vat ly, retarget giu van toc.
 * Giu chieu cao / inset / typography / nestedScroll qua [scrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SequentialFadeLargeFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    require(expandedHeight >= collapsedHeight) {
        "expandedHeight phai >= collapsedHeight"
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val expandedPx = with(density) { expandedHeight.toPx() }
    val collapsedHeightPx = with(density) { collapsedHeight.toPx() }
    val bottomRowHeightPx = expandedPx - collapsedHeightPx
    val titleBottomPadding = 28.dp

    val scrollState = scrollBehavior?.state

    val collapsedFraction = scrollState?.collapsedFraction ?: 0f
    val alphaLarge = MiuixTitleMotion.largeTitleAlpha(collapsedFraction)
    val folme = rememberMiuixSmallTitleFolme(scrollState) {
        scrollState?.collapsedFraction ?: 0f
    }
    val alphaSmall by folme.alpha
    val translationSmall by folme.translationY
    val hideSmallSemantics = alphaSmall <= 0f
    val hideLargeSemantics = alphaLarge <= 0f

    // Updater gan container == scrolledContainer — khong can lerp internal
    val containerColor = colors.containerColor

    val appBarDragModifier =
        if (scrollBehavior != null && !scrollBehavior.isPinned) {
            Modifier.draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scrollBehavior.state.heightOffset += delta
                },
                onDragStopped = { velocity ->
                    scope.launch {
                        settleMiuixAppBarScroll(scrollBehavior, velocity)
                    }
                },
            )
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .then(appBarDragModifier)
            .drawBehind { drawRect(color = containerColor) }
            .semantics { isTraversalGroup = true },
    ) {
        Column {
            // Hang collapsed: nav + small title + actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(windowInsets)
                    .height(collapsedHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Row start 4dp + nav slot min 8dp + title padding 4dp = 16dp.
                // A real navigation icon keeps its full width and existing title gap.
                Box(Modifier.widthIn(min = 8.dp)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.navigationIconContentColor,
                        content = navigationIcon,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .then(
                            if (hideSmallSemantics) {
                                Modifier.clearAndSetSemantics {}
                            } else {
                                Modifier
                            },
                        )
                        .graphicsLayer {
                            alpha = alphaSmall
                            translationY = translationSmall
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides MaterialTheme.typography.titleLarge,
                        content = title,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionIconContentColor,
                    ) {
                        actions()
                    }
                }
            }

            // Hang expanded: do title tu nhien khong bi constraint chieu cao thu hep,
            // tinh baseline padding va clip placement theo dung Material3 TwoRowsTopAppBar
            Layout(
                content = {
                    Box(
                        modifier = Modifier
                            .layoutId("title")
                            .fillMaxWidth()
                            .then(
                                if (hideLargeSemantics) {
                                    Modifier.clearAndSetSemantics {}
                                } else {
                                    Modifier
                                },
                            )
                            .graphicsLayer { alpha = alphaLarge },
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.titleContentColor,
                            LocalTextStyle provides MaterialTheme.typography.displaySmall,
                            content = title,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                    .clipToBounds(),
            ) { measurables, constraints ->
                // SettingsLib Expressive expandedTitleMarginStart / GameSpace: 24dp.
                val horizontalPadding =
                    MiuixTitleMotion.ExpandedTitleHorizontalDp.dp.roundToPx()
                val maxTitleWidth = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0)
                // Do title voi maxHeight khong bi gioi han de tranh bi bo hep / xuong dong / cat ngang
                val titlePlaceable = measurables
                    .first { it.layoutId == "title" }
                    .measure(
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = maxTitleWidth,
                            minHeight = 0,
                            maxHeight = Constraints.Infinity,
                        ),
                    )

                val titleBaseline =
                    if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                        titlePlaceable[LastBaseline]
                    } else {
                        titlePlaceable.height
                    }

                val titleBottomPaddingPx = titleBottomPadding.roundToPx()
                val paddingFromBottom =
                    (titleBottomPaddingPx - (titlePlaceable.height - titleBaseline))
                        .coerceAtLeast(0)
                val maxLayoutHeight = maxOf(
                    bottomRowHeightPx.roundToInt(),
                    titlePlaceable.height + paddingFromBottom,
                )

                // Cap nhat heightOffsetLimit theo dung chieu cao can thiet (ho tro fontScale)
                val currentLimit = -maxLayoutHeight.toFloat()
                if (scrollState != null && scrollState.heightOffsetLimit != currentLimit) {
                    scrollState.heightOffsetLimit = currentLimit
                    scrollState.heightOffset =
                        scrollState.heightOffset.coerceIn(currentLimit, 0f)
                }

                val heightOffset = scrollState?.heightOffset ?: 0f
                val layoutHeight = constraints.constrainHeight(
                    (maxLayoutHeight + heightOffset.roundToInt()).coerceAtLeast(0),
                )
                val titleY = layoutHeight - titlePlaceable.height - paddingFromBottom

                layout(constraints.maxWidth, layoutHeight) {
                    titlePlaceable.placeRelative(horizontalPadding, titleY)
                }
            }
        }
    }
}
