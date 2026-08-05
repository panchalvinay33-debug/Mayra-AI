package ai.mayra.app.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class MayraLauncherSearchTest {
    private val apps = listOf(
        LaunchableApp("WhatsApp", "com.whatsapp", "com.whatsapp.Main"),
        LaunchableApp("YouTube", "com.google.android.youtube", "com.google.android.youtube.Home"),
        LaunchableApp("Calculator", "com.motorola.calculator", "com.motorola.calculator.Main")
    )

    @Test
    fun blankQueryReturnsAllAppsInExistingOrder() {
        assertEquals(apps, filterLaunchableApps(apps, "   "))
    }

    @Test
    fun labelSearchIsCaseInsensitive() {
        assertEquals(listOf(apps[0]), filterLaunchableApps(apps, "whatS"))
    }

    @Test
    fun packageSearchWorksForOwnerWhenLabelDoesNotMatch() {
        assertEquals(listOf(apps[2]), filterLaunchableApps(apps, "motorola"))
    }

    @Test
    fun unknownQueryReturnsEmptyList() {
        assertEquals(emptyList<LaunchableApp>(), filterLaunchableApps(apps, "does-not-exist"))
    }
}
