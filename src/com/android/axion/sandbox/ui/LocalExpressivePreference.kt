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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.android.axion.sandbox.R

enum class LocalPrefPosition { Single, Top, Middle, Bottom }

fun localPrefPosition(index: Int, count: Int): LocalPrefPosition = when {
    count <= 1 -> LocalPrefPosition.Single
    index == 0 -> LocalPrefPosition.Top
    index == count - 1 -> LocalPrefPosition.Bottom
    else -> LocalPrefPosition.Middle
}

/**
 * Hieu ung nhan co lai theo chuan Miuix / Expressive.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.miuixPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier {
    if (!enabled) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val motionScheme = MaterialTheme.motionScheme
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) {
            motionScheme.fastSpatialSpec()
        } else {
            motionScheme.defaultSpatialSpec()
        },
        label = "miuix_press_scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private fun localPrefShape(position: LocalPrefPosition) = when (position) {
    LocalPrefPosition.Single -> RoundedCornerShape(20.dp)
    LocalPrefPosition.Top -> RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
    )
    LocalPrefPosition.Middle -> RoundedCornerShape(4.dp)
    LocalPrefPosition.Bottom -> RoundedCornerShape(
        topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp,
    )
}

/**
 * Hang preference local: min 72dp, padding doc CHI tren text frame;
 * icon frame 40dp, text paddingEnd 16dp; chevron Settings Expressive 18dp.
 */
@Composable
fun LocalExpressivePreferenceRow(
    title: String,
    position: LocalPrefPosition,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    /** leading custom (vd. app icon painter) — uu tien hon [icon]. */
    leading: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = localPrefShape(position)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .miuixPressScale(interactionSource, enabled = onClick != null)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
                Spacer(Modifier.width(12.dp))
            }
            icon != null -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
                .padding(end = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_expressive_chevron),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Hang app boc the Expressive theo chuan danh sach AOSP.
 */
@Composable
fun AppListPreferenceItem(
    app: AppInfo,
    position: LocalPrefPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val protectionStates = remember(
        app.isLocked,
        app.isHidden,
        app.isSandboxed,
        primaryColor,
        tertiaryColor,
        secondaryColor,
    ) {
        listOfNotNull(
            if (app.isLocked) Pair(Icons.Filled.Lock, primaryColor) else null,
            if (app.isHidden) Pair(Icons.Filled.VisibilityOff, tertiaryColor) else null,
            if (app.isSandboxed) Pair(Icons.Filled.Security, secondaryColor) else null,
        )
    }

    LocalExpressivePreferenceRow(
        title = app.label,
        summary = app.packageName,
        position = position,
        interactionSource = interactionSource,
        modifier = modifier,
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = app.iconPainter,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (protectionStates.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for ((iconVector, tint) in protectionStates) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_settings_expressive_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
fun LocalPreferenceGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable LocalPreferenceGroupScope.() -> Unit,
) {
    // Goi content() trong composition — khong dung apply() non-composable.
    val scope = LocalPreferenceGroupScope()
    scope.content()
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val n = scope.items.size
            scope.items.forEachIndexed { index, item ->
                val pos = when {
                    n == 1 -> LocalPrefPosition.Single
                    index == 0 -> LocalPrefPosition.Top
                    index == n - 1 -> LocalPrefPosition.Bottom
                    else -> LocalPrefPosition.Middle
                }
                item(pos)
            }
        }
    }
}

class LocalPreferenceGroupScope {
    internal val items = mutableListOf<@Composable (LocalPrefPosition) -> Unit>()
    fun row(content: @Composable (LocalPrefPosition) -> Unit) {
        items += content
    }
}

@Composable
fun LocalClickablePreference(
    title: String,
    position: LocalPrefPosition,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    LocalExpressivePreferenceRow(
        title = title,
        position = position,
        modifier = modifier,
        summary = summary,
        icon = icon,
        showChevron = showChevron,
        onClick = onClick,
    )
}

@Composable
fun LocalSwitchPreference(
    title: String,
    position: LocalPrefPosition,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    LocalExpressivePreferenceRow(
        title = title,
        position = position,
        modifier = modifier,
        summary = summary,
        icon = icon,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun LocalBasePreference(
    title: String,
    position: LocalPrefPosition,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
) {
    LocalExpressivePreferenceRow(
        title = title,
        position = position,
        modifier = modifier,
        summary = summary,
        icon = icon,
    )
}

@Composable
fun LocalListPreference(
    title: String,
    position: LocalPrefPosition,
    options: List<Pair<String, String>>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second
    val displaySummary = selectedLabel ?: summary
    LocalExpressivePreferenceRow(
        title = title,
        position = position,
        modifier = modifier,
        summary = displaySummary,
        icon = icon,
        showChevron = true,
        onClick = if (enabled) {
            { showDialog = true }
        } else {
            null
        },
    )
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                ) {
                    options.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = key == value,
                                    onClick = {
                                        onValueChange(key)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = key == value,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }
}
