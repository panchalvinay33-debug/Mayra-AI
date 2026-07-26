package ai.mayra.app

import android.app.Application

/**
 * Lightweight application used by local Robolectric tests.
 *
 * Production [MayraApplication] intentionally starts background scheduling and WorkManager-backed
 * runtimes. Unit tests should construct those systems explicitly instead of triggering device
 * startup side effects while Robolectric is creating the application process.
 */
class TestMayraApplication : Application()
