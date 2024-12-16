package com.earth2me.essentials.spawn;

import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.VersionUtil;
import net.ess3.api.IEssentials;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

class EssentialsSpawnPlayerListener implements Listener {
    private static final Logger logger = EssentialsSpawn.getWrappedLogger();
    private final transient IEssentials ess;
    private final transient SpawnStorage spawns;

    EssentialsSpawnPlayerListener(final IEssentials ess, final SpawnStorage spawns) {
        super();
        this.ess = ess;
        this.spawns = spawns;
    }

    void onPlayerRespawn(final PlayerRespawnEvent event) {
        final User user = ess.getUser(event.getPlayer());

        if (user.isJailed() && user.getJail() != null && !user.getJail().isEmpty()) {
            return;
        }

        if (ess.getSettings().getRespawnAtHome()) {
            final Location home;

            Location respawnLocation = null;
            if (ess.getSettings().isRespawnAtBed() &&
                    (!VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_16_1_R01) ||
                    (!event.isAnchorSpawn() || ess.getSettings().isRespawnAtAnchor()))) {
                // cannot nuke this sync load due to the event being sync so it would hand either way
                if(VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_16_1_R01)) {
                    respawnLocation = user.getBase().getRespawnLocation();
                } else { // For versions prior to 1.16.
                    respawnLocation = user.getBase().getBedSpawnLocation();
                }
            }

            if (respawnLocation != null) {
                home = respawnLocation;
            } else {
                home = user.getHome(user.getLocation());
            }

            if (home != null) {
                event.setRespawnLocation(home);
                return;
            }
        }
        if (tryRandomTeleport(user, ess.getSettings().getRandomRespawnLocation())) {
            return;
        }
        final Location spawn = spawns.getSpawn(user.getGroup());
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    void onPlayerJoin(final PlayerJoinEvent event) {
        ess.runTaskAsynchronously(() -> delayedJoin(event.getPlayer()));
    }

    private void delayedJoin(final Player player) {
        if (player.hasPlayedBefore()) {
            logger.log(Level.FINE, "Old player join");
            return;
        }

        final User user = ess.getUser(player);

        ess.scheduleSyncDelayedTask(() -> {
            final String kitName = ess.getSettings().getNewPlayerKit();
            if (!kitName.isEmpty()) {
                try {
                    final Kit kit = new Kit(kitName.toLowerCase(Locale.ENGLISH), ess);
                    kit.expandItems(user);
                } catch (final Exception ex) {
                    logger.log(Level.WARNING, ex.getMessage());
                }
            }

            logger.log(Level.FINE, "New player join");
        }, 2L);
    }

    private boolean tryRandomTeleport(final User user, final String name) {
        if (!ess.getRandomTeleport().hasLocation(name)) {
            return false;
        }
        ess.getRandomTeleport().getRandomLocation(name).thenAccept(location -> {
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            user.getAsyncTeleport().now(location, false, PlayerTeleportEvent.TeleportCause.PLUGIN, future);
        });
        return true;
    }
}
