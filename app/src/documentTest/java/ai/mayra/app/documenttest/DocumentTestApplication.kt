package ai.mayra.app.documenttest

import android.app.Application

/**
 * Side-effect-free application used only by the isolated document-test APK.
 *
 * It deliberately does not initialize Mayra's voice, contacts, device-action,
 * notification-listener, WorkManager, alarm, boot or ambient intelligence runtime.
 */
class DocumentTestApplication : Application()
