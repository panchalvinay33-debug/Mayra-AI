package ai.mayra.app.settings

import ai.mayra.app.welcomeMessage
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MayraSettingsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = MayraSettingsStore(context)

    @After
    fun cleanup() {
        store.reset()
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals("Tell Mayra what to call you.", MayraSettings(userName = "   ").validationMessage())
    }

    @Test
    fun `valid name is trimmed and accepted`() {
        val settings = MayraSettings(userName = "  Vinay  ")

        assertEquals("Vinay", settings.normalizedName)
        assertNull(settings.validationMessage())
    }

    @Test
    fun `name is bounded to safe local profile length`() {
        val settings = MayraSettings(userName = "A".repeat(100))

        assertEquals(MayraSettings.MAX_NAME_LENGTH, settings.normalizedName.length)
    }

    @Test
    fun `summary exposes profile language and memory state`() {
        val settings = MayraSettings(
            userName = "Vinay",
            language = MayraLanguage.HINDI,
            memoryEnabled = false
        )

        assertEquals("Vinay · Hindi · Memory off", settings.summary())
    }

    @Test
    fun `settings persist and onboarding completes`() {
        store.completeOnboarding(
            MayraSettings(
                userName = "Vinay",
                language = MayraLanguage.ENGLISH,
                speakResponses = false,
                continuousVoiceByDefault = true,
                memoryEnabled = false,
                personalizationEnabled = false,
                diagnosticsSharingEnabled = true
            )
        )

        val saved = store.read()
        assertEquals("Vinay", saved.userName)
        assertEquals(MayraLanguage.ENGLISH, saved.language)
        assertFalse(saved.speakResponses)
        assertTrue(saved.continuousVoiceByDefault)
        assertFalse(saved.memoryEnabled)
        assertFalse(saved.personalizationEnabled)
        assertTrue(saved.diagnosticsSharingEnabled)
        assertTrue(saved.onboardingCompleted)
    }

    @Test
    fun `invalid stored language falls back to Hinglish`() {
        context.getSharedPreferences("mayra_settings", Context.MODE_PRIVATE)
            .edit().putString("language", "UNKNOWN").apply()

        assertEquals(MayraLanguage.HINGLISH, store.read().language)
    }

    @Test
    fun `welcome message uses configured name`() {
        assertEquals(
            "Namaste, Vinay. I’m Mayra. What can I help you with today?",
            welcomeMessage("Vinay")
        )
    }

    @Test
    fun `welcome message remains safe before onboarding`() {
        assertEquals(
            "Namaste. I’m Mayra. What can I help you with today?",
            welcomeMessage("")
        )
    }
}
