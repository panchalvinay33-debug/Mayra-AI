package ai.mayra.app.assistive

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicReference

/**
 * User-enabled, read-only assistive context foundation.
 *
 * This service never performs clicks, types text, submits forms or reads password fields.
 * It keeps only a bounded in-memory snapshot so Mayra can later offer visible, user-invoked help.
 */
class MayraAssistiveService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        if (event.eventType !in SUPPORTED_EVENTS) return

        val packageName = event.packageName.toString()
        val root = rootInActiveWindow
        val visibleText = root?.let(::collectVisibleText).orEmpty()
        MayraAssistiveContext.update(
            MayraScreenSnapshot(
                packageName = packageName,
                windowTitle = event.contentDescription?.toString()?.sanitizeText(),
                visibleText = visibleText,
                capturedAt = System.currentTimeMillis()
            )
        )
        root?.recycle()
    }

    override fun onInterrupt() {
        MayraAssistiveContext.clear()
    }

    override fun onDestroy() {
        MayraAssistiveContext.clear()
        super.onDestroy()
    }

    private fun collectVisibleText(root: AccessibilityNodeInfo): List<String> {
        val results = LinkedHashSet<String>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var visited = 0

        while (pending.isNotEmpty() && visited < MAX_NODES && results.size < MAX_TEXT_ITEMS) {
            val node = pending.removeFirst()
            visited++
            try {
                if (node.isVisibleToUser && !node.isPassword && !node.isSensitiveInput()) {
                    node.text?.toString()?.sanitizeText()?.takeIf(String::isNotBlank)?.let(results::add)
                    node.contentDescription?.toString()?.sanitizeText()?.takeIf(String::isNotBlank)?.let(results::add)
                }
                repeat(node.childCount) { index ->
                    node.getChild(index)?.let(pending::addLast)
                }
            } finally {
                if (node !== root) node.recycle()
            }
        }

        while (pending.isNotEmpty()) pending.removeFirst().recycle()
        return results.take(MAX_TEXT_ITEMS)
    }

    private fun AccessibilityNodeInfo.isSensitiveInput(): Boolean {
        val classNameText = className?.toString().orEmpty()
        val hint = hintText?.toString().orEmpty().lowercase()
        val labels = listOf(text, contentDescription)
            .mapNotNull { it?.toString()?.lowercase() }
            .joinToString(" ")
        return classNameText.contains("password", ignoreCase = true) ||
            SENSITIVE_SIGNALS.any { it in hint || it in labels }
    }

    private fun String.sanitizeText(): String = replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_TEXT_LENGTH)

    private companion object {
        val SUPPORTED_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED
        )
        val SENSITIVE_SIGNALS = setOf(
            "password", "passcode", "pin", "otp", "one time password", "cvv", "security code"
        )
        const val MAX_NODES = 120
        const val MAX_TEXT_ITEMS = 40
        const val MAX_TEXT_LENGTH = 240
    }
}

data class MayraScreenSnapshot(
    val packageName: String,
    val windowTitle: String?,
    val visibleText: List<String>,
    val capturedAt: Long
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean = now - capturedAt <= 30_000L
}

object MayraAssistiveContext {
    private val latest = AtomicReference<MayraScreenSnapshot?>(null)

    fun update(snapshot: MayraScreenSnapshot) {
        latest.set(snapshot)
    }

    fun latestFresh(now: Long = System.currentTimeMillis()): MayraScreenSnapshot? =
        latest.get()?.takeIf { it.isFresh(now) }

    fun clear() {
        latest.set(null)
    }
}
