/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.services;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.core.NucleusPlayerMetadataService;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IModuleDataProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@APIService(NucleusPlayerMetadataService.class)
@NonnullByDefault
public class PlayerMetadataService implements NucleusPlayerMetadataService, ServiceBase {

    private final IPermissionService permissionService;
    private final IModuleDataProvider moduleDataProvider;
    private final IStorageManager storageManager;

    @Inject
    public PlayerMetadataService(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
        this.moduleDataProvider = serviceCollection.moduleDataProvider();
        this.permissionService = serviceCollection.permissionService();
    }

    @Override public Optional<Result> getUserData(UUID uuid) {
        final Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid);
        final boolean isEligible;
        if (user.isPresent()) {
            final User u = user.get();
            isEligible = !this.permissionService.hasPermission(u, CorePermissions.EXEMPT_FIRST_JOIN) &&
                    (!this.moduleDataProvider.getModuleConfig(CoreConfig.class).isCheckFirstDatePlayed() || !Util.hasPlayedBeforeSponge(user.get()));
        } else {
            isEligible = true;
        }
        return this.storageManager.getUserService().get(uuid).join().map(x -> new ResultImpl(uuid, x, isEligible));
    }

    public static class ResultImpl implements Result {

        // private final User user;

        private final UUID uuid;
        @Nullable private final Instant login;
        @Nullable private final Instant logout;
        @Nullable private final String lastIP;
        private final boolean firstJoinProcess;

        private ResultImpl(UUID uuid, IUserDataObject udo, boolean eligibleForFirstJoin) {
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
            Optional<Player> pl = Sponge.getServer().getPlayer(this.uuid);
            if (pl.isPresent()) {
                Location<World> l = pl.get().getLocation();
                return Optional.of(Tuple.of(
                    l.getExtent().getProperties(),
                    l.getPosition()
                ));
            }

            return Optional.empty();
        }
    }
}
