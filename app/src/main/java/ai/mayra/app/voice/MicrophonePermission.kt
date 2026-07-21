package ai.mayra.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object MicrophonePermission {
    const val permission = Manifest.permission.RECORD_AUDIO

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
