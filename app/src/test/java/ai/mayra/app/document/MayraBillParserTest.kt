package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraBillParserTest {
    @Test
    fun `parses common invoice fields and item rate`() {
        val record = MayraBillParser().parse(
            """
            XYZ Traders
            Invoice No: XYZ-2026-071
            Bill Date: 25/07/2026
            Pipe 20 450 9000
            Curtain 15 300 4500
            GST 810
            Grand Total Rs. 14310
            Paid via UPI
            """.trimIndent()
        )

        assertEquals("XYZ-2026-071", record.invoiceNumber)
        assertEquals("2026-07-25", record.billDate)
        assertEquals("14310", record.total?.stripTrailingZeros()?.toPlainString())
        assertEquals(2, record.items.size)
        assertEquals("450", record.items.first().rate?.stripTrailingZeros()?.toPlainString())
        assertTrue(record.confidence >= 0.8)
    }

    @Test
    fun `missing fields produce warnings instead of fabricated values`() {
        val record = MayraBillParser().parse("Random note without amounts")

        assertEquals(null, record.total)
        assertEquals(null, record.billDate)
        assertTrue(record.warnings.isNotEmpty())
        assertNotNull(record.vendor)
    }
}
