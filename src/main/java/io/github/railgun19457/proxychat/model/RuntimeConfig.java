package io.github.railgun19457.proxychat.model;

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
        chatFormat = chatFormat == null ? "" : chatFormat;
        joinFirst = joinFirst == null ? "" : joinFirst;
        joinSwitch = joinSwitch == null ? "" : joinSwitch;
        joinLeave = joinLeave == null ? "" : joinLeave;
        atNotifyTitle = atNotifyTitle == null ? "" : atNotifyTitle;
        atNotifyMessage = atNotifyMessage == null ? "" : atNotifyMessage;
        atNotifyActionBar = atNotifyActionBar == null ? "" : atNotifyActionBar;
    }

    public String resolveServerAlias(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return "unknown";
        }
        if (!serverAliasEnabled) {
            return serverName;
        }
        return serverAliasMap.getOrDefault(serverName, serverName);
    }
}
