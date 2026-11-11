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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.axion.sandbox.R
import kotlin.math.roundToInt

/** Chieu cao hang tab noi dung co dinh 64dp theo chuan AOSP / Google Clock SSOT. */
val SandboxBottomTabRowHeight = SandboxBottomNavGeometry.MinRowHeightDp.dp

private val PillWidth = SandboxBottomNavGeometry.PillWidthDp.dp
private val PillHeight = SandboxBottomNavGeometry.PillHeightDp.dp
private val PillCorner = SandboxBottomNavGeometry.PillCornerDp.dp
private val NavIconSize = SandboxBottomNavGeometry.IconSizeDp.dp
private val TopSpace = SandboxBottomNavGeometry.TopSpaceDp.dp
private val IndicatorToLabelSpace = SandboxBottomNavGeometry.IndicatorToLabelSpaceDp.dp

private data class SandboxBottomTabSpec(
    val index: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
)

/**
 * Thanh dieu huong day chuan Material 3 Expressive (.docs/ui/chuan-thiet-ke-bottombar-material3.md):
 * - Chieu cao noi dung: co dinh 64dp (khong phu thuoc fontScale)
 * - Active indicator: 56x32dp, bo goc 16dp (50% stadium)
 * - Icon: 24x24dp can chinh giua indicator
 * - Khoang cach Pill -> Text: dung 4dp
 * - Co chu: 12sp inactive / 14sp active
 * - Tat LocalMinimumInteractiveComponentSize de tranh phinh 48dp pha vo ty le
 * - Surface phu kin ca hang tab lan WindowInsets day de khong ho chan trong suot
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SandboxBottomBar(
    selectedPagePosition: Float,
    notificationCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            SandboxBottomTabSpec(
                index = 0,
                selectedIcon = Icons.Filled.Apps,
                unselectedIcon = Icons.Outlined.Apps,
                labelRes = R.string.tab_apps,
            ),
            SandboxBottomTabSpec(
                index = 1,
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                labelRes = R.string.tab_notifications,
            ),
            SandboxBottomTabSpec(
                index = 2,
                selectedIcon = Icons.Filled.FolderOpen,
                unselectedIcon = Icons.Outlined.FolderOpen,
                labelRes = R.string.tab_vault,
            ),
        )
    }
    val selectedIndex = selectedPagePosition.roundToInt().coerceIn(0, tabs.lastIndex)
    val contentTween = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val density = LocalDensity.current
    val rowHeightValue = SandboxBottomNavGeometry.rowHeightDp(density.fontScale)
    val blockTopValue = SandboxBottomNavGeometry.blockTopDp(rowHeightValue)
    val rowHeight = rowHeightValue.dp
    val blockTop = blockTopValue.dp

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false,
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                ) {
                    val itemCount = tabs.size.coerceAtLeast(1)
                    val itemWidth = maxWidth / itemCount
                    val centerX = itemWidth * selectedPagePosition + itemWidth / 2
                    val pillOffsetX = centerX - PillWidth / 2

                    // Active indicator pill luot theo vi tri page: 56x32dp tai Y = 6dp
                    Box(
                        modifier = Modifier
                            .offset(x = pillOffsetX, y = blockTop)
                            .size(width = PillWidth, height = PillHeight)
                            .clip(RoundedCornerShape(PillCorner))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .selectableGroup(),
                    ) {
                        tabs.forEachIndexed { index, spec ->
                            val selected = index == selectedIndex
                            val label = stringResource(spec.labelRes)
                            val contentAlpha by animateFloatAsState(
                                targetValue = if (selected) 1f else 0.75f,
                                animationSpec = contentTween,
                                label = "sandbox_nav_alpha_$index",
                            )
                            val hitSource = remember(spec.index) { MutableInteractionSource() }
                            val rippleSource = remember(spec.index) { MutableInteractionSource() }
                            val pillWidthPx = with(density) { PillWidth.toPx() }
                            val pillHeightPx = with(density) { PillHeight.toPx() }

                            LaunchedEffect(hitSource, rippleSource, pillWidthPx, pillHeightPx) {
                                val pressMap =
                                    mutableMapOf<PressInteraction.Press, PressInteraction.Press>()
                                try {
                                    hitSource.interactions.collect { interaction ->
                                        when (interaction) {
                                            is PressInteraction.Press -> {
                                                val mapped = PressInteraction.Press(
                                                    Offset(pillWidthPx / 2f, pillHeightPx / 2f),
                                                )
                                                pressMap[interaction] = mapped
                                                rippleSource.emit(mapped)
                                            }
                                            is PressInteraction.Release -> {
                                                val mapped = pressMap[interaction.press]
                                                if (mapped != null) {
                                                    rippleSource.emit(
                                                        PressInteraction.Release(mapped),
                                                    )
                                                    pressMap.remove(interaction.press)
                                                }
                                            }
                                            is PressInteraction.Cancel -> {
                                                val mapped = pressMap[interaction.press]
                                                if (mapped != null) {
                                                    rippleSource.emit(
                                                        PressInteraction.Cancel(mapped),
                                                    )
                                                    pressMap.remove(interaction.press)
                                                }
                                            }
                                            else -> rippleSource.emit(interaction)
                                        }
                                    }
                                } finally {
                                    if (pressMap.isNotEmpty()) {
                                        val pending = pressMap.values.toList()
                                        pressMap.clear()
                                        for (mapped in pending) {
                                            rippleSource.tryEmit(PressInteraction.Cancel(mapped))
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .selectable(
                                        selected = selected,
                                        onClick = { onTabSelected(spec.index) },
                                        role = Role.Tab,
                                        interactionSource = hitSource,
                                        indication = null,
                                    )
                                    .miuixPressScale(hitSource)
                                    .padding(top = blockTop),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // 1. Indicator Box 56x32dp chua Icon 24dp can giua
                                Box(
                                    modifier = Modifier
                                        .size(width = PillWidth, height = PillHeight)
                                        .clip(RoundedCornerShape(PillCorner))
                                        .indication(
                                            interactionSource = rippleSource,
                                            indication = ripple(bounded = true),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (selected) {
                                            spec.selectedIcon
                                        } else {
                                            spec.unselectedIcon
                                        },
                                        contentDescription = if (spec.index == 1 && notificationCount > 0) {
                                            stringResource(R.string.notifications_badge_count, notificationCount)
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .size(NavIconSize)
                                            .alpha(contentAlpha),
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    if (spec.index == 1 && notificationCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = if (notificationCount > 9) {
                                                    "9+"
                                                } else {
                                                    notificationCount.toString()
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onError,
                                            )
                                        }
                                    }
                                }

                                // 2. Nhan Text: cach Pill dung 4dp, dong nhat 12sp khong phong to khi chon
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    lineHeight = 15.6.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .padding(top = IndicatorToLabelSpace)
                                        .alpha(contentAlpha),
                                )
                            }
                        }
                    }
                }
                // Phu kin vung navigation bar insets day man hinh
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(NavigationBarDefaults.windowInsets),
                )
            }
        }
    }
}
