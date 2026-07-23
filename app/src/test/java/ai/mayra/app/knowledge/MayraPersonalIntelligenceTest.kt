package ai.mayra.app.knowledge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraPersonalIntelligenceTest {
    private lateinit var context: Context
    private lateinit var knowledge: MayraKnowledgeStore
    private lateinit var memory: MayraPersonalMemory
    private lateinit var intelligence: MayraPersonalIntelligence

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_knowledge_graph", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_personal_memory", Context.MODE_PRIVATE).edit().clear().commit()
        knowledge = MayraKnowledgeStore(context)
        memory = MayraPersonalMemory(context)
        intelligence = MayraPersonalIntelligence(knowledge, memory)
    }

    @Test
    fun knowledgeGraphStoresRelationsAndSearchesAliases() {
        val person = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.PERSON, name = "Mayra", aliases = setOf("Maya"), importance = 5, confidence = 0.9)
        )
        val project = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.PROJECT, name = "Mayra AI", tags = setOf("android", "assistant"), importance = 5)
        )
        intelligence.relate(KnowledgeRelation(fromId = person.id, toId = project.id, type = KnowledgeRelationType.WORKS_ON, confidence = 0.8))

        assertEquals(project.id, knowledge.related(person.id).single().second.id)
        assertEquals(person.id, intelligence.search("Maya").knowledge.first().entity.id)
        assertEquals(project.id, intelligence.search("android assistant").knowledge.first().entity.id)
    }

    @Test
    fun sensitiveKnowledgeAndNotesAreHiddenByDefault() {
        intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.ACCOUNT_REFERENCE, name = "Bank recovery hint", sensitive = true, importance = 5)
        )
        intelligence.saveNote(
            PersonalNote(type = PersonalNoteType.SECURE_REFERENCE, title = "Locker reference", body = "Stored outside Mayra", sensitive = true)
        )

        assertTrue(intelligence.search("Bank").knowledge.isEmpty())
        assertTrue(intelligence.search("Locker").memory.isEmpty())
        assertEquals(1, intelligence.search("Bank", includeSensitive = true).knowledge.size)
        assertEquals(1, intelligence.search("Locker", includeSensitive = true).memory.size)
    }

    @Test
    fun checklistCompletionUpdatesDiagnostics() {
        val first = ChecklistItem(text = "Create architecture")
        val second = ChecklistItem(text = "Run tests")
        val note = intelligence.saveNote(
            PersonalNote(type = PersonalNoteType.CHECKLIST, title = "Release preparation", checklist = listOf(first, second), priority = 5)
        )

        val updated = memory.toggleChecklist(note.id, first.id, completed = true)

        assertNotNull(updated)
        assertTrue(updated!!.checklist.first { it.id == first.id }.completed)
        assertEquals(2, memory.diagnostics().checklistItems)
        assertEquals(1, memory.diagnostics().completedChecklistItems)
    }

    @Test
    fun interactionLinksTimelineAndIncrementsUsage() {
        val person = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.PERSON, name = "Shiv", importance = 5)
        )

        val event = intelligence.recordInteraction(person.id, TimelineEventType.CALL, "Called Shiv")

        assertTrue(person.id in event.linkedEntityIds)
        assertEquals(1, knowledge.get(person.id)?.usageCount)
        assertEquals(event.id, memory.timeline(entityId = person.id).single().id)
    }

    @Test
    fun recommendationsRespectQuietHoursAndSensitiveItems() {
        val project = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.PROJECT, name = "Mayra AI", importance = 5, confidence = 0.9)
        )
        intelligence.saveNote(PersonalNote(title = "Finish runtime", priority = 5, pinned = true))
        intelligence.saveNote(PersonalNote(type = PersonalNoteType.SECURE_REFERENCE, title = "Private reference", priority = 5, sensitive = true))

        val quiet = intelligence.recommendations(
            PersonalContext(hourOfDay = 23, userAvailable = true, quietHours = true, activeEntityIds = setOf(project.id))
        )
        val active = intelligence.recommendations(
            PersonalContext(hourOfDay = 11, userAvailable = true, quietHours = false, activeEntityIds = setOf(project.id))
        )

        assertTrue(quiet.isNotEmpty())
        assertTrue(quiet.all { it.action == RecommendationAction.DEFER || it.action == RecommendationAction.ASK })
        assertTrue(active.any { it.action == RecommendationAction.SUGGEST && it.linkedEntityId == project.id })
        assertTrue(active.any { it.action == RecommendationAction.ASK && it.linkedNoteId != null })
    }

    @Test
    fun expiredKnowledgeIsRemovedDuringPruning() {
        val expired = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.EVENT, name = "Old event", expiresAt = 1L)
        )
        val active = intelligence.rememberEntity(
            KnowledgeEntity(type = KnowledgeEntityType.INTEREST, name = "Android development")
        )

        intelligence.prune()

        assertEquals(null, knowledge.get(expired.id))
        assertNotNull(knowledge.get(active.id))
        assertFalse(knowledge.entities(includeExpired = true).any { it.id == expired.id })
    }
}
