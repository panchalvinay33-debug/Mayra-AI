package ai.mayra.app.android.actions.audio

import android.content.Context
import android.media.AudioManager
import ai.mayra.app.android.actions.ActionResult
import ai.mayra.app.android.actions.AndroidAction


enum class VolumeStream {
    MEDIA,
    RING,
    ALARM,
    NOTIFICATION,
}

data class VolumeRequest(
    val stream: VolumeStream = VolumeStream.MEDIA,
    val percent: Int,
)

class VolumeAction(
    context: Context,
    private val audioManager: AudioManager =
        context.getSystemService(AudioManager::class.java),
) : AndroidAction<VolumeRequest> {

    override val id: String = "device.audio.volume"

    override suspend fun execute(request: VolumeRequest): ActionResult {
        val streamType = request.stream.toAndroidStream()
        val boundedPercent = request.percent.coerceIn(0, 100)
        val maximum = audioManager.getStreamMaxVolume(streamType)
        val target = ((boundedPercent / 100f) * maximum).toInt().coerceIn(0, maximum)

        return runCatching {
            audioManager.setStreamVolume(streamType, target, 0)
            ActionResult.Success(
                message = "${request.stream.name.lowercase()} volume set to $boundedPercent%",
                data = mapOf(
                    "stream" to request.stream.name,
                    "percent" to boundedPercent.toString(),
                    "level" to target.toString(),
                ),
            )
        }.getOrElse { error ->
            ActionResult.Failure(
                message = error.message ?: "Unable to change volume",
                recoverable = true,
                cause = error,
            )
        }
    }

    private fun VolumeStream.toAndroidStream(): Int = when (this) {
        VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
        VolumeStream.RING -> AudioManager.STREAM_RING
        VolumeStream.ALARM -> AudioManager.STREAM_ALARM
        VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
    }
}
