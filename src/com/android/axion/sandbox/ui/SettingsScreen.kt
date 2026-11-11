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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Pattern
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.sandbox.R
import com.android.axion.sandbox.security.LockedAppBehavior
import com.android.axion.sandbox.security.PrivateSectionBehavior
import com.android.axion.sandbox.security.SecurityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSecurityType: SecurityType,
    currentLockedAppBehavior: LockedAppBehavior,
    currentLockedAppTimeout: Int,
    currentPrivateBehavior: PrivateSectionBehavior,
    currentPrivateTimeout: Int,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    isPreferBiometric: Boolean,
    hasSecurityQuestion: Boolean,
    onBackClick: () -> Unit,
    onChangeSecurityType: (SecurityType) -> Unit,
    onChangeLockedAppBehavior: (LockedAppBehavior) -> Unit,
    onChangeLockedAppTimeout: (Int) -> Unit,
    onChangePrivateBehavior: (PrivateSectionBehavior) -> Unit,
    onChangePrivateTimeout: (Int) -> Unit,
    onChangeBiometricEnabled: (Boolean) -> Unit,
    onChangePreferBiometric: (Boolean) -> Unit,
    onSetupRecovery: () -> Unit,
    onForgotPassword: () -> Unit,
    onBackupAppList: () -> Unit,
    onRestoreAppList: () -> Unit
) {
    val timeoutOptions = listOf(
        "15" to stringResource(R.string.timeout_15s),
        "30" to stringResource(R.string.timeout_30s),
        "60" to stringResource(R.string.timeout_60s),
        "120" to stringResource(R.string.timeout_120s),
        "300" to stringResource(R.string.timeout_300s)
    )

    SandboxSubmenuScaffold(
        title = stringResource(R.string.settings_title),
        onBackClick = onBackClick,
    ) {
        LazyColumn(
            state = listState,
            overscrollEffect = listOverscrollEffect,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                SecuritySection(
                    currentSecurityType = currentSecurityType,
                    onChangeSecurityType = onChangeSecurityType
                )
            }

            if (isBiometricAvailable) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    BiometricsSection(
                        isBiometricEnabled = isBiometricEnabled,
                        isPreferBiometric = isPreferBiometric,
                        onChangeBiometricEnabled = onChangeBiometricEnabled,
                        onChangePreferBiometric = onChangePreferBiometric
                    )
                }
            }

            if (currentSecurityType != SecurityType.NONE) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RecoverySection(
                        hasSecurityQuestion = hasSecurityQuestion,
                        onSetupRecovery = onSetupRecovery,
                        onForgotPassword = onForgotPassword
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                LockedAppBehaviorSection(
                    currentBehavior = currentLockedAppBehavior,
                    currentTimeout = currentLockedAppTimeout,
                    timeoutOptions = timeoutOptions,
                    onChangeBehavior = onChangeLockedAppBehavior,
                    onChangeTimeout = onChangeLockedAppTimeout
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                PrivateSectionBehaviorSection(
                    currentBehavior = currentPrivateBehavior,
                    currentTimeout = currentPrivateTimeout,
                    timeoutOptions = timeoutOptions,
                    onChangeBehavior = onChangePrivateBehavior,
                    onChangeTimeout = onChangePrivateTimeout
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                BackupRestoreSection(
                    onBackup = onBackupAppList,
                    onRestore = onRestoreAppList
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AboutSection()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BackupRestoreSection(
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    LocalPreferenceGroup(title = stringResource(R.string.settings_backup_title)) {
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_backup_applist),
                summary = stringResource(R.string.settings_backup_applist_summary),
                icon = Icons.Outlined.Backup,
                position = pos,
                onClick = onBackup,
            )
        }
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_restore_applist),
                summary = stringResource(R.string.settings_restore_applist_summary),
                icon = Icons.Outlined.SettingsBackupRestore,
                position = pos,
                onClick = onRestore,
            )
        }
    }
}

@Composable
private fun SecuritySection(
    currentSecurityType: SecurityType,
    onChangeSecurityType: (SecurityType) -> Unit
) {
    val lockTypeSummary = when (currentSecurityType) {
        SecurityType.PIN -> stringResource(R.string.settings_lock_type_pin)
        SecurityType.PASSWORD -> stringResource(R.string.settings_lock_type_password)
        SecurityType.PATTERN -> stringResource(R.string.settings_lock_type_pattern)
        SecurityType.NONE -> stringResource(R.string.settings_lock_type_none)
    }

    val lockTypeIcon = when (currentSecurityType) {
        SecurityType.PIN -> Icons.Outlined.Pin
        SecurityType.PASSWORD -> Icons.Outlined.Password
        SecurityType.PATTERN -> Icons.Outlined.Pattern
        SecurityType.NONE -> Icons.Outlined.Lock
    }

    LocalPreferenceGroup(title = stringResource(R.string.settings_security_title)) {
        row { pos ->
            LocalBasePreference(
                title = stringResource(R.string.settings_current_lock_type),
                summary = lockTypeSummary,
                icon = lockTypeIcon,
                position = pos,
            )
        }
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_use_pin),
                summary = stringResource(R.string.settings_use_pin_summary),
                icon = Icons.Outlined.Pin,
                position = pos,
                onClick = { onChangeSecurityType(SecurityType.PIN) },
            )
        }
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_use_password),
                summary = stringResource(R.string.settings_use_password_summary),
                icon = Icons.Outlined.Password,
                position = pos,
                onClick = { onChangeSecurityType(SecurityType.PASSWORD) },
            )
        }
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_use_pattern),
                summary = stringResource(R.string.settings_use_pattern_summary),
                icon = Icons.Outlined.Pattern,
                position = pos,
                onClick = { onChangeSecurityType(SecurityType.PATTERN) },
            )
        }
    }
}

@Composable
private fun BiometricsSection(
    isBiometricEnabled: Boolean,
    isPreferBiometric: Boolean,
    onChangeBiometricEnabled: (Boolean) -> Unit,
    onChangePreferBiometric: (Boolean) -> Unit
) {
    LocalPreferenceGroup(title = stringResource(R.string.settings_biometrics_title)) {
        row { pos ->
            LocalSwitchPreference(
                title = stringResource(R.string.settings_biometric_unlock),
                summary = stringResource(R.string.settings_biometric_unlock_summary),
                checked = isBiometricEnabled,
                onCheckedChange = onChangeBiometricEnabled,
                icon = Icons.Outlined.Fingerprint,
                position = pos,
            )
        }
        if (isBiometricEnabled) {
            row { pos ->
                LocalSwitchPreference(
                    title = stringResource(R.string.settings_auto_biometric),
                    summary = stringResource(R.string.settings_auto_biometric_summary),
                    checked = isPreferBiometric,
                    onCheckedChange = onChangePreferBiometric,
                    icon = Icons.Outlined.AutoMode,
                    position = pos,
                )
            }
        }
    }
}

@Composable
private fun RecoverySection(
    hasSecurityQuestion: Boolean,
    onSetupRecovery: () -> Unit,
    onForgotPassword: () -> Unit
) {
    LocalPreferenceGroup(title = stringResource(R.string.settings_recovery_title)) {
        row { pos ->
            if (hasSecurityQuestion) {
                LocalClickablePreference(
                    title = stringResource(R.string.settings_forgot_password),
                    summary = stringResource(R.string.settings_forgot_password_summary),
                    icon = Icons.Outlined.LockReset,
                    position = pos,
                    onClick = onForgotPassword,
                )
            } else {
                LocalClickablePreference(
                    title = stringResource(R.string.settings_setup_question),
                    summary = stringResource(R.string.settings_setup_question_summary),
                    icon = Icons.Outlined.Help,
                    position = pos,
                    onClick = onSetupRecovery,
                )
            }
        }
    }
}

@Composable
private fun LockedAppBehaviorSection(
    currentBehavior: LockedAppBehavior,
    currentTimeout: Int,
    timeoutOptions: List<Pair<String, String>>,
    onChangeBehavior: (LockedAppBehavior) -> Unit,
    onChangeTimeout: (Int) -> Unit
) {
    val behaviorOptions = listOf(
        LockedAppBehavior.ON_LEAVE.name to stringResource(R.string.settings_behavior_on_leave),
        LockedAppBehavior.TIMEOUT.name to stringResource(R.string.settings_behavior_timeout),
        LockedAppBehavior.ON_SCREEN_OFF.name to stringResource(R.string.settings_behavior_screen_off),
        LockedAppBehavior.ON_KILL.name to stringResource(R.string.settings_behavior_on_kill)
    )

    LocalPreferenceGroup(title = stringResource(R.string.settings_locked_behavior_title)) {
        row { pos ->
            LocalListPreference(
                title = stringResource(R.string.settings_locked_behavior_summary),
                options = behaviorOptions,
                value = currentBehavior.name,
                onValueChange = { value ->
                    onChangeBehavior(LockedAppBehavior.valueOf(value))
                },
                position = pos,
            )
        }
        if (currentBehavior == LockedAppBehavior.TIMEOUT) {
            row { pos ->
                LocalListPreference(
                    title = stringResource(R.string.settings_timeout_title),
                    summary = stringResource(R.string.settings_behavior_timeout_summary, currentTimeout),
                    options = timeoutOptions,
                    value = currentTimeout.toString(),
                    onValueChange = { value -> onChangeTimeout(value.toInt()) },
                    position = pos,
                )
            }
        }
    }
}

@Composable
private fun PrivateSectionBehaviorSection(
    currentBehavior: PrivateSectionBehavior,
    currentTimeout: Int,
    timeoutOptions: List<Pair<String, String>>,
    onChangeBehavior: (PrivateSectionBehavior) -> Unit,
    onChangeTimeout: (Int) -> Unit
) {
    val behaviorOptions = listOf(
        PrivateSectionBehavior.ON_LEAVE.name to stringResource(R.string.settings_private_on_leave),
        PrivateSectionBehavior.TIMEOUT.name to stringResource(R.string.settings_private_timeout)
    )

    LocalPreferenceGroup(title = stringResource(R.string.settings_private_behavior_title)) {
        row { pos ->
            LocalListPreference(
                title = stringResource(R.string.settings_private_behavior_summary),
                options = behaviorOptions,
                value = currentBehavior.name,
                onValueChange = { value ->
                    onChangeBehavior(PrivateSectionBehavior.valueOf(value))
                },
                position = pos,
            )
        }
        if (currentBehavior == PrivateSectionBehavior.TIMEOUT) {
            row { pos ->
                LocalListPreference(
                    title = stringResource(R.string.settings_timeout_title),
                    summary = stringResource(R.string.settings_private_timeout_summary, currentTimeout),
                    options = timeoutOptions,
                    value = currentTimeout.toString(),
                    onValueChange = { value -> onChangeTimeout(value.toInt()) },
                    position = pos,
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    LocalPreferenceGroup(title = stringResource(R.string.settings_about_title)) {
        row { pos ->
            LocalClickablePreference(
                title = stringResource(R.string.settings_version),
                summary = stringResource(R.string.settings_version_value),
                icon = Icons.Outlined.Info,
                position = pos,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Evolution-X/packages_apps_Sandbox"),
                    )
                    context.startActivity(intent)
                },
            )
        }
    }
}
