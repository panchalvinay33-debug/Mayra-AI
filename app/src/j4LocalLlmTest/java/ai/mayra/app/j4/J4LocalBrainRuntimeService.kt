package ai.mayra.app.j4

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import java.io.File
import java.util.concurrent.Executors

/**
 * Crash-isolated J4 LiteRT-LM runtime probe.
 *
 * The launcher never directly touches LiteRT-LM classes. This service runs in :localbrain and
 * uses reflection so the existing Kotlin 2.0 / Java 17 Mayra source does not compile against
 * LiteRT-LM 0.15.0's newer Kotlin metadata / Java-21 classfiles.
 *
 * Motorola evidence proved CPU Engine.initialize() reaches Stage 5/5. This gate now adds fixed
 * text generation plus a bounded close path. If native close blocks, Android process teardown is
 * the final cleanup boundary so the launcher remains alive and all native memory is reclaimed.
 */
class J4LocalBrainRuntimeService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var engine: Any? = null
    private var conversation: Any? = null

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
            MSG_CLOSE -> {
                val reply = msg.replyTo
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
                    send(reply, "Runtime closed ✓", "Isolated :localbrain process stopping; launcher stays alive")
                    runCatching { Thread.sleep(120L) }
                    stopSelf()
                    Process.killProcess(Process.myPid())
                }.apply { name = "j4-close-watchdog"; isDaemon = true }.start()
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
        send(reply, "Stage 1/5 ✓", "isolated :localbrain process alive")

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
            send(reply, "Stage 4/5", "CPU config built • entering native Engine.initialize()")

            val instance = engineClass.getConstructor(configClass).newInstance(config)
            val started = System.nanoTime()
            engineClass.getMethod("initialize").invoke(instance)
            val loadMs = (System.nanoTime() - started) / 1_000_000L
            val initialized = engineClass.getMethod("isInitialized").invoke(instance) as? Boolean ?: false
            check(initialized) { "Engine returned without initialized=true" }
            val conversationConfig = conversationConfigClass.getConstructor().newInstance()
            val convo = engineClass.getMethod("createConversation", conversationConfigClass).invoke(instance, conversationConfig)
            engine = instance
            conversation = convo
            "LiteRT-LM CPU engine initialized ✓ • load ${loadMs} ms"
        }

        result.onSuccess { send(reply, "Stage 5/5 ✓", it) }
            .onFailure { error ->
                val root = rootCause(error)
                send(reply, "Runtime load failed", "${root.javaClass.simpleName}: ${root.message.orEmpty().take(260)}")
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
        send(reply, "Generating…", clean.take(120))
        val result = runCatching {
            val started = System.nanoTime()
            val message = convo.javaClass.getMethod("sendMessage", String::class.java).invoke(convo, clean)
            val totalMs = (System.nanoTime() - started) / 1_000_000L
            val text = extractText(message)
            check(text.isNotBlank()) { "Model returned no text" }
            GenerationResult(text, totalMs)
        }
        result.onSuccess { generated ->
            send(reply, "Generation PASS ✓ • ${generated.totalMs} ms", generated.text.take(MAX_DETAIL_CHARS))
        }.onFailure { error ->
            val root = rootCause(error)
            send(reply, "Generation failed", "${root.javaClass.simpleName}: ${root.message.orEmpty().take(260)}")
        }
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
                putString(KEY_DETAIL, detail)
            }
        }
        runCatching { reply.send(message) }
    }

    private data class GenerationResult(val text: String, val totalMs: Long)

    companion object {
        const val MSG_LOAD = 1
        const val MSG_CLOSE = 2
        const val MSG_GENERATE = 3
        const val MSG_STATUS = 100
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_PROMPT = "prompt"
        const val KEY_STAGE = "stage"
        const val KEY_DETAIL = "detail"
        private const val CLOSE_GRACE_MS = 2_000L
        private const val MAX_DETAIL_CHARS = 900
    }
}
