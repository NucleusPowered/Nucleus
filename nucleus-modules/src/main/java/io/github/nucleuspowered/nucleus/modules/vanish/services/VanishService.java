/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.services;

import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class VanishService implements IReloadableService.Reloadable, ServiceBase {

    private boolean isAlter = false;
    private final Map<UUID, Instant> lastVanish = new HashMap<>();
    private final IPermissionService permissionService;
    private final IStorageManager storageManager;
    private final PluginContainer pluginContainer;

    @Inject
    public VanishService(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.storageManager = serviceCollection.storageManager();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        String property = System.getProperty("nucleus.vanish.tablist.enable");
        VanishConfig vanishConfig = serviceCollection.moduleDataProvider().getModuleConfig(VanishConfig.class);
        this.isAlter = property != null && !property.isEmpty() &&
                vanishConfig.isAlterTabList();
        if (!vanishConfig.isTryHidePlayers()) {
            serviceCollection.playerOnlineService().reset();
        } else {
            serviceCollection.playerOnlineService().set(this::isOnline, this::lastSeen);
        }
    }

    public boolean isOnline(CommandSource src, User player) {
        if (player.isOnline()) {
            if (isVanished(player)) {
                return this.permissionService.hasPermission(src, VanishPermissions.VANISH_SEE);
            }

            return true;
        }

        return false;
    }

    public Optional<Instant> lastSeen(CommandSource src, User player) {
        if (isOnline(src, player) || !player.isOnline() || !getLastVanishTime(player.getUniqueId()).isPresent()) {
            return player.get(Keys.LAST_DATE_PLAYED);
        } else {
            return getLastVanishTime(player.getUniqueId());
        }

    }

    public boolean isVanished(User player) {
        return this.storageManager.getUserService()
                .getOnThread(player.getUniqueId())
                .flatMap(x -> x.get(VanishKeys.VANISH_STATUS))
                .orElse(false);
    }

    public void vanishPlayer(User player) {
        vanishPlayer(player, false);
    }

    public void vanishPlayer(User player, boolean delay) {
        this.storageManager.getUserService()
                .getOrNewOnThread(player.getUniqueId())
                .set(VanishKeys.VANISH_STATUS, true);

        if (player instanceof Player) {
            if (delay) {
                Task.builder().execute(() -> vanishPlayerInternal((Player) player)).delayTicks(0).name("Nucleus Vanish runnable").submit(this.pluginContainer);
            } else {
                this.lastVanish.put(player.getUniqueId(), Instant.now());
                vanishPlayerInternal((Player) player);
            }
        }
    }

    private void vanishPlayerInternal(Player player) {
        vanishPlayerInternal(player,
                this.storageManager.getUserService()
                        .getOrNewOnThread(player.getUniqueId())
                        .get(VanishKeys.VANISH_STATUS)
                        .orElse(false));
    }

    private void vanishPlayerInternal(Player player, boolean vanish) {
        if (vanish) {
            player.offer(Keys.VANISH, true);
            player.offer(Keys.VANISH_IGNORES_COLLISION, true);
            player.offer(Keys.VANISH_PREVENTS_TARGETING, true);

            if (this.isAlter) {
                Sponge.getServer().getOnlinePlayers().stream().filter(x -> !player.equals(x) || !this.permissionService
                        .hasPermission(x, VanishPermissions.VANISH_SEE))
                        .forEach(x -> x.getTabList().removeEntry(player.getUniqueId()));
            }
        }
    }

    public void unvanishPlayer(User user) {
        this.storageManager.getUserService()
                .getOrNew(user.getUniqueId())
                .thenAccept(x -> x.set(VanishKeys.VANISH_STATUS, false));
        user.offer(Keys.VANISH, false);
        user.offer(Keys.VANISH_IGNORES_COLLISION, false);
        user.offer(Keys.VANISH_PREVENTS_TARGETING, false);

        if (this.isAlter && user instanceof Player) {
            Player player = (Player) user;
            Sponge.getServer().getOnlinePlayers().forEach(x -> {
                if (!x.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                    x.getTabList().addEntry(TabListEntry.builder()
                            .displayName(Text.of(player.getName()))
                            .profile(player.getProfile())
                            .gameMode(player.gameMode().get())
                            .latency(player.getConnection().getLatency())
                            .list(x.getTabList()).build());
                }
            });
        }
    }

    public void setLastVanishedTime(UUID pl, Instant instant) {
        this.lastVanish.put(pl, instant);
    }

    Optional<Instant> getLastVanishTime(UUID pl) {
        return Optional.ofNullable(this.lastVanish.get(pl));
    }

    public void clearLastVanishTime(UUID pl) {
        this.lastVanish.remove(pl);
    }

}
