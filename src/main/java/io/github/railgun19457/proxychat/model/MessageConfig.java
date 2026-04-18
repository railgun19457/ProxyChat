package io.github.railgun19457.proxychat.model;

public record MessageConfig(
        String reloadSuccess,
        String reloadFailed,
        String noPermission,
        String atUsage,
        String atDisabled,
        String playerNotFound,
        String updateAvailable,
        String updateNoNeed,
        String chatFormat,
        String joinFirst,
        String joinSwitch,
        String leave,
        String atSent,
        String atReceivedTitle,
        String atReceivedMessage
) {
    public MessageConfig {
        reloadSuccess = normalize(reloadSuccess);
        reloadFailed = normalize(reloadFailed);
        noPermission = normalize(noPermission);
        atUsage = normalize(atUsage);
        atDisabled = normalize(atDisabled);
        playerNotFound = normalize(playerNotFound);
        updateAvailable = normalize(updateAvailable);
        updateNoNeed = normalize(updateNoNeed);
        chatFormat = normalize(chatFormat);
        joinFirst = normalize(joinFirst);
        joinSwitch = normalize(joinSwitch);
        leave = normalize(leave);
        atSent = normalize(atSent);
        atReceivedTitle = normalize(atReceivedTitle);
        atReceivedMessage = normalize(atReceivedMessage);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
