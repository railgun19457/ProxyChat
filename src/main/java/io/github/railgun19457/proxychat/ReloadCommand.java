package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.SimpleCommand;
import io.github.railgun19457.proxychat.model.MessageConfig;

import java.util.List;
import java.util.Map;

public final class ReloadCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        MessageConfig messageConfig = configManager.messages();
        if (!invocation.source().hasPermission(Permissions.COMMAND_RELOAD)) {
            invocation.source().sendMessage(configManager.render(messageConfig.noPermission(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            invocation.source().sendMessage(configManager.render(messageConfig.reloadUsage(), Map.of()));
            return;
        }

        boolean success = configManager.reload();
        MessageConfig reloadedMessageConfig = configManager.messages();
        String output = success ? reloadedMessageConfig.reloadSuccess() : reloadedMessageConfig.reloadFailed();
        invocation.source().sendMessage(configManager.render(output, Map.of()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(Permissions.COMMAND_RELOAD);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
