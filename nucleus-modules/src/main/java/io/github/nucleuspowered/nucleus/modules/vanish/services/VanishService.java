/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ISchedulerService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VanishService implements IReloadableService.Reloadable, ServiceBase {

    private boolean isAlter = false;
    private final Map<UUID, Instant> lastVanish = new HashMap<>();
    private final IPermissionService permissionService;
    private final IStorageManager storageManager;
    private final ISchedulerService schedulerService;

    @Inject
    public VanishService(final INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.storageManager = serviceCollection.storageManager();
        this.schedulerService = serviceCollection.schedulerService();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final String property = System.getProperty("nucleus.vanish.tablist.enable");
        final VanishConfig vanishConfig = serviceCollection.configProvider().getModuleConfig(VanishConfig.class);
        this.isAlter = property != null && !property.isEmpty() &&
                vanishConfig.isAlterTabList();
        if (!vanishConfig.isTryHidePlayers()) {
            serviceCollection.playerOnlineService().reset();
        } else {
            serviceCollection.playerOnlineService().set(this::isOnline, this::lastSeen);
        }
    }

    public boolean isOnline(@Nullable final ServerPlayer src, final User player) {
        if (player.isOnline()) {
            if (src != null && this.isVanished(player.uniqueId())) {
                return this.permissionService.hasPermission(src, VanishPermissions.VANISH_SEE);
            }

            return true;
        }

        return false;
    }

    public Optional<Instant> lastSeen(@Nullable final ServerPlayer src, final User player) {
        if (src == null || this.isOnline(src, player) || !player.isOnline() || !this.getLastVanishTime(player.uniqueId()).isPresent()) {
            return player.get(Keys.LAST_DATE_PLAYED);
        } else {
            return this.getLastVanishTime(player.uniqueId());
        }

    }

    public boolean isVanished(final UUID uuid) {
        return this.storageManager.getUserService()
                .getOnThread(uuid)
                .flatMap(x -> x.get(VanishKeys.VANISH_STATUS))
                .orElse(false);
    }

    public void vanishPlayer(final User player) {
        this.vanishPlayer(player, false);
    }

    public void vanishPlayer(final User player, final boolean delay) {
        this.storageManager.getUserService()
                .getOrNewOnThread(player.uniqueId())
                .set(VanishKeys.VANISH_STATUS, true);

        if (player.isOnline()) {
            if (delay) {
                this.schedulerService.runOnMainThread(() -> this.vanishPlayerInternal(player.player().get()));
            } else {
                this.lastVanish.put(player.uniqueId(), Instant.now());
                this.vanishPlayerInternal((Player) player);
            }
        }
    }

    private void vanishPlayerInternal(final Player player) {
        this.vanishPlayerInternal(player,
                this.storageManager.getUserService()
                        .getOrNewOnThread(player.uniqueId())
                        .get(VanishKeys.VANISH_STATUS)
                        .orElse(false));
    }

    private void vanishPlayerInternal(final Player player, final boolean vanish) {
        if (vanish) {
            player.offer(Keys.VANISH_STATE, VanishState.vanished());

            if (this.isAlter) {
                Sponge.server().onlinePlayers().stream().filter(x -> !player.equals(x) || !this.permissionService
                        .hasPermission(x, VanishPermissions.VANISH_SEE))
                        .forEach(x -> x.tabList().removeEntry(player.uniqueId()));
            }
        }
    }

    public void unvanishPlayer(final User user) {
        this.storageManager.getUserService()
                .getOrNew(user.uniqueId())
                .thenAccept(x -> x.set(VanishKeys.VANISH_STATUS, false));
        user.offer(Keys.VANISH_STATE, VanishState.unvanished());

        user.player().filter(x -> this.isAlter).ifPresent(player -> {
            Sponge.server().onlinePlayers().forEach(x -> {
                if (!x.tabList().entry(player.uniqueId()).isPresent()) {
                    x.tabList().addEntry(TabListEntry.builder()
                            .displayName(Component.text(player.name()))
                            .profile(player.profile())
                            .gameMode(player.gameMode().get())
                            .latency(player.connection().latency())
                            .list(x.tabList()).build());
                }
            });
        });
    }

    public void setLastVanishedTime(final UUID pl, final Instant instant) {
        this.lastVanish.put(pl, instant);
    }

    Optional<Instant> getLastVanishTime(final UUID pl) {
        return Optional.ofNullable(this.lastVanish.get(pl));
    }

    public void clearLastVanishTime(final UUID pl) {
        this.lastVanish.remove(pl);
    }

}
