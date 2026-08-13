package ai.mayra.app.j4

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * Crash-isolated J4 LiteRT-LM runtime probe.
 *
 * The launcher never directly touches LiteRT-LM classes. This service runs in :localbrain and
 * uses reflection so the existing Kotlin 2.0 / Java 17 Mayra source does not compile against
 * LiteRT-LM 0.15.0's newer Kotlin metadata / Java-21 classfiles.
 *
 * Native generation is synchronous in this probe. Therefore explicit cancellation is implemented
 * as a bounded isolated-process cancellation boundary instead of pretending sendMessage() supports
 * cooperative cancellation. The UI process survives and rebinds a fresh :localbrain process.
 */
class J4LocalBrainRuntimeService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var engine: Any? = null
    private var conversation: Any? = null
    private var generationCount: Int = 0

    private val incoming = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MSG_LOAD -> {
                val path = msg.data.getString(KEY_MODEL_PATH).orEmpty()
                val reply = msg.replyTo
                worker.execute { initializeRuntime(path, reply) }
                true
            }
            MSG_GENERATE -> {
                val prompt = msg.data.getString(KEY_PROMPT).orEmpty()
                val reply = msg.replyTo
                worker.execute { generate(prompt, reply) }
                true
            }
            MSG_BENCHMARK_10 -> {
                val reply = msg.replyTo
                worker.execute { benchmarkTen(reply) }
                true
            }
            MSG_METRICS -> {
                send(msg.replyTo, "Runtime metrics", memorySnapshot())
                true
            }
            MSG_CANCEL -> {
                cancelByProcessBoundary(msg.replyTo)
                true
            }
            MSG_CLOSE -> {
                closeByProcessBoundary(msg.replyTo)
                true
            }
            else -> false
        }
    })

    override fun onBind(intent: Intent?): IBinder = incoming.binder

    override fun onDestroy() {
        closeReflective(conversation)
        conversation = null
        closeReflective(engine)
        engine = null
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun initializeRuntime(modelPath: String, reply: Messenger?) {
        closeReflective(conversation)
        conversation = null
        closeReflective(engine)
        engine = null
        generationCount = 0
        send(reply, "Stage 1/5 ✓", "isolated :localbrain process alive • ${memorySnapshot()}")

        val model = File(modelPath)
        if (!model.isFile || model.length() <= 0L) {
            send(reply, "Runtime load failed", "model missing/empty at private path")
            return
        }
        send(reply, "Stage 2/5 ✓", "private model readable • ${model.length()} bytes")

        val result = runCatching {
            val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend")
            val cpuClass = Class.forName("com.google.ai.edge.litertlm.Backend\$CPU")
            val configClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
            val engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
            val conversationConfigClass = Class.forName("com.google.ai.edge.litertlm.ConversationConfig")
            send(reply, "Stage 3/5 ✓", "LiteRT-LM 0.15.0 classes loaded")

            val cpu = cpuClass.getConstructor().newInstance()
            val configCtor = configClass.getConstructor(
                String::class.java,
                backendClass,
                backendClass,
                backendClass,
                Integer::class.java,
                Integer::class.java,
                String::class.java
            )
            val config = configCtor.newInstance(model.absolutePath, cpu, null, null, 1024, null, cacheDir.absolutePath)
            send(reply, "Stage 4/5", "CPU config built • entering native Engine.initialize() • ${memorySnapshot()}")

            val instance = engineClass.getConstructor(configClass).newInstance(config)
            val started = System.nanoTime()
            engineClass.getMethod("initialize").invoke(instance)
            val loadMs = elapsedMs(started)
            val initialized = engineClass.getMethod("isInitialized").invoke(instance) as? Boolean ?: false
            check(initialized) { "Engine returned without initialized=true" }
            val conversationConfig = conversationConfigClass.getConstructor().newInstance()
            val convo = engineClass.getMethod("createConversation", conversationConfigClass).invoke(instance, conversationConfig)
            engine = instance
            conversation = convo
            "LiteRT-LM CPU engine initialized ✓ • load ${loadMs} ms • ${memorySnapshot()}"
        }

        result.onSuccess { send(reply, "Stage 5/5 ✓", it) }
            .onFailure { error ->
                val root = rootCause(error)
                send(reply, "Runtime load failed", "${root.javaClass.simpleName}: ${root.message.orEmpty().take(260)} • ${memorySnapshot()}")
            }
    }

    private fun generate(prompt: String, reply: Messenger?) {
        val convo = conversation
        if (convo == null) {
            send(reply, "Generation blocked", "Initialize LiteRT-LM CPU first")
            return
        }
        val clean = prompt.trim()
        if (clean.isEmpty()) {
            send(reply, "Generation blocked", "Prompt is empty")
            return
        }
        val beforeMemory = memorySnapshot()
        send(reply, "Generating…", "${clean.take(160)}\nBefore: $beforeMemory")
        val result = runCatching { generateOnce(convo, clean) }
        result.onSuccess { generated ->
            generationCount += 1
            send(
                reply,
                "Generation PASS ✓ • ${generated.totalMs} ms",
                buildString {
                    append(generated.text.take(MAX_RESPONSE_CHARS))
                    append("\n\nMetrics: chars=${generated.chars} • approxTokens≈${generated.approxTokens} • total=${generated.totalMs} ms")
                    if (generated.totalMs > 0L) {
                        append(" • chars/s=${format1(generated.chars * 1000.0 / generated.totalMs)}")
                        append(" • roughTokens/s≈${format1(generated.approxTokens * 1000.0 / generated.totalMs)}")
                    }
                    append(" • run=$generationCount")
                    append("\nAfter: ${memorySnapshot()}")
                }
            )
        }.onFailure { error ->
            val root = rootCause(error)
            send(reply, "Generation failed", "${root.javaClass.simpleName}: ${root.message.orEmpty().take(260)} • ${memorySnapshot()}")
        }
    }

    private fun benchmarkTen(reply: Messenger?) {
        val convo = conversation
        if (convo == null) {
            send(reply, "10-prompt benchmark blocked", "Initialize LiteRT-LM CPU first")
            return
        }
        send(reply, "10-prompt benchmark running…", "Sequential local CPU generation • ${memorySnapshot()}")
        val started = System.nanoTime()
        val results = mutableListOf<GenerationResult>()
        val failure = runCatching {
            BENCHMARK_PROMPTS.forEachIndexed { index, prompt ->
                send(reply, "Benchmark ${index + 1}/10…", prompt.take(150))
                val generated = generateOnce(convo, prompt)
                results += generated
                generationCount += 1
            }
        }.exceptionOrNull()

        if (failure != null) {
            val root = rootCause(failure)
            send(reply, "10-prompt benchmark failed", "completed=${results.size}/10 • ${root.javaClass.simpleName}: ${root.message.orEmpty().take(220)} • ${memorySnapshot()}")
            return
        }

        val wallMs = elapsedMs(started)
        val chars = results.sumOf { it.chars }
        val approxTokens = results.sumOf { it.approxTokens }
        val slowest = results.maxOfOrNull { it.totalMs } ?: 0L
        val fastest = results.minOfOrNull { it.totalMs } ?: 0L
        val average = if (results.isEmpty()) 0L else results.sumOf { it.totalMs } / results.size
        send(
            reply,
            "10-prompt benchmark PASS ✓",
            "runs=10 • wall=${wallMs} ms • avg=${average} ms • fastest=${fastest} ms • slowest=${slowest} ms • chars=$chars • approxTokens≈$approxTokens • totalRuns=$generationCount • ${memorySnapshot()}"
        )
    }

    private fun generateOnce(convo: Any, prompt: String): GenerationResult {
        val started = System.nanoTime()
        val message = convo.javaClass.getMethod("sendMessage", String::class.java).invoke(convo, prompt)
        val totalMs = elapsedMs(started)
        val text = extractText(message)
        check(text.isNotBlank()) { "Model returned no text" }
        val chars = text.length
        val approxTokens = max(1, chars / 4)
        return GenerationResult(text, totalMs, chars, approxTokens)
    }

    private fun cancelByProcessBoundary(reply: Messenger?) {
        send(reply, "Cancelling generation…", "Bounded cancellation uses isolated :localbrain process teardown; UI process stays alive")
        Thread {
            runCatching { Thread.sleep(CANCEL_GRACE_MS) }
            stopSelf()
            Process.killProcess(Process.myPid())
        }.apply { name = "j4-cancel-watchdog"; isDaemon = true }.start()
    }

    private fun closeByProcessBoundary(reply: Messenger?) {
        send(reply, "Closing runtime…", "Request accepted • bounded isolated-process cleanup")
        Thread {
            val closeThread = Thread {
                closeReflective(conversation)
                conversation = null
                closeReflective(engine)
                engine = null
            }.apply { name = "j4-native-close"; isDaemon = true }
            closeThread.start()
            runCatching { closeThread.join(CLOSE_GRACE_MS) }
            send(reply, "Runtime closed ✓", "Isolated :localbrain process stopping; launcher stays alive • ${memorySnapshot()}")
            runCatching { Thread.sleep(120L) }
            stopSelf()
            Process.killProcess(Process.myPid())
        }.apply { name = "j4-close-watchdog"; isDaemon = true }.start()
    }

    private fun extractText(message: Any?): String {
        if (message == null) return ""
        return runCatching {
            val contentsHolder = message.javaClass.getMethod("getContents").invoke(message)
            val list = contentsHolder.javaClass.getMethod("getContents").invoke(contentsHolder) as? Iterable<*>
            buildString {
                list?.forEach { content ->
                    if (content == null) return@forEach
                    val getText = content.javaClass.methods.firstOrNull { it.name == "getText" && it.parameterCount == 0 }
                    val part = getText?.invoke(content) as? String
                    if (!part.isNullOrBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(part.trim())
                    }
                }
            }
        }.getOrDefault("").ifBlank { message.toString() }
    }

    private fun memorySnapshot(): String {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        val javaUsed = runtime.totalMemory() - runtime.freeMemory()
        val nativeUsed = Debug.getNativeHeapAllocatedSize()
        return "PSS ${formatMb(info.totalPss * 1024L)} • Java ${formatMb(javaUsed)} • Native ${formatMb(nativeUsed)}"
    }

    private fun formatMb(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    private fun format1(value: Double): String = String.format(Locale.US, "%.1f", value)
    private fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L

    private fun closeReflective(target: Any?) {
        if (target == null) return
        runCatching {
            val close = target.javaClass.methods.firstOrNull { it.name == "close" && it.parameterCount == 0 }
            close?.invoke(target)
        }
    }

    private fun rootCause(error: Throwable): Throwable =
        generateSequence(error as Throwable?) { it.cause }.lastOrNull() ?: error

    private fun send(reply: Messenger?, stage: String, detail: String) {
        if (reply == null) return
        val message = Message.obtain(null, MSG_STATUS).apply {
            data = Bundle().apply {
                putString(KEY_STAGE, stage)
                putString(KEY_DETAIL, detail.take(MAX_DETAIL_CHARS))
            }
        }
        runCatching { reply.send(message) }
    }

    private data class GenerationResult(
        val text: String,
        val totalMs: Long,
        val chars: Int,
        val approxTokens: Int
    )

    companion object {
        const val MSG_LOAD = 1
        const val MSG_CLOSE = 2
        const val MSG_GENERATE = 3
        const val MSG_CANCEL = 4
        const val MSG_METRICS = 5
        const val MSG_BENCHMARK_10 = 6
        const val MSG_STATUS = 100
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_PROMPT = "prompt"
        const val KEY_STAGE = "stage"
        const val KEY_DETAIL = "detail"
        private const val CLOSE_GRACE_MS = 2_000L
        private const val CANCEL_GRACE_MS = 120L
        private const val MAX_DETAIL_CHARS = 2_400
        private const val MAX_RESPONSE_CHARS = 1_500

        private val BENCHMARK_PROMPTS = listOf(
            "ऑफलाइन एआई क्या होता है? इसे आसान हिंदी में ठीक तीन छोटे वाक्यों में समझाओ।",
            "Offline AI kya hota hai? Simple Hinglish mein exactly teen short lines mein samjhao.",
            "Explain offline AI in exactly three short sentences.",
            "Kal subah dawa yaad dilane ke request ko confirm karne ke liye ek short line banao. Koi action mat karo.",
            "Agar tumhe kisi fact ka bharosa na ho to tum kya kahogi? Ek short Hindi line mein jawab do.",
            "फोन में local AI use karne ke do privacy benefits simple Hinglish me batao.",
            "एक उपयोगी AI assistant को user की permission का सम्मान क्यों करना चाहिए? दो छोटे वाक्य।",
            "Give two concise reasons an offline assistant should keep a deterministic fallback.",
            "User बोले 'Mummy ko message bhejo' लेकिन recipient ambiguous हो, assistant ko kya karna chahiye? Sirf ek safe response likho.",
            "Summarize in one sentence: local AI can help privately, but device actions still need trusted Android APIs and confirmations."
        )
    }
}
