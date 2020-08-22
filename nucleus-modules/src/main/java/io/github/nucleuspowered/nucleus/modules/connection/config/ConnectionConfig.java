/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

@ConfigSerializable
public class ConnectionConfig {

    @Setting(value = "reserved-slots", comment = "config.connection.reservedslots")
    private int reservedSlots = -1;

    @Setting(value = "whitelist-message", comment = "config.connection.whitelistmessage")
    private String whitelistMessage = "";

    @Setting(value = "server-full-message", comment = "config.connection.serverfullmessage")
    private String serverFullMessage = "";

    public int getReservedSlots() {
        return this.reservedSlots;
    }

    public Optional<Text> getWhitelistMessage() {
        return getMessageFrom(this.whitelistMessage);
    }

    public Optional<Text> getServerFullMessage() {
        return getMessageFrom(this.serverFullMessage);
    }

    private Optional<Text> getMessageFrom(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(TextSerializers.FORMATTING_CODE.deserializeUnchecked(text));
    }

}
