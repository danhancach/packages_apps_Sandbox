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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Submenu collapsing header (Preferences / Support): Folme 1/3 + Expressive back
 * geometry (circle40 / optical16 / gap22 → title~78dp) / collapsed 22sp Medium.
 * Main-tab [SequentialFadeLargeFlexibleTopAppBar] khong dung component nay.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiuixSubmenuTopAppBar(
    title: @Composable () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
    val titleBottomPadding = 32.dp
    val collapsedTitleStart = MiuixTitleMotion.collapsedTitleStartDp().dp
    val expandedPad = MiuixTitleMotion.ExpandedTitleHorizontalDp.dp
    val collapsedTitleStyle = MaterialTheme.typography.titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = MiuixTitleMotion.CollapsedTitleSp.sp,
    )
    val expandedTitleStyle = MaterialTheme.typography.displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
    )

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
            .zIndex(1f)
            .then(appBarDragModifier)
            .drawBehind { drawRect(color = containerColor) }
            .semantics { isTraversalGroup = true },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(windowInsets)
                    .height(collapsedHeight),
            ) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveBackButton(onClick = onBack)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(
                            start = collapsedTitleStart,
                            end = 64.dp,
                        )
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
                        LocalTextStyle provides collapsedTitleStyle,
                        content = title,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }

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
                            LocalTextStyle provides expandedTitleStyle,
                            content = title,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                    .clipToBounds(),
            ) { measurables, constraints ->
                val horizontalPadding = expandedPad.roundToPx()
                val maxTitleWidth =
                    (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0)
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
