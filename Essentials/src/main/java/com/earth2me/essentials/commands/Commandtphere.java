package com.earth2me.essentials.commands;

import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.TranslatableException;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Collections;
import java.util.List;

public class Commandtphere extends EssentialsCommand {
    public Commandtphere() {
        super("tphere");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final User player = getPlayer(server, user, args, 0);
        if (!player.isTeleportEnabled()) {
            throw new TranslatableException("teleportDisabled", player.getName());
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())) {
            throw new TranslatableException("noPerm", "essentials.worlds." + user.getWorld().getName());
        }
        user.getAsyncTeleport().teleportPlayer(player, user.getBase(), new Trade(this.getName(), ess), TeleportCause.COMMAND, getNewExceptionFuture(user.getSource(), commandLabel));
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
