package ai.mayra.app.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLearningCommandParserTest {
    @Test fun parsesEnglishRemember() {
        assertEquals(
            LearningCommand.Remember("response language", "Hinglish"),
            MayraLearningCommandParser.parse("Remember response language is Hinglish")
        )
    }

    @Test fun parsesHindiRemember() {
        assertEquals(
            LearningCommand.Remember("जवाब की भाषा", "हिंग्लिश"),
            MayraLearningCommandParser.parse("याद रखो कि जवाब की भाषा है हिंग्लिश")
        )
    }

    @Test fun parsesForgetAndReviewCommands() {
        assertEquals(LearningCommand.Forget("response language"), MayraLearningCommandParser.parse("forget response language"))
        assertEquals(LearningCommand.ForgetAll, MayraLearningCommandParser.parse("sab bhool jao"))
        assertEquals(LearningCommand.ListLearned, MayraLearningCommandParser.parse("tumne kya seekha"))
        assertEquals(LearningCommand.ReviewPending, MayraLearningCommandParser.parse("pending memories"))
    }

    @Test fun unrelatedTextDoesNotWriteMemory() {
        assertEquals(LearningCommand.None, MayraLearningCommandParser.parse("Aaj mausam kaisa hai?"))
    }

    @Test fun normalizationIsBoundedAndStable() {
        assertEquals("response_language", MayraLearningRepository.normalizeKey(" Response Language "))
        assertTrue(MayraLearningRepository.normalizeKey("x".repeat(200)).length <= 80)
    }
}
