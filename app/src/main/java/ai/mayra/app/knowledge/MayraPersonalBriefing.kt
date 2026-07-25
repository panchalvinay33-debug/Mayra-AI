package ai.mayra.app.knowledge

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds a small offline briefing from owner-visible, non-sensitive personal memory. */
class MayraPersonalBriefing(context: Context) {
    private val memory = MayraPersonalMemory(context)

    fun compose(now: Long = System.currentTimeMillis(), maxItems: Int = 6): PersonalBriefing {
        val dayStart = now - 24L * 60 * 60 * 1000
        val recentEvents = memory.timeline(from = dayStart, to = now, includeSensitive = false, limit = maxItems)
        val pinned = memory.notes().filter { it.pinned && !it.sensitive }.take(maxItems)
        val openChecklist = memory.notes().flatMap { note ->
            note.checklist.filterNot { it.completed }.map { "${note.title}: ${it.text}" }
        }.take(maxItems)

        val highlights = buildList {
            recentEvents.take(3).forEach { add(it.title) }
            pinned.take(2).forEach { add("Pinned: ${it.title}") }
            openChecklist.take(2).forEach { add("Open: $it") }
        }.distinct().take(maxItems)

        val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now))
        return PersonalBriefing(
            title = "Your day · $date",
            summary = when {
                highlights.isEmpty() -> "Nothing urgent is stored in Mayra memory."
                highlights.size == 1 -> "One personal item may need your attention."
                else -> "${highlights.size} personal items may be useful today."
            },
            highlights = highlights,
            generatedAt = now
        )
    }
}

data class PersonalBriefing(
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val generatedAt: Long
)
