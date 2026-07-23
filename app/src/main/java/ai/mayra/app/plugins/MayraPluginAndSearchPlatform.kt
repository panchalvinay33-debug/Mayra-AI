package ai.mayra.app.plugins

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

enum class MayraCapability {
    SEARCH,
    READ_CONTACTS,
    READ_CALENDAR,
    WRITE_CALENDAR,
    READ_FILES,
    WRITE_FILES,
    SEND_MESSAGE,
    SEND_EMAIL,
    LOCATION,
    MEDIA_CONTROL,
    SMART_HOME,
    FINANCIAL_REFERENCE,
    NETWORK
}

enum class PluginTrustLevel { LOCAL, USER_CONNECTED, VERIFIED_PARTNER, RESTRICTED }
enum class PluginHealth { READY, DEGRADED, DISABLED, OPEN_CIRCUIT }

data class PluginDescriptor(
    val id: String,
    val displayName: String,
    val version: String,
    val description: String,
    val capabilities: Set<MayraCapability>,
    val requiredPermissions: Set<String> = emptySet(),
    val trustLevel: PluginTrustLevel = PluginTrustLevel.LOCAL,
    val networkRequired: Boolean = false,
    val handlesSensitiveData: Boolean = false
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]{3,80}")))
        require(displayName.isNotBlank())
        require(version.isNotBlank())
        require(capabilities.isNotEmpty())
        require(requiredPermissions.size <= 30)
    }
}

data class PluginContext(
    val locale: String = "hi-IN",
    val confirmed: Boolean = false,
    val grantedPermissions: Set<String> = emptySet(),
    val attributes: Map<String, String> = emptyMap(),
    val now: Long = System.currentTimeMillis()
)

data class PluginRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val operation: String,
    val parameters: Map<String, String> = emptyMap(),
    val context: PluginContext = PluginContext(),
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MS
) {
    init {
        require(operation.isNotBlank())
        require(parameters.size <= 40)
        require(timeoutMillis in 250L..30_000L)
        require(parameters.all { it.key.length <= 100 && it.value.length <= 4_000 })
    }

    companion object { const val DEFAULT_TIMEOUT_MS = 8_000L }
}

sealed interface PluginResult {
    data class Success(
        val message: String,
        val data: Map<String, String> = emptyMap(),
        val followUpAction: String? = null
    ) : PluginResult

    data class NeedsConfirmation(val prompt: String, val risk: String) : PluginResult
    data class MissingPermission(val permissions: Set<String>, val explanation: String) : PluginResult
    data class Unsupported(val reason: String) : PluginResult
    data class Failure(val reason: String, val retryable: Boolean = false) : PluginResult
}

interface MayraPlugin {
    val descriptor: PluginDescriptor
    fun canHandle(request: PluginRequest): Double
    suspend fun execute(request: PluginRequest): PluginResult
    suspend fun healthCheck(): PluginHealth = PluginHealth.READY
}

data class PluginRuntimeSnapshot(
    val id: String,
    val health: PluginHealth,
    val executions: Int,
    val successes: Int,
    val failures: Int,
    val consecutiveFailures: Int,
    val lastUsedAt: Long?,
    val circuitOpenedAt: Long?
)

class MayraPluginRegistry(
    private val failureThreshold: Int = 3,
    private val circuitResetMillis: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Runtime(
        val plugin: MayraPlugin,
        var enabled: Boolean = true,
        var executions: Int = 0,
        var successes: Int = 0,
        var failures: Int = 0,
        var consecutiveFailures: Int = 0,
        var lastUsedAt: Long? = null,
        var circuitOpenedAt: Long? = null
    )

    private val plugins = ConcurrentHashMap<String, Runtime>()

    init {
        require(failureThreshold in 1..20)
        require(circuitResetMillis in 1_000L..24L * 60 * 60 * 1000)
    }

    @Synchronized
    fun register(plugin: MayraPlugin) {
        require(plugins[plugin.descriptor.id] == null) { "Plugin already registered: ${plugin.descriptor.id}" }
        plugins[plugin.descriptor.id] = Runtime(plugin)
    }

    @Synchronized
    fun replace(plugin: MayraPlugin) {
        val existing = plugins[plugin.descriptor.id]
        plugins[plugin.descriptor.id] = Runtime(
            plugin = plugin,
            enabled = existing?.enabled ?: true,
            executions = existing?.executions ?: 0,
            successes = existing?.successes ?: 0,
            failures = existing?.failures ?: 0,
            consecutiveFailures = existing?.consecutiveFailures ?: 0,
            lastUsedAt = existing?.lastUsedAt,
            circuitOpenedAt = existing?.circuitOpenedAt
        )
    }

    @Synchronized
    fun unregister(id: String): Boolean = plugins.remove(id) != null

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        val runtime = plugins[id] ?: return false
        runtime.enabled = enabled
        if (enabled) {
            runtime.consecutiveFailures = 0
            runtime.circuitOpenedAt = null
        }
        return true
    }

    fun descriptors(): List<PluginDescriptor> = plugins.values.map { it.plugin.descriptor }.sortedBy { it.displayName }

    suspend fun executeBest(request: PluginRequest): PluginResult {
        val candidates = plugins.values.asSequence()
            .filter(::isAvailable)
            .map { it to runCatching { it.plugin.canHandle(request).coerceIn(0.0, 1.0) }.getOrDefault(0.0) }
            .filter { it.second >= MIN_CONFIDENCE }
            .sortedByDescending { it.second }
            .toList()

        val selected = candidates.firstOrNull()?.first
            ?: return PluginResult.Unsupported("No connected plugin can handle ${request.operation}.")
        return execute(selected.plugin.descriptor.id, request)
    }

    suspend fun execute(id: String, request: PluginRequest): PluginResult {
        val runtime = plugins[id] ?: return PluginResult.Unsupported("Plugin $id is not registered.")
        if (!isAvailable(runtime)) return PluginResult.Failure("Plugin ${runtime.plugin.descriptor.displayName} is unavailable.", true)

        val missing = runtime.plugin.descriptor.requiredPermissions - request.context.grantedPermissions
        if (missing.isNotEmpty()) {
            return PluginResult.MissingPermission(missing, "Required plugin permissions are missing.")
        }
        if (runtime.plugin.descriptor.handlesSensitiveData && !request.context.confirmed) {
            return PluginResult.NeedsConfirmation(
                prompt = "${runtime.plugin.descriptor.displayName} sensitive information use kar sakta hai. Continue?",
                risk = "sensitive_data"
            )
        }

        synchronized(this) {
            runtime.executions++
            runtime.lastUsedAt = clock()
        }
        val result = runCatching { runtime.plugin.execute(request) }
            .getOrElse { PluginResult.Failure(it.message ?: "Plugin execution failed", retryable = true) }

        synchronized(this) {
            when (result) {
                is PluginResult.Success -> {
                    runtime.successes++
                    runtime.consecutiveFailures = 0
                    runtime.circuitOpenedAt = null
                }
                is PluginResult.Failure -> {
                    runtime.failures++
                    runtime.consecutiveFailures++
                    if (runtime.consecutiveFailures >= failureThreshold) runtime.circuitOpenedAt = clock()
                }
                else -> Unit
            }
        }
        return result
    }

    @Synchronized
    fun snapshots(): List<PluginRuntimeSnapshot> = plugins.values.map { runtime ->
        PluginRuntimeSnapshot(
            id = runtime.plugin.descriptor.id,
            health = when {
                !runtime.enabled -> PluginHealth.DISABLED
                isCircuitOpen(runtime) -> PluginHealth.OPEN_CIRCUIT
                runtime.consecutiveFailures > 0 -> PluginHealth.DEGRADED
                else -> PluginHealth.READY
            },
            executions = runtime.executions,
            successes = runtime.successes,
            failures = runtime.failures,
            consecutiveFailures = runtime.consecutiveFailures,
            lastUsedAt = runtime.lastUsedAt,
            circuitOpenedAt = runtime.circuitOpenedAt
        )
    }.sortedBy { it.id }

    @Synchronized
    fun resetCircuit(id: String): Boolean {
        val runtime = plugins[id] ?: return false
        runtime.consecutiveFailures = 0
        runtime.circuitOpenedAt = null
        return true
    }

    private fun isAvailable(runtime: Runtime): Boolean = runtime.enabled && !isCircuitOpen(runtime)

    private fun isCircuitOpen(runtime: Runtime): Boolean {
        val opened = runtime.circuitOpenedAt ?: return false
        if (clock() - opened >= circuitResetMillis) {
            synchronized(this) {
                runtime.circuitOpenedAt = null
                runtime.consecutiveFailures = 0
            }
            return false
        }
        return true
    }

    companion object { const val MIN_CONFIDENCE = 0.35 }
}

enum class UnifiedSearchDomain {
    CONTACT,
    APP,
    NOTE,
    REMINDER,
    FILE,
    CALENDAR,
    MESSAGE,
    KNOWLEDGE,
    WEB,
    PLUGIN
}

data class UnifiedSearchQuery(
    val text: String,
    val domains: Set<UnifiedSearchDomain> = UnifiedSearchDomain.entries.toSet(),
    val limit: Int = 20,
    val includeSensitive: Boolean = false,
    val now: Long = System.currentTimeMillis()
) {
    init {
        require(text.isNotBlank())
        require(limit in 1..100)
        require(domains.isNotEmpty())
    }
}

data class UnifiedSearchItem(
    val id: String,
    val domain: UnifiedSearchDomain,
    val title: String,
    val subtitle: String = "",
    val score: Double,
    val timestamp: Long? = null,
    val actionKey: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val sensitive: Boolean = false,
    val source: String
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(score in 0.0..1.0)
        require(metadata.size <= 30)
    }
}

interface UnifiedSearchProvider {
    val id: String
    val domains: Set<UnifiedSearchDomain>
    suspend fun search(query: UnifiedSearchQuery): List<UnifiedSearchItem>
}

data class UnifiedSearchDiagnostics(
    val providersQueried: Int,
    val providersFailed: Int,
    val rawResults: Int,
    val returnedResults: Int,
    val elapsedMillis: Long
)

data class UnifiedSearchResponse(
    val items: List<UnifiedSearchItem>,
    val diagnostics: UnifiedSearchDiagnostics
)

class MayraUnifiedSearch(
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val providers = ConcurrentHashMap<String, UnifiedSearchProvider>()
    private val providerFailures = ConcurrentHashMap<String, AtomicInteger>()

    fun register(provider: UnifiedSearchProvider) {
        require(provider.id.matches(Regex("[a-z0-9_.-]{3,80}")))
        require(provider.domains.isNotEmpty())
        providers[provider.id] = provider
    }

    fun unregister(id: String): Boolean = providers.remove(id) != null

    suspend fun search(query: UnifiedSearchQuery): UnifiedSearchResponse {
        val started = clock()
        val eligible = providers.values.filter { provider -> provider.domains.any(query.domains::contains) }
        var failures = 0
        val raw = mutableListOf<UnifiedSearchItem>()

        eligible.forEach { provider ->
            val result = runCatching { provider.search(query) }
                .onFailure {
                    failures++
                    providerFailures.computeIfAbsent(provider.id) { AtomicInteger() }.incrementAndGet()
                }
                .getOrDefault(emptyList())
            raw += result
                .asSequence()
                .filter { it.domain in query.domains }
                .filter { query.includeSensitive || !it.sensitive }
                .map { it.copy(score = rerank(it, query)) }
                .toList()
        }

        val items = raw
            .distinctBy { "${it.domain}:${it.id}" }
            .sortedWith(compareByDescending<UnifiedSearchItem> { it.score }.thenByDescending { it.timestamp ?: Long.MIN_VALUE })
            .take(query.limit)

        return UnifiedSearchResponse(
            items = items,
            diagnostics = UnifiedSearchDiagnostics(
                providersQueried = eligible.size,
                providersFailed = failures,
                rawResults = raw.size,
                returnedResults = items.size,
                elapsedMillis = max(0, clock() - started)
            )
        )
    }

    fun providerFailureCounts(): Map<String, Int> = providerFailures.mapValues { it.value.get() }

    private fun rerank(item: UnifiedSearchItem, query: UnifiedSearchQuery): Double {
        val terms = tokenize(query.text)
        val corpus = tokenize(item.title + " " + item.subtitle + " " + item.metadata.values.joinToString(" "))
        val matched = terms.count { term -> corpus.any { token -> token == term || token.contains(term) } }
        val lexical = if (terms.isEmpty()) 0.0 else matched.toDouble() / terms.size
        val freshness = item.timestamp?.let { timestamp ->
            val ageDays = ((query.now - timestamp).coerceAtLeast(0) / DAY_MS).toDouble()
            (1.0 - ageDays / 90.0).coerceIn(0.0, 1.0)
        } ?: 0.45
        val exact = if (item.title.equals(query.text.trim(), ignoreCase = true)) 0.15 else 0.0
        return (item.score * 0.55 + lexical * 0.30 + freshness * 0.15 + exact).coerceIn(0.0, 1.0)
    }

    private fun tokenize(value: String): Set<String> = value.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }
        .toSet()

    private companion object { const val DAY_MS = 24L * 60 * 60 * 1000 }
}

class PluginSearchProvider(
    private val registry: MayraPluginRegistry,
    override val id: String = "mayra.plugin.search"
) : UnifiedSearchProvider {
    override val domains: Set<UnifiedSearchDomain> = setOf(UnifiedSearchDomain.PLUGIN, UnifiedSearchDomain.WEB)

    override suspend fun search(query: UnifiedSearchQuery): List<UnifiedSearchItem> {
        val result = registry.executeBest(
            PluginRequest(
                operation = "search",
                parameters = mapOf("query" to query.text, "limit" to query.limit.toString()),
                context = PluginContext(confirmed = query.includeSensitive)
            )
        )
        return when (result) {
            is PluginResult.Success -> result.data.entries.take(query.limit).mapIndexed { index, entry ->
                UnifiedSearchItem(
                    id = "plugin-$index-${entry.key.hashCode()}",
                    domain = UnifiedSearchDomain.PLUGIN,
                    title = entry.key,
                    subtitle = entry.value,
                    score = 0.62,
                    source = id
                )
            }
            else -> emptyList()
        }
    }
}
