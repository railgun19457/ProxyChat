package io.github.railgun19457.proxychat.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.Permissions;
import io.github.railgun19457.proxychat.ConfigManager;
import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class UpdateChecker {
    private static final String RELEASE_API_URL = "https://api.github.com/repos/railgun19457/ProxyChat/releases/latest";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Object pluginInstance;
    private final ProxyServer proxyServer;
    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final String currentVersion;

    public UpdateChecker(
            Object pluginInstance,
            ProxyServer proxyServer,
            PluginLogger pluginLogger,
            ConfigManager configManager,
            String currentVersion
    ) {
        this.pluginInstance = Objects.requireNonNull(pluginInstance, "pluginInstance");
        this.proxyServer = proxyServer;
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.currentVersion = currentVersion;
    }

    public void checkAsync() {
        proxyServer.getScheduler()
                .buildTask(pluginInstance, this::checkNow)
                .delay(5, TimeUnit.SECONDS)
                .schedule();
        pluginLogger.debug(PluginLogger.Topic.UPDATE, "Scheduled update check task via Velocity scheduler.");
    }

    private void checkNow() {
        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.updateCheck()) {
            pluginLogger.debug(PluginLogger.Topic.UPDATE, "Skipped update check because update.check=false.");
            return;
        }

        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null || latestVersion.isBlank()) {
                pluginLogger.debug(PluginLogger.Topic.UPDATE, "Update check response did not contain a tag name.");
                return;
            }

            String currentNormalized = normalizeVersion(currentVersion);
            String latestNormalized = normalizeVersion(latestVersion);

            if (compareVersions(latestNormalized, currentNormalized) <= 0) {
                MessageConfig messages = configManager.messages();
                Component noNeedMessage = configManager.render(messages.updateNoNeed(), Map.of(
                    "current", currentVersion,
                    "latest", latestVersion
                ));
                pluginLogger.debug(PluginLogger.Topic.UPDATE, "{}", PlainTextComponentSerializer.plainText().serialize(noNeedMessage));
                return;
            }

            pluginLogger.info(PluginLogger.Topic.UPDATE, "Update available: {} -> {}", currentVersion, latestVersion);
            MessageConfig messages = configManager.messages();
            Component updateMessage = configManager.render(messages.updateAvailable(), Map.of(
                    "current", currentVersion,
                    "latest", latestVersion
            ));

            if (runtime.updateNotifyAdmins()) {
                int notified = 0;
                for (Player player : proxyServer.getAllPlayers()) {
                    if (player.hasPermission(Permissions.UPDATE_NOTIFY)) {
                        player.sendMessage(updateMessage);
                        notified++;
                    }
                }
                pluginLogger.debug(PluginLogger.Topic.UPDATE, "Sent update notification to {} online admins.", notified);
            }
        } catch (Exception ex) {
            pluginLogger.warn(PluginLogger.Topic.UPDATE, "Update check failed: {}", ex.getMessage());
            pluginLogger.debug(PluginLogger.Topic.UPDATE, "Update check exception:", ex);
        }
    }

    private String fetchLatestVersion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASE_API_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ProxyChat")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }

        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(response.body());
        } catch (Exception ex) {
            throw new IOException("Failed to parse GitHub release response as JSON.", ex);
        }
        if (!rootElement.isJsonObject()) {
            return null;
        }

        JsonObject root = rootElement.getAsJsonObject();
        JsonElement tagElement = root.get("tag_name");
        if (tagElement == null || tagElement.isJsonNull()) {
            return null;
        }

        String tagName = tagElement.getAsString();
        return tagName == null || tagName.isBlank() ? null : tagName;
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "0";
        }
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("[.-]");
        String[] rightParts = right.split("[.-]");
        int max = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < max; i++) {
            String leftToken = i < leftParts.length ? leftParts[i] : "0";
            String rightToken = i < rightParts.length ? rightParts[i] : "0";

            boolean leftNumeric = leftToken.matches("\\d+");
            boolean rightNumeric = rightToken.matches("\\d+");

            if (leftNumeric && rightNumeric) {
                int cmp = Integer.compare(Integer.parseInt(leftToken), Integer.parseInt(rightToken));
                if (cmp != 0) {
                    return cmp;
                }
                continue;
            }

            int cmp = leftToken.compareToIgnoreCase(rightToken);
            if (cmp != 0) {
                return cmp;
            }
        }

        return 0;
    }
}
