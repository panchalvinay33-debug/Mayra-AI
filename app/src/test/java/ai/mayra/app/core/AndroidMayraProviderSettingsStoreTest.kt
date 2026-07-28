package ai.mayra.app.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidMayraProviderSettingsStoreTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_provider_settings_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun defaultsAreDisabledAndValidHttps() {
        val settings = AndroidMayraProviderSettingsStore(context).read()
        assertFalse(settings.enabled)
        assertTrue(settings.endpoint.startsWith("https://"))
        assertTrue(settings.validatedConfig().isSuccess)
    }

    @Test fun persistsOnlyNonSecretSettings() {
        val store = AndroidMayraProviderSettingsStore(context)
        val settings = MayraProviderSettings(true, "https://provider.example/v1", "mayra-hi", 5_000, 15_000, 64_000)
        assertTrue(store.write(settings).isSuccess)
        assertEquals(settings, AndroidMayraProviderSettingsStore(context).read())
        val raw = context.getSharedPreferences("mayra_provider_settings_v1", Context.MODE_PRIVATE).all
        assertFalse(raw.keys.any { it.contains("token", true) || it.contains("secret", true) || it.contains("authorization", true) })
    }

    @Test fun rejectsPlainHttpWithoutOverwritingPreviousSettings() {
        val store = AndroidMayraProviderSettingsStore(context)
        val good = MayraProviderSettings(true, "https://provider.example/v1", "mayra-hi")
        assertTrue(store.write(good).isSuccess)
        assertTrue(store.write(good.copy(endpoint = "http://unsafe.example")).isFailure)
        assertEquals(good, store.read())
    }

    @Test fun emergencyDisablePreservesEndpointAndModel() {
        val store = AndroidMayraProviderSettingsStore(context)
        val settings = MayraProviderSettings(true, "https://provider.example/v1", "mayra-hi")
        store.write(settings)
        assertTrue(store.disable())
        val restored = store.read()
        assertFalse(restored.enabled)
        assertEquals(settings.endpoint, restored.endpoint)
        assertEquals(settings.model, restored.model)
    }
}
