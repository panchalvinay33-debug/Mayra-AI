package ai.mayra.app

import android.app.Application
import androidx.work.Configuration

/** Lightweight Robolectric application that avoids booting the full Mayra runtime in unit tests. */
class TestMayraApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
