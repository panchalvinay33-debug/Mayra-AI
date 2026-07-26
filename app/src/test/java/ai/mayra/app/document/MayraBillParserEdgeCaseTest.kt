package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraBillParserEdgeCaseTest {

    @Test
    fun `parses indian style invoice fields and keeps warnings honest`() {
        val result = MayraBillParser().parse(
            """
            Shree Shyam Event Management
            Invoice No: SS-2026-55
            Bill Date: 25/07/2026
            Pipe 10 450 4500
            Curtain 5 300 1500
            GST 1080
            Grand Total Rs. 7080
            Paid via UPI
            """.trimIndent()
        )

        assertEquals("SS-2026-55", result.invoiceNumber)
        assertEquals("2026-07-25", result.billDate)
        assertEquals("7080", result.total?.toPlainString())
        assertTrue(result.items.size >= 2)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `does not claim complete extraction without total`() {
        val result = MayraBillParser().parse(
            """
            Vendor ABC
            Invoice No: ABC-1
            Item One 2 100 200
            """.trimIndent()
        )

        assertTrue(result.total == null)
        assertTrue(result.warnings.any { it.contains("Total") })
        assertTrue(result.confidence < 1.0)
    }
}
