package io.github.railgun19457.proxychat.service;

import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.util.Locale;

public final class PluginLogger {
    public enum Topic {
        LIFECYCLE,
        CONFIG,
        CHAT,
        COMMAND,
        UPDATE,
        SECURITY
    }

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final Logger delegate;

    private volatile boolean debugEnabled;
    private volatile boolean printChatEnabled = true;

    public PluginLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public void applyRuntime(RuntimeConfig runtime) {
        if (runtime == null) {
            this.debugEnabled = false;
            this.printChatEnabled = true;
            return;
        }

        this.debugEnabled = runtime.loggingDebug();
        this.printChatEnabled = runtime.loggingPrintChat();
    }

    public void info(Topic topic, String message, Object... args) {
        delegate.info(prefix(topic) + message, args);
    }

    public void warn(Topic topic, String message, Object... args) {
        delegate.warn(prefix(topic) + message, args);
    }

    public void error(Topic topic, String message, Object... args) {
        delegate.error(prefix(topic) + message, args);
    }

    public void error(Topic topic, String message, Throwable throwable) {
        delegate.error(prefix(topic) + message, throwable);
    }

    public void debug(Topic topic, String message, Object... args) {
        if (!debugEnabled) {
            return;
        }
        delegate.info(prefix(topic) + "[DEBUG] " + message, args);
    }

    public void printChat(Component message) {
        if (!printChatEnabled || message == null) {
            return;
        }

        // Keep chat logs single-line to avoid log forging with embedded newlines.
        String singleLine = PLAIN_TEXT.serialize(message).replaceAll("\\R", "\\\\n");
        delegate.info(prefix(Topic.CHAT) + "{}", singleLine);
    }

    private static String prefix(Topic topic) {
        return "[ProxyChat][" + topic.name().toLowerCase(Locale.ROOT) + "] ";
    }
}