package ai.mayra.app.document

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class MayraDocumentAnalysisPipelineTest {

    @Test
    fun `analysis pipeline returns source and parsed bill`() {
        val result = MayraDocumentAnalysisPipeline().analyseTextDocument(
            MayraDocumentSource(
                uri = "content://bill/1",
                fileName = "bill.txt",
                mimeType = "text/plain",
                modifiedTime = 1L
            ),
            "Vendor Shop\nInvoice No: X-1\nTotal Amount 500"
        )

        assertEquals("content://bill/1", result.source.uri)
        assertEquals("500", result.bill.total?.toPlainString())
        assertTrue(result.readyForExport)
    }
}
