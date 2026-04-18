package io.github.railgun19457.proxychat;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.proxychat.service.PluginLogger;
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
    public static final String VERSION = BuildConstants.VERSION;

    private final ProxyServer proxyServer;
    private final PluginLogger pluginLogger;
    private final Path dataDirectory;

    private ConfigManager configManager;

    @Inject
    public PluginMain(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.pluginLogger = new PluginLogger(logger);
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(pluginLogger, dataDirectory);
        try {
            this.configManager.initialize();
        } catch (IOException ex) {
            pluginLogger.error(PluginLogger.Topic.CONFIG, "Failed to initialize configuration files.", ex);
            return;
        }

        proxyServer.getEventManager().register(this, new ChatListener(proxyServer, pluginLogger, configManager));
        registerCommands();

        UpdateChecker updateChecker = new UpdateChecker(this, proxyServer, pluginLogger, configManager, VERSION);
        updateChecker.checkAsync();

        pluginLogger.info(PluginLogger.Topic.LIFECYCLE, "Enabled successfully. version={}", VERSION);
    }

    private void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();

        commandManager.register(
            commandManager.metaBuilder("proxychat")
                .aliases("pc")
                        .plugin(this)
                        .build(),
                new ReloadCommand(configManager, pluginLogger)
        );

        commandManager.register(
                commandManager.metaBuilder("at")
                        .plugin(this)
                        .build(),
                new AtCommand(proxyServer, configManager, pluginLogger)
        );

        pluginLogger.info(PluginLogger.Topic.LIFECYCLE, "Commands registered: /proxychat, /pc, /at");
    }
}
