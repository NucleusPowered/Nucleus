/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.data;

import io.github.nucleuspowered.nucleus.api.data.interfaces.EndTimestamp;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class WarnData extends EndTimestamp {
    @Setting
    private UUID warner;

    @Setting
    private Long endtimestamp;

    @Setting
    private String reason;

    @Setting
    private Long timeFromNextLogin;

    @Setting
    private long date;

    @Setting
    private boolean expired = false;

    public WarnData() { }

    public WarnData(Instant date, UUID warner, String reason) {
        this.warner = warner;
        this.reason = reason;
        this.date = date.toEpochMilli();
    }

    public WarnData(Instant date, UUID warner, String reason, boolean expired) {
        this.warner = warner;
        this.reason = reason;
        this.expired = expired;
        this.date = date.toEpochMilli();
    }


    /**
     * Creates the data.
     *
     * @param warner       The UUID of the warner
     * @param endtimestamp The end timestamp
     * @param reason       The reason
     * @param date         The date
     */
    public WarnData(Instant date, UUID warner, String reason, Instant endtimestamp) {
        this(date, warner, reason);
        this.endtimestamp = endtimestamp.getEpochSecond();
        this.date = date.toEpochMilli();
    }

    /**
     * Creates the data.
     *
     * @param warner            The UUID of the muter
     * @param reason            The reason
     * @param timeFromNextLogin The time to warn for from next login.
     * @param date              The date
     */
    public WarnData(Instant date, UUID warner, String reason, Duration timeFromNextLogin) {
        this(date, warner, reason);
        this.timeFromNextLogin = timeFromNextLogin.getSeconds();
        this.date = date.toEpochMilli();
    }

    public String getReason() {
        return reason;
    }

    /**
     * Gets the timestamp for the end of the warning.
     *
     * @return An {@link Instant}
     */
    public Optional<Instant> getEndTimestamp() {
        if (endtimestamp == null) {
            return Optional.empty();
        }

        return Optional.of(Instant.ofEpochSecond(endtimestamp));
    }

    public UUID getWarner() {
        return warner;
    }

    public Optional<Duration> getTimeFromNextLogin() {
        if (timeFromNextLogin == null) {
            return Optional.empty();
        }

        return Optional.of(Duration.of(timeFromNextLogin, ChronoUnit.SECONDS));
    }

    public Instant getDate() {
        return Instant.ofEpochMilli(date);
    }

    public boolean isExpired() {
        return expired;
    }
}
