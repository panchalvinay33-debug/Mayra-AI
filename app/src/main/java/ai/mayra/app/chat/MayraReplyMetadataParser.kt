package ai.mayra.app.chat

import ai.mayra.app.memory.PersonalMemoryAwareMayraAssistant
import java.util.Base64

data class ParsedMayraReply(val text: String, val usedPersonalMemoryKeys: List<String>)

object MayraReplyMetadataParser {
    fun parse(raw: String): ParsedMayraReply {
        val start = raw.lastIndexOf(PersonalMemoryAwareMayraAssistant.USAGE_MARKER)
        if (start < 0 || !raw.endsWith(PersonalMemoryAwareMayraAssistant.USAGE_SUFFIX)) {
            return ParsedMayraReply(raw.trimEnd(), emptyList())
        }
        val payloadStart = start + PersonalMemoryAwareMayraAssistant.USAGE_MARKER.length
        val payloadEnd = raw.length - PersonalMemoryAwareMayraAssistant.USAGE_SUFFIX.length
        val keys = raw.substring(payloadStart, payloadEnd)
            .split(',')
            .filter(String::isNotBlank)
            .mapNotNull { encoded ->
                runCatching {
                    String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8).trim()
                }.getOrNull()?.takeIf(String::isNotBlank)
            }
            .distinct()
        return ParsedMayraReply(raw.substring(0, start).trimEnd(), keys)
    }
}
