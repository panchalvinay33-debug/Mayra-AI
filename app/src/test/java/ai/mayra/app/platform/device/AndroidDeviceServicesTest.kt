package ai.mayra.app.platform.device

import ai.mayra.app.core.actions.DevicePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidDeviceServicesTest {
    @Test
    fun permissionSnapshotSeparatesGrantedAndPermanentlyDeniedPermissions() {
        val provider = DevicePermissionSnapshotProvider(
            stateReader = DevicePermissionStateReader { permission ->
                permission in setOf(DevicePermission.QUERY_APPS, DevicePermission.READ_CONTACTS)
            },
            permanentlyDeniedReader = DevicePermissionStateReader { permission ->
                permission in setOf(DevicePermission.CALL_PHONE, DevicePermission.READ_CONTACTS)
            }
        )

        val snapshot = provider.snapshot()

        assertEquals(
            setOf(DevicePermission.QUERY_APPS, DevicePermission.READ_CONTACTS),
            snapshot.granted
        )
        assertEquals(setOf(DevicePermission.CALL_PHONE), snapshot.permanentlyDenied)
    }

    @Test
    fun phoneNormalizerConvertsCommonIndianFormatsToE164() {
        assertEquals("+919876543210", PhoneNumberNormalizer.normalize("98765 43210"))
        assertEquals("+919876543210", PhoneNumberNormalizer.normalize("09876543210"))
        assertEquals("+919876543210", PhoneNumberNormalizer.normalize("91-98765-43210"))
        assertEquals("+919876543210", PhoneNumberNormalizer.normalize("+91 98765 43210"))
    }

    @Test
    fun phoneNormalizerSupportsAnotherDefaultCountryCode() {
        assertEquals("+14155552671", PhoneNumberNormalizer.normalize("415 555 2671", "+1"))
    }

    @Test
    fun contactResolverPrefersOneExactNameMatch() {
        val resolver = ContactResolver(
            ContactPhoneDataSource {
                listOf(
                    ContactPhone(1, "Rahul Sharma", "9876543210"),
                    ContactPhone(2, "Rahul Verma", "9123456780")
                )
            }
        )

        val result = resolver.resolve(" Rahul Sharma ")

        assertTrue(result is ContactResolution.Resolved)
        assertEquals("Rahul Sharma", (result as ContactResolution.Resolved).contact.displayName)
        assertEquals("+919876543210", result.contact.normalizedPhoneNumber)
    }

    @Test
    fun contactResolverReturnsAmbiguousForMultiplePrefixMatches() {
        val resolver = ContactResolver(
            ContactPhoneDataSource {
                listOf(
                    ContactPhone(2, "Rahul Verma", "9123456780"),
                    ContactPhone(1, "Rahul Sharma", "9876543210")
                )
            }
        )

        val result = resolver.resolve("rahul")

        assertTrue(result is ContactResolution.Ambiguous)
        assertEquals(
            listOf("Rahul Sharma", "Rahul Verma"),
            (result as ContactResolution.Ambiguous).candidates.map(ContactPhone::displayName)
        )
    }

    @Test
    fun contactResolverCollapsesDuplicatePhoneNumbers() {
        val resolver = ContactResolver(
            ContactPhoneDataSource {
                listOf(
                    ContactPhone(1, "Mayra", "9876543210"),
                    ContactPhone(1, "Mayra Home", "+91 98765 43210")
                )
            }
        )

        val result = resolver.resolve("Mayra")

        assertTrue(result is ContactResolution.Resolved)
        assertEquals("+919876543210", (result as ContactResolution.Resolved).contact.normalizedPhoneNumber)
    }

    @Test
    fun contactResolverReturnsNotFoundForBlankOrMissingQuery() {
        val resolver = ContactResolver(ContactPhoneDataSource { emptyList() })

        assertEquals(ContactResolution.NotFound, resolver.resolve(" "))
        assertEquals(ContactResolution.NotFound, resolver.resolve("Nobody"))
    }

    @Test
    fun installedAppResolverPrefersExactPackageName() {
        val resolver = InstalledAppResolver(
            InstalledAppDataSource {
                listOf(
                    InstalledApp("YouTube", "com.google.android.youtube"),
                    InstalledApp("YouTube Music", "com.google.android.apps.youtube.music")
                )
            }
        )

        val result = resolver.resolve("com.google.android.apps.youtube.music")

        assertTrue(result is AppResolution.Resolved)
        assertEquals(
            "YouTube Music",
            (result as AppResolution.Resolved).app.label
        )
    }

    @Test
    fun installedAppResolverMatchesNormalizedLabel() {
        val resolver = InstalledAppResolver(
            InstalledAppDataSource {
                listOf(InstalledApp("Google   Maps", "com.google.android.apps.maps"))
            }
        )

        val result = resolver.resolve(" google maps ")

        assertTrue(result is AppResolution.Resolved)
        assertEquals(
            "com.google.android.apps.maps",
            (result as AppResolution.Resolved).app.packageName
        )
    }

    @Test
    fun installedAppResolverReturnsAmbiguousForSharedPrefix() {
        val resolver = InstalledAppResolver(
            InstalledAppDataSource {
                listOf(
                    InstalledApp("YouTube Music", "music.package"),
                    InstalledApp("YouTube", "video.package")
                )
            }
        )

        val result = resolver.resolve("you")

        assertTrue(result is AppResolution.Ambiguous)
        assertEquals(
            listOf("YouTube", "YouTube Music"),
            (result as AppResolution.Ambiguous).candidates.map(InstalledApp::label)
        )
    }

    @Test
    fun installedAppResolverReturnsNotFoundForUnknownApp() {
        val resolver = InstalledAppResolver(
            InstalledAppDataSource { listOf(InstalledApp("Calculator", "calculator.package")) }
        )

        assertEquals(AppResolution.NotFound, resolver.resolve("Camera"))
    }
}
