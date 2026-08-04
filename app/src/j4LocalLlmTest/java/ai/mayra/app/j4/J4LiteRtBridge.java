package ai.mayra.app.j4;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;

import java.io.File;

/**
 * Java-only J4 bridge around LiteRT-LM 0.15.0.
 *
 * LiteRT-LM 0.15.0 publishes Java 21 class files and Kotlin 2.2 metadata while the main Mayra
 * application intentionally remains on the proven Java 17 / Kotlin 2.0 toolchain. Keeping every
 * LiteRT type behind this engineering-only Java bridge lets the J4 workflow use JDK 21 without
 * forcing the normal J1/J2/J3/final-app source to understand newer Kotlin metadata.
 */
public final class J4LiteRtBridge implements AutoCloseable {
    private Engine engine;
    private Conversation conversation;

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
        EngineConfig config = new EngineConfig(
                model.getAbsolutePath(),
                new Backend.CPU(),
                null,
                null,
                1024,
                null,
                cache.getAbsolutePath()
        );
        Engine next = new Engine(config);
        next.initialize();
        Conversation nextConversation = next.createConversation(new ConversationConfig());
        engine = next;
        conversation = nextConversation;
        long loadMs = (System.nanoTime() - started) / 1_000_000L;
        return new LoadResult(loadMs, model.length());
    }

    public synchronized GenerationResult generate(String prompt) {
        Conversation current = conversation;
        if (current == null) throw new IllegalStateException("LiteRT-LM engine is not loaded");
        String clean = prompt == null ? "" : prompt.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Prompt is empty");

        long started = System.nanoTime();
        Message message = current.sendMessage(clean);
        long totalMs = (System.nanoTime() - started) / 1_000_000L;
        StringBuilder text = new StringBuilder();
        for (Content content : message.getContents().getContents()) {
            if (content instanceof Content.Text) {
                String part = ((Content.Text) content).getText();
                if (part != null && !part.isBlank()) {
                    if (text.length() > 0) text.append('\n');
                    text.append(part.trim());
                }
            }
        }
        if (text.length() == 0) text.append(message.toString());
        return new GenerationResult(text.toString(), totalMs);
    }

    public synchronized boolean isLoaded() {
        return engine != null && engine.isInitialized() && conversation != null;
    }

    @Override
    public synchronized void close() {
        if (conversation != null) {
            try { conversation.close(); } catch (Throwable ignored) { }
            conversation = null;
        }
        if (engine != null) {
            try { engine.close(); } catch (Throwable ignored) { }
            engine = null;
        }
    }

    public record LoadResult(long loadMs, long modelBytes) { }
    public record GenerationResult(String text, long totalMs) { }
}
