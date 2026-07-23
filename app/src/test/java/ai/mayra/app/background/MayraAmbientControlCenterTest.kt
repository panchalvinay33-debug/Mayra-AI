package ai.mayra.app.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MayraAmbientControlCenterTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_ambient_preferences", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("mayra_ambient_events", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun defaultsKeepIntelligenceLocalAndSensitiveBriefingsOff() {
        val preferences = AmbientPreferenceStore(context).read()

        assertTrue(preferences.notificationIntelligenceEnabled)
        assertTrue(preferences.retainLocalHistory)
        assertTrue(preferences.proactiveSuggestionsEnabled)
        assertFalse(preferences.sensitiveContentInBriefings)
    }

    @Test
    fun updatesPersistAcrossStoreInstances() {
        AmbientPreferenceStore(context).update {
            it.copy(
                notificationIntelligenceEnabled = false,
                eveningBriefingEnabled = false,
                sensitiveContentInBriefings = true
            )
        }

        val restored = AmbientPreferenceStore(context).read()
        assertFalse(restored.notificationIntelligenceEnabled)
        assertFalse(restored.eveningBriefingEnabled)
        assertTrue(restored.sensitiveContentInBriefings)
    }

    @Test
    fun clearingHistoryRemovesStoredAmbientEvents() {
        AmbientEventStore(context).append(
            AmbientEvent("com.example", "Delivery", "Package arrived", 100L)
        )
        assertTrue(AmbientEventStore(context).snapshot().isNotEmpty())

        MayraAmbientControlCenter.clearLocalAmbientHistory(context)

        assertTrue(AmbientEventStore(context).snapshot().isEmpty())
    }
}
