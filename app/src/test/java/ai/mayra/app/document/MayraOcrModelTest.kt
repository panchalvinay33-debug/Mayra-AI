package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraOcrModelTest {

    @Test
    fun `ocr result keeps confidence and source`() {
        val result = MayraOcrResult(
            sourceId = "receipt-1",
            words = listOf(MayraOcrWord("Total", 0.98)),
            fullText = "Total 500",
            averageConfidence = 0.98,
            completed = true
        )

        assertEquals("receipt-1", result.sourceId)
        assertTrue(result.averageConfidence > 0.9)
        assertTrue(result.completed)
    }
}
