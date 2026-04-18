package io.github.railgun19457.proxychat.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
        boolean chatRoutingEnabled,
        boolean chatRoutingDefaultSend,
        boolean chatRoutingDefaultReceive,
        Map<String, Boolean> chatRoutingSendMap,
        Map<String, Boolean> chatRoutingReceiveMap,
        boolean chatFilterEnabled,
        List<Pattern> chatFilterRules,
        boolean joinLeaveEnabled,
        String joinFirst,
        String joinSwitch,
        String joinLeave,
        boolean atEnabled,
        boolean atNotifyTitleEnabled,
        boolean atNotifyMessageEnabled,
        boolean atNotifyActionBarEnabled,
        String atNotifyTitle,
        String atNotifyMessage,
        String atNotifyActionBar
) {
    public RuntimeConfig {
        serverAliasMap = Collections.unmodifiableMap(new LinkedHashMap<>(serverAliasMap));
        serverAliasComponentMap = Collections.unmodifiableMap(new LinkedHashMap<>(serverAliasComponentMap));
        chatRoutingSendMap = Collections.unmodifiableMap(new LinkedHashMap<>(chatRoutingSendMap));
        chatRoutingReceiveMap = Collections.unmodifiableMap(new LinkedHashMap<>(chatRoutingReceiveMap));
        chatFilterRules = List.copyOf(chatFilterRules);
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

        String normalizedServer = serverName.toLowerCase(Locale.ROOT);
        return serverAliasComponentMap.getOrDefault(normalizedServer, Component.text(serverName));
    }

    public boolean canServerSendChat(String serverName) {
        if (!chatRoutingEnabled) {
            return true;
        }
        return resolveRoutingValue(serverName, chatRoutingDefaultSend, chatRoutingSendMap);
    }

    public boolean canServerReceiveChat(String serverName) {
        if (!chatRoutingEnabled) {
            return true;
        }
        return resolveRoutingValue(serverName, chatRoutingDefaultReceive, chatRoutingReceiveMap);
    }

    public boolean shouldBlockForwarding(String message) {
        if (!chatFilterEnabled || message == null || message.isEmpty()) {
            return false;
        }

        for (Pattern pattern : chatFilterRules) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean resolveRoutingValue(String serverName, boolean defaultValue, Map<String, Boolean> table) {
        if (serverName == null || serverName.isBlank()) {
            return defaultValue;
        }
        String normalizedServer = serverName.toLowerCase(Locale.ROOT);
        return table.getOrDefault(normalizedServer, defaultValue);
    }
}
