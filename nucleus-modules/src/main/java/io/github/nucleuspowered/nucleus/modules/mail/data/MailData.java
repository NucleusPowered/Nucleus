/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.data;

import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class MailData implements MailMessage {

    @Setting
    private UUID uuid;

    @Setting
    private long date;

    @Setting
    private String message;

    public MailData() { }

    public MailData(UUID uuid, Instant date, String message) {
        this.uuid = uuid;
        this.date = date.toEpochMilli();
        this.message = message;
    }

    @Override public String getMessage() {
        return this.message;
    }

    @Override public Instant getDate() {
        return Instant.ofEpochMilli(this.date);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override public Optional<User> getSender() {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(this.uuid);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MailData mailData = (MailData) o;

        if (this.date != mailData.date) {
            return false;
        }
        return this.uuid.equals(mailData.uuid) && this.message.equals(mailData.message);
    }

    @Override public int hashCode() {
        int result = this.uuid.hashCode();
        result = 31 * result + (int) (this.date ^ (this.date >>> 32));
        result = 31 * result + this.message.hashCode();
        return result;
    }
}
