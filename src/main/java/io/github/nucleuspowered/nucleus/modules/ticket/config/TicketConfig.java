/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Optional;

@ConfigSerializable
public class TicketConfig {

    @Setting(value = "default-assignee", comment = "config.ticket.defaultassignee")
    private String defaultAssignee;

    public Optional<String> getDefaultAssignee() {
        return Optional.ofNullable(defaultAssignee);
    }
}
