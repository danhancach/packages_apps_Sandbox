/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.sandbox.R

/**
 * SettingsLib Expressive / GameSpace back: vong 40dp + mui ten 16dp path chinh xac,
 * hit >=48dp, autoMirror RTL, surfaceContainerHighest / onSurfaceVariant.
 */
private val ExpressiveBackArrowPathData = addPathNodes(
    "M3.626,9L8.526,13.9C8.726,14.1 8.817,14.333 8.801,14.6C8.801,14.867 8.701,15.1 " +
        "8.501,15.3C8.301,15.483 8.067,15.583 7.801,15.6C7.534,15.6 7.301,15.5 7.101,15.3L0.501," +
        "8.7C0.401,8.6 0.326,8.492 0.276,8.375C0.242,8.258 0.226,8.133 0.226,8C0.226,7.867 " +
        "0.242,7.742 0.276,7.625C0.326,7.508 0.401,7.4 0.501,7.3L7.101,0.7C7.284,0.517 7.509," +
        "0.425 7.776,0.425C8.059,0.425 8.301,0.517 8.501,0.7C8.701,0.9 8.801,1.142 8.801," +
        "1.425C8.801,1.692 8.701,1.925 8.501,2.125L3.626,7H14.801C15.084,7 15.317,7.1 15.501," +
        "7.3C15.701,7.483 15.801,7.717 15.801,8C15.801,8.283 15.701,8.525 15.501,8.725C15.317," +
        "8.908 15.084,9 14.801,9H3.626Z",
)

internal val ExpressiveBackArrow: ImageVector = ImageVector.Builder(
    name = "ExpressiveBackArrow",
    defaultWidth = MiuixTitleMotion.BackArrowDp.dp,
    defaultHeight = MiuixTitleMotion.BackArrowDp.dp,
    viewportWidth = 16f,
    viewportHeight = 16f,
    autoMirror = true,
).addPath(
    pathData = ExpressiveBackArrowPathData,
    fill = SolidColor(Color.Black),
).build()

@Composable
fun ExpressiveBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startPad = remember {
        MiuixTitleMotion.backIconButtonStartPaddingDp().dp
    }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(start = startPad)
            .size(MiuixTitleMotion.BackTouchMinDp.dp),
    ) {
        Box(
            modifier = Modifier
                .size(MiuixTitleMotion.BackCircleDp.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ExpressiveBackArrow,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MiuixTitleMotion.BackArrowDp.dp),
            )
        }
    }
}
