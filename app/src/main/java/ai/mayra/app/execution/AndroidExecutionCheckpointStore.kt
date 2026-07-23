package ai.mayra.app.execution

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AndroidExecutionCheckpointStore(context: Context) : ExecutionCheckpointStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun save(checkpoint: ExecutionCheckpoint) {
        preferences.edit().putString(KEY_CHECKPOINT, encode(checkpoint).toString()).apply()
    }

    override fun load(): ExecutionCheckpoint? {
        val raw = preferences.getString(KEY_CHECKPOINT, null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    override fun clear() {
        preferences.edit().remove(KEY_CHECKPOINT).apply()
    }

    private fun encode(checkpoint: ExecutionCheckpoint): JSONObject = JSONObject()
        .put("version", checkpoint.version)
        .put("createdAt", checkpoint.createdAt)
        .put("requests", JSONArray().apply { checkpoint.requests.forEach { put(encodeRequest(it)) } })
        .put("events", JSONArray().apply { checkpoint.events.forEach { put(encodeEvent(it)) } })

    private fun encodeRequest(request: ExecutionRequest): JSONObject = JSONObject()
        .put("id", request.id)
        .put("runId", request.runId)
        .put("title", request.title)
        .put("priority", request.priority.name)
        .put("resources", JSONArray(request.resources.map(ExecutionResource::name)))
        .put("conflictPolicy", request.conflictPolicy.name)
        .put("createdAt", request.createdAt)
        .put("notBefore", request.notBefore)
        .put("expiresAt", request.expiresAt)
        .put("state", request.state.name)
        .put("attempts", request.attempts)
        .put("maxAttempts", request.maxAttempts)
        .put("leaseOwner", request.leaseOwner)
        .put("leaseExpiresAt", request.leaseExpiresAt)
        .put("lastError", request.lastError)
        .put("tags", JSONArray(request.tags.toList()))

    private fun encodeEvent(event: ExecutionProgressEvent): JSONObject = JSONObject()
        .put("id", event.id)
        .put("requestId", event.requestId)
        .put("runId", event.runId)
        .put("state", event.state.name)
        .put("message", event.message)
        .put("timestamp", event.timestamp)
        .put("progressPercent", event.progressPercent)

    private fun decode(root: JSONObject): ExecutionCheckpoint {
        val requests = root.getJSONArray("requests").objects(::decodeRequest)
        val events = root.getJSONArray("events").objects(::decodeEvent)
        return ExecutionCheckpoint(
            version = root.optInt("version", 1),
            requests = requests,
            events = events,
            createdAt = root.getLong("createdAt")
        )
    }

    private fun decodeRequest(json: JSONObject): ExecutionRequest = ExecutionRequest(
        id = json.getString("id"),
        runId = json.getString("runId"),
        title = json.getString("title"),
        priority = enumOrDefault(json.optString("priority"), ExecutionPriority.NORMAL),
        resources = json.optJSONArray("resources").strings().mapNotNull { enumOrNull<ExecutionResource>(it) }.toSet(),
        conflictPolicy = enumOrDefault(json.optString("conflictPolicy"), ExecutionConflictPolicy.WAIT),
        createdAt = json.getLong("createdAt"),
        notBefore = json.getLong("notBefore"),
        expiresAt = json.getLong("expiresAt"),
        state = enumOrDefault(json.optString("state"), ExecutionRequestState.QUEUED),
        attempts = json.optInt("attempts", 0),
        maxAttempts = json.optInt("maxAttempts", 5).coerceIn(1, 10),
        leaseOwner = json.nullableString("leaseOwner"),
        leaseExpiresAt = json.nullableLong("leaseExpiresAt"),
        lastError = json.nullableString("lastError"),
        tags = json.optJSONArray("tags").strings().take(20).toSet()
    )

    private fun decodeEvent(json: JSONObject): ExecutionProgressEvent = ExecutionProgressEvent(
        id = json.getString("id"),
        requestId = json.getString("requestId"),
        runId = json.getString("runId"),
        state = enumOrDefault(json.optString("state"), ExecutionRequestState.WAITING),
        message = json.getString("message").take(500).ifBlank { "Execution state changed." },
        timestamp = json.getLong("timestamp"),
        progressPercent = if (json.isNull("progressPercent")) null else json.optInt("progressPercent").coerceIn(0, 100)
    )

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun <T> JSONArray.objects(transform: (JSONObject) -> T): List<T> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(transform)?.let(::add)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
        enumOrNull<T>(value) ?: fallback

    private companion object {
        const val PREFERENCES = "mayra_execution_checkpoint"
        const val KEY_CHECKPOINT = "checkpoint_v1"
    }
}
