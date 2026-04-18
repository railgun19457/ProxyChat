package io.github.railgun19457.proxychat;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.service.UpdateChecker;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = PluginMain.PLUGIN_ID,
        name = "ProxyChat",
        version = PluginMain.VERSION,
        description = "Cross-server chat plugin for Velocity",
        authors = {"railgun19457"}
)
public final class PluginMain {
    public static final String PLUGIN_ID = "proxychat";
    public static final String VERSION = "0.1.0";

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;

    @Inject
    public PluginMain(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(logger, dataDirectory);
        try {
            this.configManager.initialize();
        } catch (IOException ex) {
            logger.error("[ProxyChat] Failed to initialize configuration files.", ex);
            return;
        }

        proxyServer.getEventManager().register(this, new ChatListener(proxyServer, logger, configManager));
        registerCommands();

        UpdateChecker updateChecker = new UpdateChecker(proxyServer, logger, configManager, VERSION);
        updateChecker.checkAsync();

        logger.info("[ProxyChat] Enabled successfully. Version={}", VERSION);
    }

    private void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();

        commandManager.register(
            commandManager.metaBuilder("proxychat")
                .aliases("pc")
                        .plugin(this)
                        .build(),
                new ReloadCommand(configManager)
        );

        commandManager.register(
                commandManager.metaBuilder("at")
                        .plugin(this)
                        .build(),
                new AtCommand(proxyServer, configManager)
        );
    }
}
