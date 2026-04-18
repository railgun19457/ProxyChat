package io.github.railgun19457.proxychat;

import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import io.github.railgun19457.proxychat.service.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.toml";
    private static final String MESSAGE_FILE_NAME = "message.toml";

    private final PluginLogger pluginLogger;
    private final Path dataDirectory;
    private final MiniMessage miniMessage;

    private volatile RuntimeConfig runtimeConfig;
    private volatile MessageConfig messageConfig;

    public ConfigManager(PluginLogger pluginLogger, Path dataDirectory) {
        this.pluginLogger = pluginLogger;
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
            pluginLogger.applyRuntime(newRuntimeConfig);
            pluginLogger.info(
                    PluginLogger.Topic.CONFIG,
                    "Configuration reloaded successfully. config-version={}",
                    newRuntimeConfig.configVersion()
            );
            return true;
        } catch (Exception ex) {
            pluginLogger.error(PluginLogger.Topic.CONFIG, "Failed to reload configuration. Keeping previous snapshot.", ex);
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
        return render(template, placeholders, Map.of());
    }

    public Component render(String template, Map<String, String> placeholders, Map<String, Component> componentPlaceholders) {
        Map<String, String> safePlaceholders = placeholders == null ? Map.of() : placeholders;
        Map<String, Component> safeComponentPlaceholders = componentPlaceholders == null ? Map.of() : componentPlaceholders;
        String content = normalizePlaceholderSyntax(template == null ? "" : template, safePlaceholders, safeComponentPlaceholders);

        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : safePlaceholders.entrySet()) {
            resolvers.add(Placeholder.unparsed(entry.getKey(), entry.getValue() == null ? "" : entry.getValue()));
        }
        for (Map.Entry<String, Component> entry : safeComponentPlaceholders.entrySet()) {
            resolvers.add(Placeholder.component(entry.getKey(), entry.getValue() == null ? Component.empty() : entry.getValue()));
        }

        try {
            if (resolvers.isEmpty()) {
                return miniMessage.deserialize(content);
            }
            return miniMessage.deserialize(content, TagResolver.resolver(resolvers));
        } catch (Exception ex) {
            pluginLogger.warn(
                    PluginLogger.Topic.CONFIG,
                    "Failed to parse MiniMessage template. Falling back to plain text. template={}",
                    content,
                    ex
            );
            return Component.text(renderPlainFallback(content, safePlaceholders, safeComponentPlaceholders));
        }
    }

    public void debug(String message, Object... args) {
        pluginLogger.debug(PluginLogger.Topic.CONFIG, message, args);
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
        Map<String, Component> aliasComponentMap = new LinkedHashMap<>();
        TomlTable table = toml.getTable("server-alias.map");
        if (table != null) {
            for (String key : table.keySet()) {
                Object value = table.get(key);
                if (value instanceof String str) {
                    String normalizedKey = key.toLowerCase(Locale.ROOT);
                    aliasMap.put(normalizedKey, str);
                    aliasComponentMap.put(normalizedKey, parseAliasComponent(key, str));
                }
            }
        }

        Map<String, Boolean> chatRoutingSendMap = parseBooleanTable(toml.getTable("chat-routing.send"));
        Map<String, Boolean> chatRoutingReceiveMap = parseBooleanTable(toml.getTable("chat-routing.receive"));
        List<Pattern> chatFilterRules = parseRegexRules(toml.getArray("chat-filter.rules"));

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
                aliasComponentMap,
                boolValue(toml, "chat-routing.enabled", false),
                boolValue(toml, "chat-routing.default-send", true),
                boolValue(toml, "chat-routing.default-receive", true),
                chatRoutingSendMap,
                chatRoutingReceiveMap,
                boolValue(toml, "chat-filter.enabled", false),
                chatFilterRules,
                boolValue(toml, "join-leave.enabled", true),
                stringValue(toml, "join-leave.first-join", "<green>+ <white>{player}</white> <gray>joined <gold>{server}</gold></gray>"),
                stringValue(toml, "join-leave.switch-server", "<yellow>* <white>{player}</white> <gray>moved <gold>{from}</gold> -> <gold>{to}</gold></gray>"),
                stringValue(toml, "join-leave.leave", "<red>- <white>{player}</white> <gray>left the network</gray>"),
                boolValue(toml, "at.enabled", true),
                boolValue(toml, "at.notify-title-enabled", true),
                boolValue(toml, "at.notify-message-enabled", true),
                boolValue(toml, "at.notify-action-bar-enabled", true),
                stringValue(toml, "at.notify-title", "<gold>@ Mention</gold>"),
                stringValue(toml, "at.notify-message", "<yellow>{player}</yellow> <gray>mentioned you:</gray> <white>{message}</white>"),
                stringValue(toml, "at.notify-action-bar", "<gold>@{player}</gold> <white>{message}</white>")
        );
    }

    private MessageConfig parseMessageConfig(Path path) throws IOException {
        TomlParseResult toml = parseToml(path);

        return new MessageConfig(
                stringValue(toml, "messages.reload-success", "<green>ProxyChat reloaded.</green>"),
                stringValue(toml, "messages.reload-failed", "<red>Reload failed. Check console logs.</red>"),
                stringValue(toml, "messages.reload-usage", "<yellow>Usage: /proxychat reload or /pc reload</yellow>"),
                stringValue(toml, "messages.no-permission", "<red>You do not have permission.</red>"),
                stringValue(toml, "messages.at-usage", "<yellow>Usage: /at [player] [optional message]</yellow>"),
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

    private Component parseAliasComponent(String aliasKey, String rawAlias) {
        if (rawAlias == null || rawAlias.isBlank()) {
            return Component.text(aliasKey);
        }
        try {
            return miniMessage.deserialize(rawAlias);
        } catch (Exception ex) {
            pluginLogger.warn(
                    PluginLogger.Topic.CONFIG,
                    "Invalid MiniMessage in server-alias.map.{}: {}",
                    aliasKey,
                    ex.getMessage()
            );
            return Component.text(rawAlias);
        }
    }

    private Map<String, Boolean> parseBooleanTable(TomlTable table) {
        Map<String, Boolean> output = new LinkedHashMap<>();
        if (table == null) {
            return output;
        }

        for (String key : table.keySet()) {
            Object value = table.get(key);
            if (value instanceof Boolean boolValue) {
                output.put(key.toLowerCase(Locale.ROOT), boolValue);
            }
        }
        return output;
    }

    private List<Pattern> parseRegexRules(TomlArray ruleArray) {
        List<Pattern> output = new ArrayList<>();
        if (ruleArray == null) {
            return output;
        }

        for (int i = 0; i < ruleArray.size(); i++) {
            Object value = ruleArray.get(i);
            if (!(value instanceof String regex) || regex.isBlank()) {
                continue;
            }

            try {
                output.add(Pattern.compile(regex));
            } catch (PatternSyntaxException ex) {
                pluginLogger.warn(PluginLogger.Topic.CONFIG, "Invalid chat filter regex at index {}: {}", i, ex.getMessage());
            }
        }
        return output;
    }

    private static String normalizePlaceholderSyntax(
            String template,
            Map<String, String> placeholders,
            Map<String, Component> componentPlaceholders
    ) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(placeholders.keySet());
        keys.addAll(componentPlaceholders.keySet());

        String output = template;
        for (String key : keys) {
            output = output.replace("{" + key + "}", "<" + key + ">");
        }
        return output;
    }

    private String renderPlainFallback(
            String template,
            Map<String, String> placeholders,
            Map<String, Component> componentPlaceholders
    ) {
        String output = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            output = output.replace("{" + entry.getKey() + "}", value)
                    .replace("<" + entry.getKey() + ">", value);
        }
        for (Map.Entry<String, Component> entry : componentPlaceholders.entrySet()) {
            String value = entry.getValue() == null
                    ? ""
                    : PlainTextComponentSerializer.plainText().serialize(entry.getValue());
            output = output.replace("{" + entry.getKey() + "}", value)
                    .replace("<" + entry.getKey() + ">", value);
        }
        return miniMessage.stripTags(output);
    }
}
