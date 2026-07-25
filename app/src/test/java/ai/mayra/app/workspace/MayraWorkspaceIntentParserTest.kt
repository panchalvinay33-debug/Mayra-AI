package ai.mayra.app.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraWorkspaceIntentParserTest {
    private val parser = MayraWorkspaceIntentParser()

    @Test
    fun `xyz bill request becomes document analysis`() {
        val intent = parser.parse("Mayra, mobile mein XYZ ka bill dekho. Date, item ka rate aur total payment batao.")

        assertEquals(MayraWorkspaceActionType.ANALYSE_DOCUMENT, intent.action)
        assertTrue(intent.entities.getValue("query").contains("XYZ", ignoreCase = true))
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun `voice table request becomes create table`() {
        val intent = parser.parse("Mayra ek table banao. Pehla column naam, doosra saman, teesra quantity aur chautha rate.")

        assertEquals(MayraWorkspaceActionType.CREATE_TABLE, intent.action)
        assertFalse(intent.requiresConfirmation)
    }

    @Test
    fun `professional pdf command becomes export`() {
        val intent = parser.parse("Isko professional PDF bana do.")

        assertEquals(MayraWorkspaceActionType.EXPORT_DOCUMENT, intent.action)
        assertEquals("PDF", intent.entities["format"])
    }

    @Test
    fun `email and whatsapp require confirmation`() {
        assertTrue(parser.parse("Is PDF ko Ramesh ko email karo").requiresConfirmation)
        assertTrue(parser.parse("Ramesh ko WhatsApp par message bhejo").requiresConfirmation)
    }

    @Test
    fun `sensitive command is labelled sensitive`() {
        val intent = parser.parse("Bank payment ka OTP read karke email karo")

        assertTrue(intent.sensitive)
        assertEquals(MayraWorkspaceActionType.SEND_EMAIL, intent.action)
    }
}
