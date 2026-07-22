package ai.mayra.app.platform.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import ai.mayra.app.core.device.DeviceClipboardGateway

class AndroidClipboardGateway(
    context: Context
) : DeviceClipboardGateway {
    private val clipboard = context.applicationContext
        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun copy(label: String, text: String): Boolean {
        if (text.isEmpty()) return false
        return runCatching {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(label.ifBlank { DEFAULT_LABEL }, text)
            )
            true
        }.getOrDefault(false)
    }

    private companion object {
        const val DEFAULT_LABEL = "Mayra"
    }
}
