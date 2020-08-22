/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.playeronline;

import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerOnlineService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import com.google.inject.Singleton;

@Singleton
public class PlayerOnlineService implements IPlayerOnlineService {

    private static final BiPredicate<CommandSource, User> STANDARD_ONLINE =
            (source, user) -> Sponge.getServer().getPlayer(user.getUniqueId()).isPresent();
    private static final BiFunction<CommandSource, User, Optional<Instant>> STANDARD_LAST_PLAYED =
            (source, user) -> user.get(Keys.LAST_DATE_PLAYED);

    private BiPredicate<CommandSource, User> online = STANDARD_ONLINE;
    private BiFunction<CommandSource, User, Optional<Instant>> lastPlayed = STANDARD_LAST_PLAYED;

    @Override public boolean isOnline(CommandSource src, User player) {
        return this.online.test(src, player);
    }

    @Override public Optional<Instant> lastSeen(CommandSource src, User player) {
        return this.lastPlayed.apply(src, player);
    }

    @Override public void set(BiPredicate<CommandSource, User> isOnline, BiFunction<CommandSource, User, Optional<Instant>> lastSeen) {
        this.online = isOnline == null ? STANDARD_ONLINE : isOnline;
        this.lastPlayed = lastSeen == null ? STANDARD_LAST_PLAYED : lastSeen;
    }

    @Override public void reset() {
        set(null, null);
    }

}
