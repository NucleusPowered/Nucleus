/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.services;

import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigProvider;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.core.NucleusPlayerMetadataService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

@APIService(NucleusPlayerMetadataService.class)
public class PlayerMetadataService implements NucleusPlayerMetadataService, ServiceBase {

    private final IPermissionService permissionService;
    private final IConfigProvider configProvider;
    private final IStorageManager storageManager;

    @Inject
    public PlayerMetadataService(final INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
        this.configProvider = serviceCollection.configProvider();
        this.permissionService = serviceCollection.permissionService();
    }

    @Override public Optional<Result> getUserData(final UUID uuid) {
        final Optional<User> user = Sponge.server().userManager().load(uuid).join();
        final boolean isEligible;
        if (user.isPresent()) {
            final User u = user.get();
            isEligible = !this.permissionService.hasPermission(u, CorePermissions.EXEMPT_FIRST_JOIN) &&
                    (!this.configProvider.getCoreConfig().isCheckFirstDatePlayed() || !Util.hasPlayedBeforeSponge(user.get()));
        } else {
            isEligible = true;
        }
        return this.storageManager.getUserService().get(uuid).join().map(x -> new ResultImpl(uuid, x, isEligible));
    }

    public static final class ResultImpl implements Result {

        // private final User user;

        private final UUID uuid;
        @Nullable private final Instant login;
        @Nullable private final Instant logout;
        @Nullable private final String lastIP;
        private final boolean firstJoinProcess;

        private ResultImpl(final UUID uuid, final IUserDataObject udo, final boolean eligibleForFirstJoin) {
            // this.user = userService.getUser();

            this.uuid = uuid;
            this.firstJoinProcess = eligibleForFirstJoin && !udo.get(CoreKeys.FIRST_JOIN_PROCESSED).orElse(false);
            this.login = udo.get(CoreKeys.LAST_LOGIN).orElse(null);
            this.logout = udo.get(CoreKeys.LAST_LOGOUT).orElse(null);
            this.lastIP = udo.get(CoreKeys.IP_ADDRESS).orElse(null);
        }

        @Override public boolean hasFirstJoinBeenProcessed() {
            return this.firstJoinProcess;
        }

        @Override public Optional<Instant> getLastLogin() {
            return Optional.ofNullable(this.login);
        }

        @Override public Optional<Instant> getLastLogout() {
            return Optional.ofNullable(this.logout);
        }

        @Override public Optional<String> getLastIP() {
            return Optional.ofNullable(this.lastIP);
        }

        @Override public Optional<Tuple<WorldProperties, Vector3d>> getLastLocation() {
            final Optional<ServerPlayer> pl = Sponge.server().player(this.uuid);
            if (pl.isPresent()) {
                final ServerLocation l = pl.get().serverLocation();
                return Optional.of(Tuple.of(
                    l.world().properties(),
                    l.position()
                ));
            }

            return Optional.empty();
        }
    }
}
