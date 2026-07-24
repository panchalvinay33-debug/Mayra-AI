package ai.mayra.app.identity

import ai.mayra.app.TestMayraApplication
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class MayraContactIdentityEngineTest {
    private val mummy = MayraContactIdentity(
        id = "mummy",
        canonicalContactName = "Sunita Panchal",
        relationship = "Mummy",
        aliases = setOf("Maa", "Mom"),
        preferredChannel = MayraCommunicationChannel.WHATSAPP,
        trust = MayraContactTrust.TRUSTED
    )

    @Test
    fun `relationship resolves exact canonical contact`() {
        val result = MayraContactIdentityEngine { listOf(mummy) }.resolve("MUMMY")

        val resolved = assertIs<MayraIdentityResolution.Resolved>(result)
        assertEquals("Sunita Panchal", resolved.identity.canonicalContactName)
        assertTrue(resolved.exact)
    }

    @Test
    fun `alias normalization ignores punctuation and spacing`() {
        val result = MayraContactIdentityEngine { listOf(mummy) }.resolve("  maa!!! ")

        assertIs<MayraIdentityResolution.Resolved>(result)
    }

    @Test
    fun `partial unique identity can resolve`() {
        val doctor = MayraContactIdentity(canonicalContactName = "Dr Rajesh Sharma", relationship = "Family Doctor")

        val result = MayraContactIdentityEngine { listOf(doctor) }.resolve("family")

        assertEquals("Dr Rajesh Sharma", assertIs<MayraIdentityResolution.Resolved>(result).identity.canonicalContactName)
    }

    @Test
    fun `duplicate alias is ambiguous and never guessed`() {
        val officeRahul = MayraContactIdentity(canonicalContactName = "Rahul Verma", aliases = setOf("Rahul"))
        val schoolRahul = MayraContactIdentity(canonicalContactName = "Rahul Sharma", aliases = setOf("Rahul"))

        val result = MayraContactIdentityEngine { listOf(officeRahul, schoolRahul) }.resolve("Rahul")

        val ambiguous = assertIs<MayraIdentityResolution.Ambiguous>(result)
        assertEquals(2, ambiguous.candidates.size)
    }

    @Test
    fun `unknown identity remains unmapped for Android contact fallback`() {
        assertIs<MayraIdentityResolution.Unmapped>(
            MayraContactIdentityEngine { listOf(mummy) }.resolve("Amit")
        )
    }

    @Test
    fun `safety summary includes relationship trust and channel`() {
        val summary = identitySafetySummary(mummy)

        assertTrue(summary.contains("Mummy"))
        assertTrue(summary.contains("trusted"))
        assertTrue(summary.contains("whatsapp"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class MayraContactIdentityStoreTest {
    @Test
    fun `identity persists and can be removed without touching Android contact`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MayraContactIdentityStore(context)
        store.clear()
        val identity = MayraContactIdentity(
            id = "boss",
            canonicalContactName = "Vikas Sir",
            relationship = "Boss",
            aliases = setOf("Office Boss"),
            preferredChannel = MayraCommunicationChannel.PHONE,
            trust = MayraContactTrust.SENSITIVE
        )

        store.upsert(identity)

        val stored = store.all().single()
        assertEquals("Vikas Sir", stored.canonicalContactName)
        assertEquals(MayraContactTrust.SENSITIVE, stored.trust)
        assertTrue(store.remove("boss"))
        assertTrue(store.all().isEmpty())
        assertFalse(store.remove("missing"))
    }

    @Test
    fun `upsert replaces same identity id and normalizes aliases`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MayraContactIdentityStore(context)
        store.clear()
        store.upsert(MayraContactIdentity(id = "doctor", canonicalContactName = "Dr A", aliases = setOf("  Doctor  ")))
        store.upsert(MayraContactIdentity(id = "doctor", canonicalContactName = "Dr B", aliases = setOf("Family   Doctor")))

        val stored = store.all().single()
        assertEquals("Dr B", stored.canonicalContactName)
        assertEquals(setOf("Family Doctor"), stored.aliases)
    }
}