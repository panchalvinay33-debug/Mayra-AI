package ai.mayra.app.assistant

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionService

/**
 * System-supported foundation for Mayra's always-present assistant mode.
 *
 * Android keeps the user-selected VoiceInteractionService alive for assistant duties. Heavy work,
 * visible UI and conversational processing belong in the associated interaction session rather
 * than this lightweight service.
 */
class MayraVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        MayraAssistantRoleState.markServiceReady(true)
    }

    override fun onShutdown() {
        MayraAssistantRoleState.markServiceReady(false)
        super.onShutdown()
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        showSession(Bundle().apply { putBoolean(KEY_FROM_KEYGUARD, true) }, 0)
    }

    companion object {
        const val KEY_FROM_KEYGUARD = "mayra_from_keyguard"
    }
}

object MayraAssistantRoleState {
    @Volatile private var serviceReady: Boolean = false

    fun markServiceReady(ready: Boolean) {
        serviceReady = ready
    }

    fun snapshot(context: Context): MayraAssistantRoleSnapshot {
        val component = ComponentName(context, MayraVoiceInteractionService::class.java)
        val active = VoiceInteractionService.isActiveService(context, component)
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        } else active
        return MayraAssistantRoleSnapshot(roleHeld = roleHeld, activeService = active, serviceReady = serviceReady)
    }

    fun requestRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val manager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!manager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) return null
        return manager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
    }
}

data class MayraAssistantRoleSnapshot(
    val roleHeld: Boolean,
    val activeService: Boolean,
    val serviceReady: Boolean
) {
    val jarvisBackgroundReady: Boolean get() = roleHeld && activeService
}
