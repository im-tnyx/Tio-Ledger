package com.tioledger.ui.validation

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo

/** Small platform-only safety probe; not a screenshot framework or screen-reader test. */
class AccountsValidationInstrumentation : Instrumentation() {
    private var arguments: Bundle? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments
        start()
    }

    override fun onStart() {
        val context = targetContext
        val applicationClass = context.applicationContext.javaClass.name
        val databases = context.databaseList()
        val isolated = context.packageName == "com.tioledger.shared.ui.test"
        val safe = isolated && applicationClass == Application::class.java.name && databases.isEmpty()
        val result =
            Bundle().apply {
                putString("applicationClass", applicationClass)
                putStringArray("databases", databases)
                putBoolean("isolatedPackage", isolated)
                putBoolean("displayOnlyStartup", safe)
            }
        if (safe && arguments?.getString("mode") == "inspect-focus") {
            inspectFocus(result)
        }
        finish(if (safe) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }

    private fun inspectFocus(result: Bundle) {
        startActivitySync(
            Intent(targetContext, AccountsValidationActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("scenario", "mixed")
                .putExtra("widthDp", arguments?.getString("widthDp")?.toIntOrNull() ?: 360)
                .putExtra("darkTheme", arguments?.getString("darkTheme") == "true"),
        )
        waitForIdleSync()
        // Keep the real TalkBack service running; a node dump is not speech/traversal evidence.
        val automation = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        SystemClock.sleep(1_000)
        val root = automation.rootInActiveWindow
        if (root?.packageName?.toString() != targetContext.packageName) {
            result.putString("focusProbe", "Unexpected active window; no action taken")
            return
        }
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)
        if (arguments?.getString("scroll") == "true") {
            val list = nodes.firstOrNull { it.isScrollable && it.className?.toString() != "android.widget.EditText" }
            result.putBoolean("scrollAction", list?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true)
            SystemClock.sleep(1_000)
            nodes.clear()
            automation.rootInActiveWindow?.let { collectNodes(it, nodes) }
        }
        result.putString("harnessNodes", nodes.joinToString(" | ") { describe(it) })
        val title = arguments?.getString("headerTitle") ?: "Cash"
        val header =
            nodes.firstOrNull { node ->
                val childTexts = (0 until node.childCount).mapNotNull { node.getChild(it)?.text?.toString() }
                title in childTexts && childTexts.any { it.startsWith("USD ") }
            }
        result.putBoolean("headerFocusAction", header?.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) == true)
        SystemClock.sleep(1_000)
        result.putString(
            "accessibilityFocus",
            automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let(::describe),
        )
        val captions = mutableListOf<String>()
        automation.windows.forEach { window ->
            val overlayNodes = mutableListOf<AccessibilityNodeInfo>()
            window.root?.let { collectNodes(it, overlayNodes) }
            captions += overlayNodes.filter { it.packageName?.toString() == "com.google.android.marvin.talkback" }.map(::describe)
        }
        result.putString("talkBackOverlayNodes", captions.joinToString(" | "))
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        nodes: MutableList<AccessibilityNodeInfo>,
    ) {
        if (nodes.size >= 200) return
        nodes += node
        repeat(node.childCount) { index -> node.getChild(index)?.let { collectNodes(it, nodes) } }
    }

    private fun describe(node: AccessibilityNodeInfo): String =
        "text=${node.text}; description=${node.contentDescription}; " +
            "enabled=${node.isEnabled}; focused=${node.isAccessibilityFocused}"
}
