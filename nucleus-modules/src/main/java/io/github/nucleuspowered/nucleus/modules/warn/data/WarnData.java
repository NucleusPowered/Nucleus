/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.data;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.warning.data.Warning;
import io.github.nucleuspowered.nucleus.datatypes.EndTimestamp;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@ConfigSerializable
public class WarnData extends EndTimestamp implements Warning {
    @Setting
    private UUID warner;

    @Setting
    private String reason;

    @Setting
    private long date;

    @Setting
    private boolean expired = false;

    public WarnData() { }

    public WarnData(Instant date, UUID warner, String reason) {
        this(date, warner, reason, false);
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
    public WarnData(Instant date, UUID warner, String reason, @Nullable Duration timeFromNextLogin) {
        this(date, warner, reason);
        if (timeFromNextLogin == null) {
            this.timeFromNextLogin = null;
        } else {
            this.timeFromNextLogin = timeFromNextLogin.getSeconds();
        }
        this.date = date.toEpochMilli();
    }

    @Override public String getReason() {
        return this.reason;
    }

    @Override public Optional<UUID> getWarner() {
        return this.warner.equals(Util.CONSOLE_FAKE_UUID) ? Optional.empty() : Optional.of(this.warner);
    }

    @Override public Instant getDate() {
        return Instant.ofEpochMilli(this.date);
    }

    @Override public boolean isExpired() {
        return this.expired;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WarnData warnData = (WarnData) o;

        if (this.date != warnData.date) {
            return false;
        }
        if (this.expired != warnData.expired) {
            return false;
        }
        if (!this.warner.equals(warnData.warner)) {
            return false;
        }
        return this.reason.equals(warnData.reason);
    }

    @Override public int hashCode() {
        int result = this.warner.hashCode();
        result = 31 * result + this.reason.hashCode();
        result = 31 * result + (int) (this.date ^ (this.date >>> 32));
        result = 31 * result + (this.expired ? 1 : 0);
        return result;
    }
}
