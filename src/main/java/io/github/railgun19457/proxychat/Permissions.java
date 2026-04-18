package io.github.railgun19457.proxychat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;

public final class Permissions {
    public static final String COMMAND_RELOAD = "proxychat.command.reload";
    public static final String COMMAND_AT = "proxychat.command.at";
    public static final String UPDATE_NOTIFY = "proxychat.notify.update";

    private Permissions() {
    }

    public static boolean canUseAt(CommandSource source) {
        if (!(source instanceof Player)) {
            return true;
        }
        return source.getPermissionValue(COMMAND_AT) != Tristate.FALSE;
    }

    public static boolean canUseReload(CommandSource source) {
        return !(source instanceof Player) || source.hasPermission(COMMAND_RELOAD);
    }
}
