package ai.mayra.app.j4;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Reflection-only J4 bridge around LiteRT-LM 0.15.0.
 *
 * LiteRT-LM 0.15.0 ships Java 21 class files plus Kotlin 2.2/2.3 metadata while the proven Mayra
 * application stays on Java 17 / Kotlin 2.0. Keeping LiteRT on the J4 runtime-only classpath prevents
 * Kotlin/KSP from parsing incompatible metadata. The isolated :localbrain process touches LiteRT
 * only after the owner explicitly starts the runtime benchmark.
 */
public final class J4LiteRtBridge implements AutoCloseable {
    private Object engine;
    private Object conversation;

    public synchronized LoadResult load(String modelPath, String cacheDir) {
        close();
        File model = new File(modelPath);
        if (!model.isFile() || model.length() <= 0L) {
            throw new IllegalArgumentException("Verified model file is missing or empty");
        }
        File cache = new File(cacheDir);
        if (!cache.exists() && !cache.mkdirs()) {
            throw new IllegalStateException("Could not create LiteRT cache directory");
        }

        long started = System.nanoTime();
        try {
            ClassLoader loader = J4LiteRtBridge.class.getClassLoader();
            Class<?> backendClass = Class.forName("com.google.ai.edge.litertlm.Backend", true, loader);
            Class<?> cpuClass = Class.forName("com.google.ai.edge.litertlm.Backend$CPU", true, loader);
            Class<?> engineConfigClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig", true, loader);
            Class<?> engineClass = Class.forName("com.google.ai.edge.litertlm.Engine", true, loader);
            Class<?> conversationConfigClass = Class.forName("com.google.ai.edge.litertlm.ConversationConfig", true, loader);

            Object cpu = cpuClass.getConstructor().newInstance();
            Constructor<?> configCtor = engineConfigClass.getConstructor(
                    String.class,
                    backendClass,
                    backendClass,
                    backendClass,
                    Integer.class,
                    Integer.class,
                    String.class
            );
            Object config = configCtor.newInstance(
                    model.getAbsolutePath(),
                    cpu,
                    null,
                    null,
                    Integer.valueOf(1024),
                    null,
                    cache.getAbsolutePath()
            );

            Object nextEngine = engineClass.getConstructor(engineConfigClass).newInstance(config);
            engineClass.getMethod("initialize").invoke(nextEngine);
            Object conversationConfig = conversationConfigClass.getConstructor().newInstance();
            Object nextConversation = engineClass
                    .getMethod("createConversation", conversationConfigClass)
                    .invoke(nextEngine, conversationConfig);

            engine = nextEngine;
            conversation = nextConversation;
            long loadMs = (System.nanoTime() - started) / 1_000_000L;
            return new LoadResult(loadMs, model.length());
        } catch (Throwable error) {
            close();
            throw reflectionFailure("LiteRT-LM load failed", error);
        }
    }

    /**
     * Generation stays available for the next benchmark gate, but the current J4 UI should first
     * prove load/close on Motorola before enabling free-form conversation.
     */
    public synchronized GenerationResult generate(String prompt) {
        Object current = conversation;
        if (current == null) throw new IllegalStateException("LiteRT-LM engine is not loaded");
        String clean = prompt == null ? "" : prompt.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Prompt is empty");

        long started = System.nanoTime();
        try {
            ClassLoader loader = J4LiteRtBridge.class.getClassLoader();
            Class<?> conversationClass = Class.forName("com.google.ai.edge.litertlm.Conversation", true, loader);
            Class<?> repetitionPenaltyClass = Class.forName("com.google.ai.edge.litertlm.RepetitionPenaltyConfig", true, loader);
            Class<?> noRepeatNgramClass = Class.forName("com.google.ai.edge.litertlm.NoRepeatNgramConfig", true, loader);
            Class<?> suppressTokensClass = Class.forName("com.google.ai.edge.litertlm.SuppressTokensConfig", true, loader);
            Class<?> thinkingClass = Class.forName("com.google.ai.edge.litertlm.ThinkingConfig", true, loader);
            Class<?> responseFormatClass = Class.forName("com.google.ai.edge.litertlm.ResponseFormat", true, loader);

            Method send = conversationClass.getMethod(
                    "sendMessage",
                    String.class,
                    java.util.Map.class,
                    repetitionPenaltyClass,
                    noRepeatNgramClass,
                    suppressTokensClass,
                    Integer.class,
                    thinkingClass,
                    responseFormatClass
            );
            Object message = send.invoke(
                    current,
                    clean,
                    Collections.emptyMap(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            long totalMs = (System.nanoTime() - started) / 1_000_000L;
            String text = extractText(message);
            return new GenerationResult(text, totalMs);
        } catch (Throwable error) {
            throw reflectionFailure("LiteRT-LM generation failed", error);
        }
    }

    public synchronized boolean isLoaded() {
        Object currentEngine = engine;
        if (currentEngine == null || conversation == null) return false;
        try {
            Object result = currentEngine.getClass().getMethod("isInitialized").invoke(currentEngine);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        closeReflective(conversation);
        conversation = null;
        closeReflective(engine);
        engine = null;
    }

    private static String extractText(Object message) throws Exception {
        if (message == null) return "";
        Object contents = message.getClass().getMethod("getContents").invoke(message);
        if (contents == null) return message.toString();
        Object listValue = contents.getClass().getMethod("getContents").invoke(contents);
        if (!(listValue instanceof List<?>)) return message.toString();

        StringBuilder text = new StringBuilder();
        for (Object content : (List<?>) listValue) {
            if (content == null) continue;
            if (!"com.google.ai.edge.litertlm.Content$Text".equals(content.getClass().getName())) continue;
            Object value = content.getClass().getMethod("getText").invoke(content);
            if (value instanceof String) {
                String part = ((String) value).trim();
                if (!part.isEmpty()) {
                    if (text.length() > 0) text.append('\n');
                    text.append(part);
                }
            }
        }
        return text.length() == 0 ? message.toString() : text.toString();
    }

    private static void closeReflective(Object value) {
        if (value == null) return;
        try {
            value.getClass().getMethod("close").invoke(value);
        } catch (Throwable ignored) {
            // Engineering harness cleanup is best-effort; isolated process remains the crash boundary.
        }
    }

    private static RuntimeException reflectionFailure(String prefix, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String detail = cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
        return new IllegalStateException(prefix + " • " + detail, cause);
    }

    public static final class LoadResult {
        private final long loadMs;
        private final long modelBytes;

        public LoadResult(long loadMs, long modelBytes) {
            this.loadMs = loadMs;
            this.modelBytes = modelBytes;
        }

        public long loadMs() { return loadMs; }
        public long modelBytes() { return modelBytes; }
    }

    public static final class GenerationResult {
        private final String text;
        private final long totalMs;

        public GenerationResult(String text, long totalMs) {
            this.text = text;
            this.totalMs = totalMs;
        }

        public String text() { return text; }
        public long totalMs() { return totalMs; }
    }
}
