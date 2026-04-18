package io.github.railgun19457.proxychat.service;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.Permissions;
import io.github.railgun19457.proxychat.ConfigManager;
import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String RELEASE_API_URL = "https://api.github.com/repos/railgun19457/ProxyChat/releases/latest";
    private static final Pattern TAG_PATTERN = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ConfigManager configManager;
    private final String currentVersion;

    public UpdateChecker(ProxyServer proxyServer, Logger logger, ConfigManager configManager, String currentVersion) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configManager = configManager;
        this.currentVersion = currentVersion;
    }

    public void checkAsync() {
        CompletableFuture.runAsync(this::checkNow);
    }

    private void checkNow() {
        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.updateCheck()) {
            return;
        }

        try {
            String latestVersion = fetchLatestVersion();
            if (latestVersion == null || latestVersion.isBlank()) {
                configManager.debug("Update check response did not contain a tag name.");
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
                configManager.debug("{}", PlainTextComponentSerializer.plainText().serialize(noNeedMessage));
                return;
            }

            logger.info("[ProxyChat] Update available: {} -> {}", currentVersion, latestVersion);
            MessageConfig messages = configManager.messages();
            Component updateMessage = configManager.render(messages.updateAvailable(), Map.of(
                    "current", currentVersion,
                    "latest", latestVersion
            ));

            if (runtime.updateNotifyAdmins()) {
                for (Player player : proxyServer.getAllPlayers()) {
                    if (player.hasPermission(Permissions.UPDATE_NOTIFY)) {
                        player.sendMessage(updateMessage);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("[ProxyChat] Update check failed: {}", ex.getMessage());
            configManager.debug("Update check exception: {}", ex.toString());
        }
    }

    private String fetchLatestVersion() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASE_API_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ProxyChat")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }

        Matcher matcher = TAG_PATTERN.matcher(response.body());
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
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
