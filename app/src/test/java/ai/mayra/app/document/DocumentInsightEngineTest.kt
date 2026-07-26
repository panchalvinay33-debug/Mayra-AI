package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentInsightEngineTest {
    @Test
    fun detectsSummaryIntentInEnglishAndHindi() {
        assertEquals(
            DocumentQueryIntent.SUMMARY,
            DocumentInsightEngine.detectIntent("Summarize my project document")
        )
        assertEquals(
            DocumentQueryIntent.SUMMARY,
            DocumentInsightEngine.detectIntent("इस फाइल का सारांश बताओ")
        )
    }

    @Test
    fun detectsQuestionAndSearchIntent() {
        assertEquals(
            DocumentQueryIntent.QUESTION,
            DocumentInsightEngine.detectIntent("What are the payment terms in this file?")
        )
        assertEquals(
            DocumentQueryIntent.SEARCH,
            DocumentInsightEngine.detectIntent("Search files for invoice 482")
        )
    }

    @Test
    fun summaryPrioritizesRepeatedTopicsAndKeepsOriginalOrder() {
        val text = """
            Project Mayra is an offline-first Android assistant.
            The weather was pleasant during the meeting.
            Local document search protects private user data on the device.
            The team discussed lunch after the review.
            Document search and document summaries use indexed text on the device.
        """.trimIndent()

        val summary = DocumentInsightEngine.summarize(text, maxSentences = 3)

        assertTrue(summary.contains("Project Mayra"))
        assertTrue(summary.contains("Local document search"))
        assertTrue(summary.contains("Document search and document summaries"))
        assertTrue(summary.indexOf("Project Mayra") < summary.indexOf("Local document search"))
        assertTrue(summary.indexOf("Local document search") < summary.indexOf("Document search and document summaries"))
    }

    @Test
    fun answerReturnsOnlyMatchingEvidence() {
        val text = """
            The invoice total is 48,500 rupees.
            Payment is due within 30 days from the invoice date.
            The office address is Pitol, Madhya Pradesh.
        """.trimIndent()

        val result = DocumentInsightEngine.answer("What are the payment terms?", text)

        assertNotNull(result)
        val (answer, evidence) = result!!
        assertTrue(answer.contains("30 days"))
        assertEquals(1, evidence.size)
        assertFalse(answer.contains("office address"))
    }

    @Test
    fun answerReturnsNullWhenTextHasNoGroundedMatch() {
        val result = DocumentInsightEngine.answer(
            "What is the cancellation policy?",
            "Payment is due within 30 days. The office is open Monday to Friday."
        )

        assertNull(result)
    }

    @Test
    fun confidenceReflectsCoveredQuestionTerms() {
        val confidence = DocumentInsightEngine.confidence(
            "payment due date",
            listOf("Payment is due within 30 days from the invoice date.")
        )

        assertTrue(confidence >= 60)
        assertTrue(confidence <= 100)
    }
}
