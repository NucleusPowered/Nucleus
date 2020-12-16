/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.services.impl.playername.PlayerDisplayNameService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ImplementedBy(PlayerDisplayNameService.class)
public interface IPlayerDisplayNameService {

    void provideDisplayNameResolver(DisplayNameResolver resolver);

    void provideDisplayNameQuery(DisplayNameQuery resolver);

    Optional<User> getUser(Text displayName);

    Optional<User> getUser(String displayName);

    Optional<Player> getPlayer(Text displayName);

    Optional<Player> getPlayer(String displayName);

    /**
     * Gets the {@link UUID} of the players that have a name or display name
     * that starts with the given parameter.
     *
     * @param displayName The display name
     * @return The {@link UUID}
     */
    Map<UUID, List<String>> startsWith(String displayName);

    Text getDisplayName(UUID playerUUID);

    default Text getDisplayName(Player player) {
        return getDisplayName(player.getUniqueId());
    }

    default Text getDisplayName(User user) {
        return getDisplayName(user.getUniqueId());
    }

    Text getDisplayName(CommandSource source);

    Text getName(CommandSource user);

    Text addCommandToName(CommandSource p);

    Text addCommandToDisplayName(CommandSource p);

    @FunctionalInterface
    interface DisplayNameResolver {

        Optional<Text> resolve(UUID userUUID);

    }

    interface DisplayNameQuery {

        Optional<User> resolve(String name);

        Map<UUID, String> startsWith(String name);

    }

}
