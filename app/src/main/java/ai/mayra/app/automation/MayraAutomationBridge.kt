package ai.mayra.app.automation

import ai.mayra.app.knowledge.ChecklistItem
import ai.mayra.app.knowledge.MayraPersonalIntelligence
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.knowledge.TimelineEvent
import ai.mayra.app.knowledge.TimelineEventType
import ai.mayra.app.voice.VoiceActionRequest

sealed interface MayraAutomationOutcome {
    data class Android(val result: AutomationResult) : MayraAutomationOutcome
    data class Personal(val message: String, val data: Map<String, String> = emptyMap()) : MayraAutomationOutcome
    data class Unsupported(val message: String) : MayraAutomationOutcome
    data class Failed(val message: String) : MayraAutomationOutcome
}

class MayraAutomationBridge(
    private val android: MayraAndroidAutomation,
    private val personal: MayraPersonalIntelligence
) {
    fun execute(action: VoiceActionRequest): MayraAutomationOutcome = runCatching {
        when (action.actionKey) {
            "device.open_app" -> openApp(action)
            "device.call_contact" -> dial(action)
            "communication.send_message" -> message(action)
            "personal.create_reminder" -> reminder(action)
            "personal.create_note" -> note(action)
            "personal.add_to_list" -> addToList(action)
            "personal.search_memory" -> searchMemory(action)
            "device.control" -> deviceControl(action)
            else -> MayraAutomationOutcome.Unsupported("No automation bridge exists for ${action.actionKey}.")
        }
    }.getOrElse { MayraAutomationOutcome.Failed(it.message ?: "Automation bridge failed.") }

    private fun openApp(action: VoiceActionRequest): MayraAutomationOutcome {
        val app = required(action, "app")
        return MayraAutomationOutcome.Android(
            android.execute(
                AutomationRequest(
                    type = AutomationType.OPEN_APP,
                    parameters = mapOf("package" to app),
                    confirmed = action.requiresConfirmation
                )
            )
        )
    }

    private fun dial(action: VoiceActionRequest): MayraAutomationOutcome = MayraAutomationOutcome.Android(
        android.execute(
            AutomationRequest(
                type = AutomationType.DIAL_NUMBER,
                parameters = mapOf("number" to required(action, "contact")),
                confirmed = action.requiresConfirmation
            )
        )
    )

    private fun message(action: VoiceActionRequest): MayraAutomationOutcome {
        val contact = required(action, "contact")
        val message = required(action, "message")
        val whatsapp = action.payload["channel"]?.equals("whatsapp", true) == true
        return MayraAutomationOutcome.Android(
            android.execute(
                AutomationRequest(
                    type = if (whatsapp) AutomationType.COMPOSE_WHATSAPP else AutomationType.COMPOSE_SMS,
                    parameters = mapOf("number" to contact, "message" to message),
                    confirmed = action.requiresConfirmation
                )
            )
        )
    }

    private fun reminder(action: VoiceActionRequest): MayraAutomationOutcome {
        val content = required(action, "content")
        val time = required(action, "time")
        val parsed = LocalTimeInterpreter.parse(time)
        personal.record(
            TimelineEvent(
                type = TimelineEventType.REMINDER,
                title = content,
                description = "Requested for $time",
                occurredAt = parsed?.epochMillis ?: System.currentTimeMillis(),
                metadata = mapOf("spoken_time" to time)
            )
        )
        if (parsed == null) return MayraAutomationOutcome.Personal("Reminder saved, but Android alarm time needs clarification.")
        return MayraAutomationOutcome.Android(
            android.execute(
                AutomationRequest(
                    type = AutomationType.CREATE_ALARM,
                    parameters = mapOf("hour" to parsed.hour.toString(), "minute" to parsed.minute.toString(), "label" to content),
                    confirmed = true
                )
            )
        )
    }

    private fun note(action: VoiceActionRequest): MayraAutomationOutcome {
        val content = required(action, "content")
        val saved = personal.saveNote(PersonalNote(title = content.take(80), body = content))
        return MayraAutomationOutcome.Personal("Note saved.", mapOf("noteId" to saved.id))
    }

    private fun addToList(action: VoiceActionRequest): MayraAutomationOutcome {
        val listName = action.payload["list"].orEmpty().ifBlank { "default" }
        val content = required(action, "content")
        val saved = personal.saveNote(
            PersonalNote(
                type = if (listName.equals("shopping", true)) PersonalNoteType.SHOPPING_LIST else PersonalNoteType.CHECKLIST,
                title = listName.replaceFirstChar { it.uppercase() },
                checklist = listOf(ChecklistItem(text = content)),
                priority = 2
            )
        )
        return MayraAutomationOutcome.Personal("Added to $listName list.", mapOf("noteId" to saved.id))
    }

    private fun searchMemory(action: VoiceActionRequest): MayraAutomationOutcome {
        val query = required(action, "query")
        val result = personal.search(query)
        val summary = buildList {
            result.knowledge.take(3).forEach { add(it.entity.name) }
            result.memory.take(3).forEach { add(it.title) }
        }.distinct()
        return MayraAutomationOutcome.Personal(
            if (summary.isEmpty()) "Nothing matching $query was found." else "Found ${summary.size} matching items.",
            mapOf("matches" to summary.joinToString(" | "))
        )
    }

    private fun deviceControl(action: VoiceActionRequest): MayraAutomationOutcome {
        val control = required(action, "control").lowercase()
        val operation = action.payload["operation"].orEmpty().lowercase()
        val request = when (control) {
            "wifi" -> AutomationRequest(AutomationType.OPEN_WIFI_SETTINGS)
            "bluetooth" -> AutomationRequest(AutomationType.OPEN_BLUETOOTH_SETTINGS)
            "torch" -> AutomationRequest(AutomationType.SET_FLASHLIGHT, mapOf("enabled" to (operation != "off").toString()))
            "volume" -> AutomationRequest(AutomationType.CHANGE_MEDIA_VOLUME, mapOf("operation" to operation.ifBlank { "increase" }))
            "brightness" -> AutomationRequest(AutomationType.SET_BRIGHTNESS, mapOf("value" to brightnessValue(operation)))
            "silent mode" -> AutomationRequest(AutomationType.OPEN_DND_SETTINGS)
            else -> return MayraAutomationOutcome.Unsupported("Unsupported device control: $control")
        }
        return MayraAutomationOutcome.Android(android.execute(request))
    }

    private fun brightnessValue(operation: String): String = when (operation) {
        "increase", "high", "on" -> "220"
        "decrease", "low", "off" -> "60"
        else -> "140"
    }

    private fun required(action: VoiceActionRequest, key: String): String = action.payload[key]?.trim()?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("Missing voice action parameter: $key")
}

data class ParsedLocalTime(val hour: Int, val minute: Int, val epochMillis: Long)

object LocalTimeInterpreter {
    fun parse(value: String, now: Long = System.currentTimeMillis()): ParsedLocalTime? {
        val lower = value.trim().lowercase()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        if (lower.contains("tomorrow") || lower.contains("kal")) calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val explicit = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(lower)
        val hour: Int
        val minute: Int
        if (explicit != null) {
            var h = explicit.groupValues[1].toIntOrNull() ?: return null
            minute = explicit.groupValues[2].toIntOrNull() ?: 0
            val meridiem = explicit.groupValues[3]
            if (h !in 0..23 || minute !in 0..59) return null
            if (meridiem == "pm" && h < 12) h += 12
            if (meridiem == "am" && h == 12) h = 0
            hour = h
        } else {
            hour = when {
                lower.contains("subah") || lower.contains("morning") -> 8
                lower.contains("dopahar") || lower.contains("afternoon") -> 15
                lower.contains("shaam") || lower.contains("evening") -> 19
                lower.contains("raat") || lower.contains("night") -> 21
                else -> return null
            }
            minute = 0
        }
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        if (!lower.contains("tomorrow") && !lower.contains("kal") && calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return ParsedLocalTime(hour, minute, calendar.timeInMillis)
    }
}
