package io.github.railgun19457.proxychat;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ServerConnection;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ChatListener {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ConfigManager configManager;

    public ChatListener(ProxyServer proxyServer, Logger logger, ConfigManager configManager) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configManager = configManager;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerChat(PlayerChatEvent event) {
        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.chatEnabled()) {
            return;
        }
        if (!event.getResult().isAllowed()) {
            return;
        }

        Player player = event.getPlayer();
        String serverName = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("unknown");

        String template = runtime.chatFormat();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("server", configManager.resolveServerName(serverName));
        placeholders.put("player", player.getUsername());
        placeholders.put("message", event.getResult().getMessage().orElse(event.getMessage()));

        Component rendered = configManager.render(template, placeholders);
        broadcast(rendered);

        if (runtime.loggingPrintChat()) {
            logger.info("{}", PlainTextComponentSerializer.plainText().serialize(rendered));
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.joinLeaveEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        RegisteredServer currentConnection = event.getServer();
        String currentServerRaw = currentConnection.getServerInfo().getName();
        String currentServer = configManager.resolveServerName(currentServerRaw);
        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getUsername());
        placeholders.put("server", currentServer);

        if (previousServer.isEmpty()) {
            broadcast(configManager.render(runtime.joinFirst(), placeholders));
        } else {
            String fromRaw = previousServer.get().getServerInfo().getName();
            if (fromRaw.equalsIgnoreCase(currentServerRaw)) {
                return;
            }
            placeholders.put("from", configManager.resolveServerName(fromRaw));
            placeholders.put("to", currentServer);
            broadcast(configManager.render(runtime.joinSwitch(), placeholders));
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.joinLeaveEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            return;
        }

        String template = runtime.joinLeave();
        if (template.isBlank()) {
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getUsername());
        placeholders.put("server", configManager.resolveServerName(currentServer.get().getServerInfo().getName()));
        broadcast(configManager.render(template, placeholders));
    }

    private void broadcast(Component component) {
        for (Player online : proxyServer.getAllPlayers()) {
            online.sendMessage(component);
        }
    }
}
