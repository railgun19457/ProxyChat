package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.proxychat.model.MessageConfig;
import io.github.railgun19457.proxychat.service.PluginLogger;

import java.util.List;
import java.util.Map;

public final class ReloadCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final PluginLogger pluginLogger;

    public ReloadCommand(ConfigManager configManager, PluginLogger pluginLogger) {
        this.configManager = configManager;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public void execute(Invocation invocation) {
        MessageConfig messageConfig = configManager.messages();
        if (!Permissions.canUseReload(invocation.source())) {
            pluginLogger.warn(
                    PluginLogger.Topic.SECURITY,
                    "Blocked /proxychat reload without permission. source={}",
                    sourceName(invocation.source())
            );
            invocation.source().sendMessage(configManager.render(messageConfig.noPermission(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            pluginLogger.debug(
                    PluginLogger.Topic.COMMAND,
                    "Rejected /proxychat with invalid args. source={}, args={}",
                    sourceName(invocation.source()),
                    String.join(" ", args)
            );
            invocation.source().sendMessage(configManager.render(messageConfig.reloadUsage(), Map.of()));
            return;
        }

        pluginLogger.info(PluginLogger.Topic.COMMAND, "Reload requested. source={}", sourceName(invocation.source()));
        boolean success = configManager.reload();
        MessageConfig reloadedMessageConfig = configManager.messages();
        String output = success ? reloadedMessageConfig.reloadSuccess() : reloadedMessageConfig.reloadFailed();
        invocation.source().sendMessage(configManager.render(output, Map.of()));
        if (success) {
            pluginLogger.info(PluginLogger.Topic.COMMAND, "Reload completed successfully. source={}", sourceName(invocation.source()));
        } else {
            pluginLogger.warn(PluginLogger.Topic.COMMAND, "Reload failed. source={}", sourceName(invocation.source()));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.canUseReload(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("reload");
        }
        return List.of();
    }

    private static String sourceName(com.velocitypowered.api.command.CommandSource source) {
        return source instanceof Player player ? player.getUsername() : "Console";
    }
}
