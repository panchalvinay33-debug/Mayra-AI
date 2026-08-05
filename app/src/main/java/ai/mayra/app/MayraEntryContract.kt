package ai.mayra.app

import android.content.Context
import android.content.Intent

/**
 * Shared navigation contract between Mayra Home, the Android assistant surface and full Mayra.
 *
 * The launcher and voice-session surfaces stay lightweight. They hand off deeper conversation,
 * memory and action work to MainActivity instead of initializing heavy AI inside Home.
 */
object MayraEntryContract {
    const val EXTRA_SOURCE = "ai.mayra.app.extra.ENTRY_SOURCE"

    enum class Source(val wireValue: String) {
        LAUNCHER("launcher"),
        VOICE_SESSION("voice_session"),
        OTHER("other")
    }

    fun fullMayraIntent(context: Context, source: Source): Intent =
        Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_SOURCE, source.wireValue)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
}
