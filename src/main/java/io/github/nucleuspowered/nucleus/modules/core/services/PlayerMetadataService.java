/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.services;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.service.NucleusPlayerMetadataService;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.UserDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
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
public class PlayerMetadataService implements NucleusPlayerMetadataService, ServiceBase, IDataManagerTrait {

    @Override public Optional<Result> getUserData(UUID uuid) {
        return getUser(uuid).join().map(x -> new ResultImpl(uuid, x));
    }

    public class ResultImpl implements Result {

        // private final User user;

        private final UUID uuid;
        @Nullable private final Instant login;
        @Nullable private final Instant logout;
        @Nullable private final String lastIP;
        @Nullable private final LocationNode lastLocation;

        private ResultImpl(UUID uuid, UserDataObject udo) {
            // this.user = userService.getUser();

            this.uuid = uuid;
            this.login = udo.get(CoreKeys.LAST_LOGIN).orElse(null);
            this.logout = udo.get(CoreKeys.LAST_LOGOUT).orElse(null);
            this.lastIP = udo.get(CoreKeys.IP_ADDRESS).orElse(null);
            this.lastLocation = udo.get(CoreKeys.LAST_LOCATION).orElse(null);
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

            try {
                if (this.lastLocation != null) {
                    return Optional.of(this.lastLocation.getLocationIfNotLoaded());
                }
            } catch (Exception ignored) {}

            return Optional.empty();
        }
    }
}
