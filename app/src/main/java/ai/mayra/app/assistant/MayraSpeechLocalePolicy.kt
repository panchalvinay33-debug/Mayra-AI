package ai.mayra.app.assistant

import java.util.Locale

object MayraSpeechLocalePolicy {
    fun candidates(deviceLocaleTag: String): List<String> = buildList {
        fun addIfNew(tag: String) {
            if (tag.isNotBlank() && none { it.equals(tag, ignoreCase = true) }) add(tag)
        }

        addIfNew(deviceLocaleTag)
        addIfNew("hi-IN")
        addIfNew("en-IN")
        addIfNew("en-US")
    }

    fun currentDeviceLocaleTag(): String = Locale.getDefault().toLanguageTag()
}
