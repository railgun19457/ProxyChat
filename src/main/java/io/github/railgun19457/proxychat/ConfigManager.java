package io.github.railgun19457.proxychat;

import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.tomlj.Toml;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.toml";
    private static final String MESSAGE_FILE_NAME = "message.toml";

    private final Logger logger;
    private final Path dataDirectory;
    private final MiniMessage miniMessage;

    private volatile RuntimeConfig runtimeConfig;
    private volatile MessageConfig messageConfig;

    public ConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public synchronized void initialize() throws IOException {
        Files.createDirectories(dataDirectory);
        ensureDefaultFile(CONFIG_FILE_NAME);
        ensureDefaultFile(MESSAGE_FILE_NAME);
        if (!reload()) {
            throw new IOException("Unable to load config.toml or message.toml");
        }
    }

    public synchronized boolean reload() {
        try {
            RuntimeConfig newRuntimeConfig = parseRuntimeConfig(dataDirectory.resolve(CONFIG_FILE_NAME));
            MessageConfig newMessageConfig = parseMessageConfig(dataDirectory.resolve(MESSAGE_FILE_NAME));
            this.runtimeConfig = newRuntimeConfig;
            this.messageConfig = newMessageConfig;
            logger.info("[ProxyChat] Configuration reloaded successfully.");
            return true;
        } catch (Exception ex) {
            logger.error("[ProxyChat] Failed to reload configuration. Keeping previous snapshot.", ex);
            return false;
        }
    }

    public RuntimeConfig runtime() {
        return Objects.requireNonNull(runtimeConfig, "Runtime config has not been initialized");
    }

    public MessageConfig messages() {
        return Objects.requireNonNull(messageConfig, "Message config has not been initialized");
    }

    public String resolveServerName(String serverName) {
        return runtime().resolveServerAlias(serverName);
    }

    public Component render(String template, Map<String, String> placeholders) {
        String resolved = applyPlaceholders(template, placeholders);
        return miniMessage.deserialize(resolved);
    }

    public String applyPlaceholders(String template, Map<String, String> placeholders) {
        String content = template == null ? "" : template;
        if (placeholders == null || placeholders.isEmpty()) {
            return content;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : miniMessage.escapeTags(entry.getValue());
            content = content.replace(key, value);
        }
        return content;
    }

    public void debug(String message, Object... args) {
        RuntimeConfig snapshot = runtimeConfig;
        if (snapshot != null && snapshot.loggingDebug()) {
            logger.info("[ProxyChat/Debug] " + message, args);
        }
    }

    private void ensureDefaultFile(String resourceName) throws IOException {
        Path outputPath = dataDirectory.resolve(resourceName);
        if (Files.exists(outputPath)) {
            return;
        }

        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing default resource: " + resourceName);
            }
            Files.copy(input, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private RuntimeConfig parseRuntimeConfig(Path path) throws IOException {
        TomlParseResult toml = parseToml(path);

        Map<String, String> aliasMap = new LinkedHashMap<>();
        TomlTable table = toml.getTable("server-alias.map");
        if (table != null) {
            for (String key : table.keySet()) {
                Object value = table.get(key);
                if (value instanceof String str) {
                    aliasMap.put(key, str);
                }
            }
        }

        return new RuntimeConfig(
                intValue(toml, "meta.config-version", 1),
                boolValue(toml, "update.check", true),
                boolValue(toml, "update.notify-admins", true),
                boolValue(toml, "logging.print-chat", true),
                boolValue(toml, "logging.debug", false),
                boolValue(toml, "chat.enabled", true),
                stringValue(toml, "chat.format", "<white>{player}</white>: <white>{message}</white>"),
                boolValue(toml, "server-alias.enabled", false),
                aliasMap,
                boolValue(toml, "join-leave.enabled", true),
                stringValue(toml, "join-leave.first-join", "<green>+ <white>{player}</white> <gray>joined <gold>{server}</gold></gray>"),
                stringValue(toml, "join-leave.switch-server", "<yellow>* <white>{player}</white> <gray>moved <gold>{from}</gold> -> <gold>{to}</gold></gray>"),
                stringValue(toml, "join-leave.leave", "<red>- <white>{player}</white> <gray>left the network</gray>"),
                boolValue(toml, "at.enabled", true),
                stringValue(toml, "at.notify-title", "<gold>@ Mention</gold>"),
                stringValue(toml, "at.notify-message", "<yellow>{player}</yellow> <gray>mentioned you:</gray> <white>{message}</white>"),
                stringValue(toml, "at.notify-action-bar", "<gold>@{player}</gold> <white>{message}</white>"),
                boolValue(toml, "at.sound-enabled", true)
        );
    }

    private MessageConfig parseMessageConfig(Path path) throws IOException {
        TomlParseResult toml = parseToml(path);

        return new MessageConfig(
                stringValue(toml, "messages.reload-success", "<green>ProxyChat reloaded.</green>"),
                stringValue(toml, "messages.reload-failed", "<red>Reload failed. Check console logs.</red>"),
                stringValue(toml, "messages.reload-usage", "<yellow>Usage: /proxychat reload or /pc reload</yellow>"),
                stringValue(toml, "messages.no-permission", "<red>You do not have permission.</red>"),
                stringValue(toml, "messages.at-usage", "<yellow>Usage: /at <player> <message></yellow>"),
                stringValue(toml, "messages.at-disabled", "<red>AT feature is disabled.</red>"),
                stringValue(toml, "messages.player-not-found", "<red>Player not found: {player}</red>"),
                stringValue(toml, "messages.update-available", "<yellow>New version available: <white>{current}</white> -> <green>{latest}</green></yellow>"),
                stringValue(toml, "messages.update-no-need", "<gray>Already up to date: {current}</gray>"),
                stringValue(toml, "messages.at-sent", "<gray>Mention sent to <yellow>{target}</yellow>.</gray>")
        );
    }

    private TomlParseResult parseToml(Path path) throws IOException {
        TomlParseResult parseResult = Toml.parse(Files.readString(path, StandardCharsets.UTF_8));
        if (!parseResult.hasErrors()) {
            return parseResult;
        }

        StringBuilder errorBuilder = new StringBuilder();
        for (TomlParseError error : parseResult.errors()) {
            errorBuilder.append("[")
                    .append(error.position())
                    .append("] ")
                    .append(error.getMessage())
                    .append(System.lineSeparator());
        }

        throw new IOException("Invalid TOML in " + path.getFileName() + ": " + errorBuilder);
    }

    private static boolean boolValue(TomlParseResult toml, String key, boolean defaultValue) {
        Boolean value = toml.getBoolean(key);
        return value == null ? defaultValue : value;
    }

    private static int intValue(TomlParseResult toml, String key, int defaultValue) {
        Long value = toml.getLong(key);
        return value == null ? defaultValue : value.intValue();
    }

    private static String stringValue(TomlParseResult toml, String key, String defaultValue) {
        String value = toml.getString(key);
        return value == null ? defaultValue : value;
    }
}
