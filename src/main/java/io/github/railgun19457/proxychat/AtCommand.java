package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import io.github.railgun19457.proxychat.service.PluginLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AtCommand implements SimpleCommand {
    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final PluginLogger pluginLogger;

    public AtCommand(ProxyServer proxyServer, ConfigManager configManager, PluginLogger pluginLogger) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        MessageConfig messages = configManager.messages();

        if (!Permissions.canUseAt(source)) {
            pluginLogger.warn(PluginLogger.Topic.SECURITY, "Blocked /at without permission. source={}", sourceName(source));
            source.sendMessage(configManager.render(messages.noPermission(), Map.of()));
            return;
        }

        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.atEnabled()) {
            pluginLogger.debug(PluginLogger.Topic.COMMAND, "Rejected /at because feature is disabled. source={}", sourceName(source));
            source.sendMessage(configManager.render(messages.atDisabled(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            pluginLogger.debug(PluginLogger.Topic.COMMAND, "Rejected /at with no target argument. source={}", sourceName(source));
            source.sendMessage(configManager.render(messages.atUsage(), Map.of()));
            return;
        }

        Optional<Player> targetOptional = proxyServer.getPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            pluginLogger.debug(
                    PluginLogger.Topic.COMMAND,
                    "Rejected /at because target not found. source={}, target={}",
                    sourceName(source),
                    args[0]
            );
            source.sendMessage(configManager.render(messages.playerNotFound(), Map.of("player", args[0])));
            return;
        }

        Player target = targetOptional.get();
        String rawMessage = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "";
        String senderName = source instanceof Player sender ? sender.getUsername() : "Console";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", senderName);
        placeholders.put("sender", senderName);
        placeholders.put("target", target.getUsername());
        placeholders.put("message", rawMessage);

        String notifyTemplate = runtime.atNotifyMessage();
        Component notifyComponent = Component.empty();
        if (runtime.atNotifyMessageEnabled() && !notifyTemplate.isBlank()) {
            notifyComponent = configManager.render(notifyTemplate, placeholders);
            target.sendMessage(notifyComponent);
        }

        String titleTemplate = runtime.atNotifyTitle();
        if (runtime.atNotifyTitleEnabled() && !titleTemplate.isBlank()) {
            target.showTitle(Title.title(
                    configManager.render(titleTemplate, placeholders),
                    notifyComponent,
                    Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(250))
            ));
        }

        if (runtime.atNotifyActionBarEnabled()
                && runtime.atNotifyActionBar() != null
                && !runtime.atNotifyActionBar().isBlank()) {
            target.sendActionBar(configManager.render(runtime.atNotifyActionBar(), placeholders));
        }

        source.sendMessage(configManager.render(messages.atSent(), placeholders));
        pluginLogger.info(
                PluginLogger.Topic.COMMAND,
                "Delivered /at notification. source={}, target={}, message-length={}",
                senderName,
                target.getUsername(),
                rawMessage.length()
        );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return proxyServer.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .toList();
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.canUseAt(invocation.source());
    }

    private static String sourceName(CommandSource source) {
        return source instanceof Player player ? player.getUsername() : "Console";
    }
}
