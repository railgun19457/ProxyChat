package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.model.RuntimeConfig;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
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

    public AtCommand(ProxyServer proxyServer, ConfigManager configManager) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        MessageConfig messages = configManager.messages();

        if (!source.hasPermission(Permissions.COMMAND_AT)) {
            source.sendMessage(configManager.render(messages.noPermission(), Map.of()));
            return;
        }

        RuntimeConfig runtime = configManager.runtime();
        if (!runtime.atEnabled()) {
            source.sendMessage(configManager.render(messages.atDisabled(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            source.sendMessage(configManager.render(messages.atUsage(), Map.of()));
            return;
        }

        Optional<Player> targetOptional = proxyServer.getPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            source.sendMessage(configManager.render(messages.playerNotFound(), Map.of("player", args[0])));
            return;
        }

        Player target = targetOptional.get();
        String rawMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String senderName = source instanceof Player sender ? sender.getUsername() : "Console";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", senderName);
        placeholders.put("sender", senderName);
        placeholders.put("target", target.getUsername());
        placeholders.put("message", rawMessage);

        String notifyTemplate = runtime.atNotifyMessage();
        if (!notifyTemplate.isBlank()) {
            target.sendMessage(configManager.render(notifyTemplate, placeholders));
        }

        String titleTemplate = runtime.atNotifyTitle();
        if (!titleTemplate.isBlank()) {
            target.showTitle(Title.title(
                    configManager.render(titleTemplate, placeholders),
                    configManager.render(notifyTemplate, placeholders),
                    Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(250))
            ));
        }

        if (runtime.atNotifyActionBar() != null && !runtime.atNotifyActionBar().isBlank()) {
            target.sendActionBar(configManager.render(runtime.atNotifyActionBar(), placeholders));
        }

        if (runtime.atSoundEnabled()) {
            target.playSound(Sound.sound(
                    Key.key("minecraft:entity.experience_orb.pickup"),
                    Sound.Source.PLAYER,
                    1.0f,
                    1.0f
            ));
        }

        source.sendMessage(configManager.render(messages.atSent(), placeholders));
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
        return invocation.source().hasPermission(Permissions.COMMAND_AT);
    }
}
