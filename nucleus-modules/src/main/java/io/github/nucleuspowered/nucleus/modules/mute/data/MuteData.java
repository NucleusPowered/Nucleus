/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.data;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.datatypes.EndTimestamp;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@ConfigSerializable
public final class MuteData extends EndTimestamp implements Mute {

    @Setting
    private UUID muter;

    @Setting
    private String reason;

    @Setting
    private long creationTime = Instant.now().getEpochSecond();

    // For Configurate
    public MuteData() { }

    public MuteData(UUID muter, String reason) {
        this.muter = muter;
        this.reason = reason;
    }

    /**
     * Creates the data.
     *
     * @param muter The UUID of the muter
     * @param endtimestamp The end timestamp
     * @param reason The reason
     */
    public MuteData(UUID muter, String reason, Instant endtimestamp) {
        this(muter, reason);
        this.endtimestamp = endtimestamp.getEpochSecond();
    }

    /**
     * Creates the data.
     *
     * @param muter The UUID of the muter
     * @param reason The reason
     * @param timeFromNextLogin The time to mute for from next login.
     */
    public MuteData(UUID muter, String reason, @Nullable Duration timeFromNextLogin) {
        this(muter, reason);
        if (timeFromNextLogin != null) {
            this.timeFromNextLogin = timeFromNextLogin.getSeconds();
        }
    }

    @Override public String getReason() {
        return this.reason;
    }

    /**
     * Gets the timestamp for the end of the mute.
     *
     * @return An {@link Instant}
     */
    public Optional<Instant> getEndTimestamp() {
        if (this.endtimestamp == null) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochSecond(this.endtimestamp));
    }

    public Optional<Instant> getCreationInstant() {
        return this.creationTime > 0 ? Optional.of(Instant.ofEpochSecond(this.creationTime)) : Optional.empty();
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    @Override public Optional<UUID> getMuter() {
        return this.muter.equals(Util.CONSOLE_FAKE_UUID) ? Optional.empty() : Optional.of(this.muter);
    }

    public UUID getMuterInternal() {
        return this.muter;
    }

    public Optional<Duration> getTimeFromNextLogin() {
        if (this.timeFromNextLogin == null) {
            return Optional.empty();
        }

        return Optional.of(Duration.of(this.timeFromNextLogin, ChronoUnit.SECONDS));
    }
}
