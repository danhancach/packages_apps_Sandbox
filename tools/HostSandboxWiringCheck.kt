/**
 * Host check — production wiring markers for Sandbox UI redesign REWORK attempt5.
 * Sequence/ownership: khong chi token-presence — doi chieu call-chain markers
 * (recovery SubmenuScaffold/Local*, Vault FlowRow/toolbar scroll, bottomSlot do,
 * bottom nav finite rowHeight + geometry SSOT).
 */
package com.android.axion.sandbox.tools

import java.io.File
import kotlin.system.exitProcess

object HostSandboxWiringCheck {
    private val fails = ArrayList<String>()

    private fun check(cond: Boolean, msg: String) {
        if (!cond) fails.add(msg)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val root = File(
            args.getOrNull(0)
                ?: System.getenv("SANDBOX_ROOT")
                ?: "packages/apps/Sandbox",
        )
        fun read(rel: String): String {
            val f = File(root, rel)
            check(f.isFile, "missing $rel")
            return if (f.isFile) f.readText() else ""
        }

        val shell = read("src/com/android/axion/sandbox/ui/SandboxMainShell.kt")
        check(shell.contains("RememberStretchArbitrationRegistration"), "shell registers stretch owner")
        check(shell.contains("rememberLazyGridState()") || shell.contains("rememberLazyGridState"), "shell hoist grid state")
        check(shell.contains("rememberLazyListState"), "shell hoist list state")
        check(!shell.contains("canContentScroll = { true }"), "no hardcoded canContentScroll true")
        check(shell.contains("miuixAlwaysVerticalStretch") || shell.contains("useAlwaysStretch"), "always-stretch path wired")
        check(shell.contains("LocalSandboxPagerSettled"), "pager settled local")
        check(!shell.contains("SandboxBottomContentFade"), "fading edge removed per user request")
        check(shell.contains("rootInteractive"), "root interactive flag")
        // Bottom chrome: pager weight + onSizeChanged measured height (khong hardcode viewport).
        check(shell.contains("LocalSandboxBottomChromeHeight provides bottomChromeHeight"), "shell provides measured chrome height")
        check(shell.contains("onSizeChanged"), "shell measures bottom chrome onSizeChanged")
        check(shell.contains("weight(1f)"), "pager viewport weight(1f) above nav")
        check(shell.contains("SandboxBottomBar("), "shell wires SandboxBottomBar")

        val app = read("src/com/android/axion/sandbox/ui/SandboxApp.kt")
        check(app.contains("onOpenDetails"), "sheet details action")
        check(app.contains("MiuixPageTransition"), "detail transition")
        check(app.contains("SandboxSubRoute.APP_DETAIL"), "detail route")
        check(app.contains("detailSnapshot"), "detail outgoing snapshot")
        check(app.contains("onSettledAtRoot"), "clear snapshot when settled")
        check(!app.contains("fun M3EFloatingToolbar"), "M3EFloatingToolbar removed")
        check(!app.contains("DampingRatioMediumBouncy"), "no bouncy toolbar items")
        check(!app.contains("fun NonLazyAppGrid"), "NonLazyAppGrid removed")
        check(app.contains("miuixAlwaysVerticalStretch"), "Apps/Notif always-stretch call sites")
        check(app.contains("gridState"), "AppsTab accepts gridState")
        check(app.contains("listState"), "NotificationsTab accepts listState")
        check(app.contains("interactionsEnabled"), "detail interaction gate")

        // Detail/sheet: khong con axion PreferenceGroup/SwitchPreference (strip Local*).
        val appShared = app
            .replace("LocalSwitchPreference", "X")
            .replace("LocalPreferenceGroup", "Y")
            .replace("LocalClickablePreference", "Z")
            .replace("LocalBasePreference", "W")
            .replace("LocalExpressivePreferenceRow", "R")
            .replace("LocalListPreference", "L")
            .replace("LocalPrefPosition", "P")
        check(!appShared.contains("SwitchPreference("), "SandboxApp no shared SwitchPreference")
        check(!appShared.contains("PreferenceGroup("), "SandboxApp no shared PreferenceGroup")
        check(!appShared.contains("BasePreference("), "SandboxApp no shared BasePreference")
        check(!appShared.contains("ClickablePreference("), "SandboxApp no shared ClickablePreference")
        check(app.contains("LocalPreferenceGroup"), "SandboxApp wires LocalPreferenceGroup")
        check(app.contains("LocalExpressivePreferenceRow"), "sheet header LocalExpressive row")

        val vault = read("src/com/android/axion/sandbox/ui/VaultTab.kt")
        check(vault.contains("vault_filter_all"), "vault filters")
        check(vault.contains("LocalSandboxRootInteractive"), "vault root interactive gate")
        check(vault.contains("LocalSandboxPagerSettled"), "vault pager settled gate")
        check(vault.contains("AnimatedVisibility"), "vault toolbar AnimatedVisibility")
        check(vault.contains("bottomSlotHeight"), "vault measured bottom slot")
        check(!vault.contains("88.dp") || !vault.contains("gridBottomPad = if"), "no hardcoded 88/72 grid pad")
        check(vault.contains("miuixAlwaysVerticalStretch"), "vault always-stretch")
        // Adaptive filter/toolbar: wrap + scroll, slot do chung entry/exit.
        check(vault.contains("FlowRow("), "vault filters FlowRow wrap")
        check(vault.contains("horizontalScroll(rememberScrollState())"), "vault toolbar actions scroll")
        check(vault.contains("animateContentSize()"), "vault bottom slot animateContentSize")
        check(vault.contains("onSizeChanged"), "vault bottom slot onSizeChanged")
        val filterBlockStart = vault.indexOf("contentType = \"filters\"")
        val filterBlock = if (filterBlockStart >= 0) {
            vault.substring(filterBlockStart, (filterBlockStart + 1200).coerceAtMost(vault.length))
        } else {
            ""
        }
        check(filterBlock.contains("FlowRow("), "filters span uses FlowRow")
        // Tranh false-positive: FlowRow( chua substring Row( — chi bat Row( khong phai FlowRow(.
        val filterHasBareRow = Regex("""(?<!Flow)Row\(""").containsMatchIn(filterBlock)
        check(!filterHasBareRow, "filters not fixed single-line Row")
        check(filterBlock.contains("vault_filter_files"), "filters block lists all chips")
        val toolbarIdx = vault.indexOf("vault_selected_count")
        val toolbarBlock = if (toolbarIdx >= 0) {
            vault.substring((toolbarIdx - 1200).coerceAtLeast(0), (toolbarIdx + 1600).coerceAtMost(vault.length))
        } else {
            ""
        }
        check(toolbarBlock.contains("Column("), "toolbar two-level Column")
        check(toolbarBlock.contains("horizontalScroll"), "toolbar horizontalScroll near actions")
        check(toolbarBlock.contains("R.string.action_restore"), "toolbar keeps restore")
        check(toolbarBlock.contains("R.string.action_delete"), "toolbar keeps delete")
        check(toolbarBlock.contains("showRestoreConfirm") && toolbarBlock.contains("showDeleteConfirm"), "toolbar wires confirm flags")
        check(vault.contains("if (measured > 0.dp) measured else 72.dp"), "bottomSlotHeight uses measured height")

        val submenu = read("src/com/android/axion/sandbox/ui/SandboxSubmenuScaffold.kt")
        check(submenu.contains("RememberStretchArbitrationRegistration"), "submenu registers owner")
        check(submenu.contains("SandboxSubmenuScrollScope"), "submenu shares scroll scope")
        check(submenu.contains("listOverscrollEffect"), "submenu shares overscroll")
        check(submenu.contains("canScrollForward"), "submenu derives canContentScroll")
        check(submenu.contains("rememberMiuixVerticalStretchOverscrollFactory"), "submenu Miuix factory")

        val pref = read("src/com/android/axion/sandbox/ui/LocalExpressivePreference.kt")
        check(pref.contains("scope.content()"), "LocalPreferenceGroup invokes content()")
        check(!pref.contains(".apply(content)"), "no non-composable apply(content)")
        check(pref.contains("ic_settings_expressive_chevron"), "Settings chevron vector")
        check(pref.contains("size(40.dp)"), "icon frame 40dp")
        check(pref.contains("padding(end = 16.dp)"), "text paddingEnd 16dp")
        check(pref.contains("fun LocalSwitchPreference"), "LocalSwitchPreference exists")
        check(pref.contains("fun LocalListPreference"), "LocalListPreference exists")

        val settings = read("src/com/android/axion/sandbox/ui/SettingsScreen.kt")
        check(settings.contains("LocalPreferenceGroup"), "settings wires LocalPreferenceGroup")
        check(settings.contains("LocalSwitchPreference") || settings.contains("LocalListPreference"), "settings local prefs")
        check(settings.contains("listOverscrollEffect"), "settings shares overscroll")
        val settingsShared = settings.replace("LocalSwitchPreference", "X").replace("LocalListPreference", "Y")
        check(!settingsShared.contains("SwitchPreference("), "settings no shared SwitchPreference")
        check(!settingsShared.contains("ListPreference("), "settings no shared ListPreference")

        // Recovery screens: SubmenuScaffold + Local* + shared overscroll (khong Axion/shared pref).
        val recoverySetup = read("src/com/android/axion/sandbox/ui/SecurityQuestionSetupScreen.kt")
        check(recoverySetup.contains("SandboxSubmenuScaffold("), "recovery setup SubmenuScaffold")
        check(recoverySetup.contains("listState"), "recovery setup listState")
        check(recoverySetup.contains("listOverscrollEffect"), "recovery setup overscroll share")
        check(recoverySetup.contains("LocalListPreference("), "recovery setup LocalListPreference")
        check(recoverySetup.contains("LocalPreferenceGroup"), "recovery setup LocalPreferenceGroup")
        check(recoverySetup.contains("onComplete"), "recovery setup onComplete callback")
        check(recoverySetup.contains("onBack"), "recovery setup onBack callback")
        check(recoverySetup.contains("onSkip"), "recovery setup onSkip optional")
        check(!recoverySetup.contains("AxionScaffold"), "recovery setup no AxionScaffold")
        val recoverySetupShared = recoverySetup
            .replace("LocalListPreference", "L")
            .replace("LocalPreferenceGroup", "G")
        check(!recoverySetupShared.contains("ListPreference("), "recovery setup no shared ListPreference")
        check(!recoverySetupShared.contains("PreferenceGroup("), "recovery setup no shared PreferenceGroup")

        val forgot = read("src/com/android/axion/sandbox/ui/ForgotPasswordScreen.kt")
        check(forgot.contains("SandboxSubmenuScaffold("), "forgot SubmenuScaffold")
        check(forgot.contains("listState"), "forgot listState")
        check(forgot.contains("listOverscrollEffect"), "forgot overscroll share")
        check(forgot.contains("LocalBasePreference("), "forgot LocalBasePreference")
        check(forgot.contains("LocalPreferenceGroup"), "forgot LocalPreferenceGroup")
        check(forgot.contains("onAnswerSubmit"), "forgot verify callback")
        check(forgot.contains("onSuccess"), "forgot reset success callback")
        check(forgot.contains("showConfirmDialog"), "forgot confirm reset dialog")
        check(!forgot.contains("AxionScaffold"), "forgot no AxionScaffold")
        val forgotShared = forgot
            .replace("LocalBasePreference", "B")
            .replace("LocalPreferenceGroup", "G")
        check(!forgotShared.contains("BasePreference("), "forgot no shared BasePreference")
        check(!forgotShared.contains("PreferenceGroup("), "forgot no shared PreferenceGroup")

        val main = read("src/com/android/axion/sandbox/MainActivity.kt")
        check(main.contains("rootInteractive"), "MainActivity passes rootInteractive")
        check(main.contains("recoveryOpen"), "nested recovery flag")
        // Nested host: SETTINGS child contains second MiuixPageTransition
        val nestedIdx = main.indexOf("childContent = {")
        val nestedBlock = if (nestedIdx >= 0) main.substring(nestedIdx) else ""
        check(
            nestedBlock.contains("MiuixPageTransition") &&
                nestedBlock.contains("SETTINGS_RECOVERY"),
            "SETTINGS→RECOVERY nested MiuixPageTransition",
        )
        check(main.contains("PredictiveBackHandler(enabled = recoveryOpen)"), "nested recovery Back")
        check(
            main.contains("PredictiveBackHandler(enabled = settingsOpen && !recoveryOpen)"),
            "outer settings Back gated",
        )
        // Wiring: initial setup + nested SETTINGS→RECOVERY + forgot keep distinct callbacks.
        check(main.contains("SandboxScreen.SETUP_SECURITY_QUESTION"), "initial recovery setup route")
        check(main.contains("SecurityQuestionSetupScreen("), "SecurityQuestionSetupScreen call sites")
        val setupRouteIdx = main.indexOf("SandboxScreen.SETUP_SECURITY_QUESTION ->")
        val setupRouteBlock = if (setupRouteIdx >= 0) main.substring(setupRouteIdx, (setupRouteIdx + 900).coerceAtMost(main.length)) else ""
        check(setupRouteBlock.contains("setSecurityQuestion"), "initial setup persists question")
        check(setupRouteBlock.contains("SandboxScreen.MAIN"), "initial setup returns MAIN")
        check(nestedBlock.contains("SecurityQuestionSetupScreen("), "nested SETTINGS→RECOVERY uses setup screen")
        check(nestedBlock.contains("setSecurityQuestion"), "nested recovery persists question")
        check(nestedBlock.contains("SandboxScreen.SETTINGS"), "nested recovery returns SETTINGS")
        val forgotIdx = main.indexOf("SandboxScreen.FORGOT_PASSWORD ->")
        val forgotBlock = if (forgotIdx >= 0) main.substring(forgotIdx, (forgotIdx + 1200).coerceAtMost(main.length)) else ""
        check(forgotBlock.contains("ForgotPasswordScreen("), "forgot password screen wired")
        check(forgotBlock.contains("verifySecurityAnswer"), "forgot verify answer")
        check(forgotBlock.contains("clearCredentials"), "forgot reset clears credentials")
        check(forgotBlock.contains("forgotPasswordReturnScreen"), "forgot back uses return screen")

        val auth = read("src/com/android/axion/sandbox/AuthenticateActivity.kt")
        check(!auth.contains("applyOptionalCrossWindowBlur"), "blur helper removed")
        check(auth.contains("AUTH_BLUR"), "solid auth fallback documented")

        val vi = read("res/values-vi/strings.xml")
        check(vi.split("name=\"tab_vault\"").size - 1 == 1, "vi tab_vault unique")

        val chevron = read("res/drawable/ic_settings_expressive_chevron.xml")
        check(chevron.contains("M321,880L250,809L579,480L250,151L321,80L721,480L321,880Z"), "chevron path Settings")
        check(chevron.contains("autoMirrored=\"true\""), "chevron autoMirrored")

        val transition = read("src/com/android/axion/sandbox/ui/motion/MiuixPageTransition.kt")
        check(transition.contains("onSettledAtRoot"), "transition settled root callback")

        // Bottom nav: finite content-driven rowHeight (khong heightIn(min) + maxHeight viewport).
        val bottomBar = read("src/com/android/axion/sandbox/ui/SandboxBottomBar.kt")
        val bottomGeom = read("src/com/android/axion/sandbox/ui/SandboxBottomNavGeometry.kt")
        check(bottomGeom.contains("object SandboxBottomNavGeometry"), "bottom nav geometry SSOT")
        check(bottomGeom.contains("fun rowHeightDp"), "geometry rowHeightDp")
        check(bottomGeom.contains("fun blockTopDp"), "geometry blockTopDp")
        check(bottomGeom.contains("fun pillOffsetXRtlMirrored"), "geometry RTL mirror helper")
        check(bottomBar.contains("SandboxBottomNavGeometry.rowHeightDp"), "bar uses geometry rowHeight")
        check(bottomBar.contains("SandboxBottomNavGeometry.blockTopDp"), "bar uses geometry blockTop")
        check(bottomBar.contains(".height(rowHeight)"), "bar applies finite .height(rowHeight)")
        check(!bottomBar.contains("heightIn(min = SandboxBottomTabRowHeight)"), "bar no min-only heightIn row")
        check(!bottomBar.contains("maxHeight - contentBlock"), "bar not centering from viewport maxHeight")
        check(bottomBar.contains("windowInsetsBottomHeight"), "bar keeps system nav inset")
        check(bottomBar.contains("maxLines = 1") &&
            bottomBar.contains("softWrap = false") &&
            bottomBar.contains("overflow = TextOverflow.Ellipsis"),
            "bar label bounded to one line at high fontScale/narrow width")
        check(!bottomBar.contains("HorizontalDivider") && !bottomBar.contains("Divider("), "bar no divider")

        if (fails.isNotEmpty()) {
            System.err.println("HostSandboxWiringCheck FAIL:")
            fails.forEach { System.err.println(" - $it") }
            exitProcess(1)
        }
        println("HostSandboxWiringCheck PASS")
    }
}
