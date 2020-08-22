/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.platform;

import io.github.nucleuspowered.nucleus.services.interfaces.IPlatformService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;

import java.time.Instant;
import java.util.Optional;

import com.google.inject.Singleton;

@Singleton
public class PlatformService implements IPlatformService {

    @Nullable private Instant gameStartedTime;

    @Override public boolean isServer() {
        return Sponge.getPlatform().getType().isServer();
    }

    @Override public Optional<Instant> gameStartedTime() {
        return Optional.ofNullable(this.gameStartedTime);
    }

    @Override public void setGameStartedTime() {
        this.gameStartedTime = Instant.now();
    }

    @Override public void unsetGameStartedTime() {
        this.gameStartedTime = null;
    }
}
