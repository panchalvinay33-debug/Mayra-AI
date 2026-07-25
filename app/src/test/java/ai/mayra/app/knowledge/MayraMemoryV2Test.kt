package ai.mayra.app.knowledge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraMemoryV2Test {
    @Test fun blocksOtpAndPasswords() {
        assertTrue(MayraMemoryPrivacyGuard.looksSensitive("My OTP is 123456"))
        assertTrue(MayraMemoryPrivacyGuard.looksSensitive("password: hello123"))
        assertTrue(MayraMemoryPrivacyGuard.looksSensitive("api key abcdef"))
    }

    @Test fun blocksCardLikeNumbers() {
        assertTrue(MayraMemoryPrivacyGuard.looksSensitive("4111111111111111"))
    }

    @Test fun allowsOrdinaryPersonalFacts() {
        assertFalse(MayraMemoryPrivacyGuard.looksSensitive("I prefer Hindi and morning reminders"))
        assertFalse(MayraMemoryPrivacyGuard.looksSensitive("My project is Mayra AI"))
    }
}
