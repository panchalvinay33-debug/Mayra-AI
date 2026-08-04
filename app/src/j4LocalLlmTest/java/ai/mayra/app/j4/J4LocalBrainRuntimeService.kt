package ai.mayra.app.j4

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import java.io.File
import java.util.concurrent.Executors

/**
 * Crash-isolated J4 LiteRT-LM runtime probe.
 *
 * The launcher never directly touches LiteRT-LM classes. This service runs in :localbrain and
 * uses reflection so the existing Kotlin 2.0 / Java 17 Mayra source does not compile against
 * LiteRT-LM 0.15.0's Kotlin 2.2 / Java-21 classfiles. First gate is initialize + close only.
 */
class J4LocalBrainRuntimeService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var engine: AutoCloseable? = null

    private val incoming = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MSG_LOAD -> {
                val path = msg.data.getString(KEY_MODEL_PATH).orEmpty()
                val reply = msg.replyTo
                worker.execute { initializeRuntime(path, reply) }
                true
            }
            MSG_CLOSE -> {
                worker.execute {
                    runCatching { engine?.close() }
                    engine = null
                    send(reply = msg.replyTo, stage = "Runtime closed ✓", detail = "LiteRT-LM engine released")
                }
                true
            }
            else -> false
        }
    })

    override fun onBind(intent: Intent?): IBinder = incoming.binder

    override fun onDestroy() {
        runCatching { engine?.close() }
        engine = null
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun initializeRuntime(modelPath: String, reply: Messenger?) {
        runCatching { engine?.close() }
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
            val config = configCtor.newInstance(model.absolutePath, cpu, null, null, null, null, cacheDir.absolutePath)
            send(reply, "Stage 4/5", "CPU config built • entering native Engine.initialize()")

            val instance = engineClass.getConstructor(configClass).newInstance(config)
            engineClass.getMethod("initialize").invoke(instance)
            val initialized = engineClass.getMethod("isInitialized").invoke(instance) as? Boolean ?: false
            check(initialized) { "Engine returned without initialized=true" }
            engine = instance as AutoCloseable
            "LiteRT-LM CPU engine initialized ✓"
        }

        result.onSuccess { send(reply, "Stage 5/5 ✓", it) }
            .onFailure { error ->
                val root = generateSequence(error as Throwable?) { it.cause }.lastOrNull() ?: error
                send(reply, "Runtime load failed", "${root.javaClass.simpleName}: ${root.message.orEmpty().take(260)}")
            }
    }

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

    companion object {
        const val MSG_LOAD = 1
        const val MSG_CLOSE = 2
        const val MSG_STATUS = 100
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_STAGE = "stage"
        const val KEY_DETAIL = "detail"
    }
}
