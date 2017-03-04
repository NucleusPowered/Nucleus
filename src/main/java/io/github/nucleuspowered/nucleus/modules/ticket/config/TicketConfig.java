/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class TicketConfig {
    @Setting(value = "database", comment = "config.ticket.database")
    private TicketDatabaseConfig ticketDatabaseConfig = new TicketDatabaseConfig();

    public TicketDatabaseConfig getTicketDatabaseConfig() {
        return ticketDatabaseConfig;
    }
}
