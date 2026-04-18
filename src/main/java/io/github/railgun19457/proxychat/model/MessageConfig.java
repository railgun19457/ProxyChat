package io.github.railgun19457.proxychat.model;

public record MessageConfig(
        String reloadSuccess,
        String reloadFailed,
        String reloadUsage,
        String noPermission,
        String atUsage,
        String atDisabled,
        String playerNotFound,
        String updateAvailable,
        String updateNoNeed,
        String atSent
    ) {
    public MessageConfig {
        reloadSuccess = normalize(reloadSuccess);
        reloadFailed = normalize(reloadFailed);
        reloadUsage = normalize(reloadUsage);
        noPermission = normalize(noPermission);
        atUsage = normalize(atUsage);
        atDisabled = normalize(atDisabled);
        playerNotFound = normalize(playerNotFound);
        updateAvailable = normalize(updateAvailable);
        updateNoNeed = normalize(updateNoNeed);
        atSent = normalize(atSent);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
