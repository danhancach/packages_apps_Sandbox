/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.sandbox.ui.motion

/**
 * Route stack cho [MiuixPageTransition] trong Sandbox.
 * ROOT = main pager tabs; cac route con la overlay submenu/detail.
 */
enum class SandboxSubRoute {
    ROOT,
    SETTINGS,
    SETTINGS_RECOVERY,
    CHANGE_SECURITY,
    VERIFY_CURRENT_CREDENTIAL,
    APP_DETAIL,
    UNLOCK_PRIVATE,
}
