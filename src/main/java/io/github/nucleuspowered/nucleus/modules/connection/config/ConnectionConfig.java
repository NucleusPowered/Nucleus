/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ConnectionConfig {

    @Setting(value = "reserved-slots", comment = "loc:config.connection.reservedslots")
    private int reservedSlots = -1;

    public int getReservedSlots() {
        return reservedSlots;
    }
}
