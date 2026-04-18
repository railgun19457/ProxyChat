package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.SimpleCommand;
import io.github.railgun19457.proxychat.model.MessageConfig;

import java.util.Map;

public final class ReloadCommand implements SimpleCommand {
    public static final String PERMISSION = "proxychat.reload";

    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        MessageConfig messageConfig = configManager.messages();
        if (!invocation.source().hasPermission(PERMISSION)) {
            invocation.source().sendMessage(configManager.render(messageConfig.noPermission(), Map.of()));
            return;
        }

        boolean success = configManager.reload();
        MessageConfig reloadedMessageConfig = configManager.messages();
        String output = success ? reloadedMessageConfig.reloadSuccess() : reloadedMessageConfig.reloadFailed();
        invocation.source().sendMessage(configManager.render(output, Map.of()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }
}
