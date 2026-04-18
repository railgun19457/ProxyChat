package io.github.railgun19457.proxychat.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RuntimeConfig(
        int configVersion,
        boolean updateCheck,
        boolean updateNotifyAdmins,
        boolean loggingPrintChat,
        boolean loggingDebug,
        boolean chatEnabled,
        String chatFormat,
        boolean serverAliasEnabled,
        Map<String, String> serverAliasMap,
        Map<String, Component> serverAliasComponentMap,
        boolean joinLeaveEnabled,
        String joinFirst,
        String joinSwitch,
        String joinLeave,
        boolean atEnabled,
        String atNotifyTitle,
        String atNotifyMessage,
        String atNotifyActionBar,
        boolean atSoundEnabled
) {
    public RuntimeConfig {
        serverAliasMap = Collections.unmodifiableMap(new LinkedHashMap<>(serverAliasMap));
        serverAliasComponentMap = Collections.unmodifiableMap(new LinkedHashMap<>(serverAliasComponentMap));
        chatFormat = chatFormat == null ? "" : chatFormat;
        joinFirst = joinFirst == null ? "" : joinFirst;
        joinSwitch = joinSwitch == null ? "" : joinSwitch;
        joinLeave = joinLeave == null ? "" : joinLeave;
        atNotifyTitle = atNotifyTitle == null ? "" : atNotifyTitle;
        atNotifyMessage = atNotifyMessage == null ? "" : atNotifyMessage;
        atNotifyActionBar = atNotifyActionBar == null ? "" : atNotifyActionBar;
    }

    public String resolveServerAlias(String serverName) {
        return PlainTextComponentSerializer.plainText().serialize(resolveServerAliasComponent(serverName));
    }

    public Component resolveServerAliasComponent(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return Component.text("unknown");
        }
        if (!serverAliasEnabled) {
            return Component.text(serverName);
        }
        return serverAliasComponentMap.getOrDefault(serverName, Component.text(serverName));
    }
}
