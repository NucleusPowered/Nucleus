/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.data;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.datatypes.EndTimestamp;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public final class JailData extends EndTimestamp implements Jailing {

    @Setting
    private UUID jailer;

    @Setting
    private String jailName;

    @Setting
    private String reason;

    @Setting
    private double previousx = 0;

    @Setting
    private double previousy = -1;

    @Setting
    private double previousz = 0;

    @Setting
    private UUID world;

    @Setting
    private long creationTime = Instant.now().getEpochSecond();

    // Configurate
    public JailData() { }

    public JailData(UUID jailer, String jailName, String reason, Location<World> previousLocation) {
        this.jailer = jailer;
        this.reason = reason;
        this.jailName = jailName;

        if (previousLocation != null) {
            this.world = previousLocation.getExtent().getUniqueId();
            this.previousx = previousLocation.getX();
            this.previousy = previousLocation.getY();
            this.previousz = previousLocation.getZ();
        }
    }

    public JailData(UUID jailer, String jailName, String reason, Location<World> previousLocation, Instant endTimestamp) {
        this(jailer, jailName, reason, previousLocation);
        this.endtimestamp = endTimestamp.getEpochSecond();
    }

    public JailData(UUID jailer, String jailName, String reason, Location<World> previousLocation, Duration timeFromNextLogin) {
        this(jailer, jailName, reason, previousLocation);
        this.timeFromNextLogin = timeFromNextLogin.getSeconds();
    }

    public void setPreviousLocation(Location<World> previousLocation) {
        this.world = previousLocation.getExtent().getUniqueId();
        this.previousx = previousLocation.getX();
        this.previousy = previousLocation.getY();
        this.previousz = previousLocation.getZ();
    }

    @Override public String getReason() {
        return this.reason;
    }

    @Override public String getJailName() {
        return this.jailName;
    }

    @Override public Optional<UUID> getJailer() {
        return this.jailer.equals(Util.CONSOLE_FAKE_UUID) ? Optional.empty() : Optional.of(this.jailer);
    }

    public Optional<Instant> getCreationInstant() {
        return this.creationTime > 0 ? Optional.of(Instant.ofEpochSecond(this.creationTime)) : Optional.empty();
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public UUID getJailerInternal() {
        return this.jailer;
    }

    @Override public Optional<Location<World>> getPreviousLocation() {
        if (this.world != null) {
            Optional<World> ow = Sponge.getServer().getWorld(this.world);
            if (ow.isPresent() && this.previousx != 0 && this.previousy != -1 && this.previousz != 0) {
                return Optional.of(new Location<>(ow.get(), this.previousx, this.previousy, this.previousz));
            }
        }

        return Optional.empty();
    }
}
