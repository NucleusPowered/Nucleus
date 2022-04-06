/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.configurate.datatypes;

import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@ConfigSerializable
public final class UserCacheDataNode {

    @Setting
    @Nullable
    private String ipAddress;

    @Setting
    @Nullable
    private String jail = null;

    @Setting
    private boolean isMuted = false;

    public UserCacheDataNode() {
        // ignored - for Configurate
    }

    public UserCacheDataNode set(final IUserDataObject x,
            final Predicate<IUserDataObject> mutedProcessor,
            final Function<IUserDataObject, String> jailProcessor) {
        this.ipAddress = x.get(CoreKeys.IP_ADDRESS).map(y -> y.replace("/", "")).orElse(null);
        this.jail = jailProcessor.apply(x);
        this.isMuted = mutedProcessor.test(x);
        return this;
    }

    public Optional<String> getIpAddress() {
        return Optional.ofNullable(this.ipAddress);
    }

    public boolean isJailed() {
        return this.getJailName().isPresent();
    }

    public Optional<String> getJailName() {
        return Optional.ofNullable(this.jail);
    }

    public boolean isMuted() {
        return this.isMuted;
    }
}
