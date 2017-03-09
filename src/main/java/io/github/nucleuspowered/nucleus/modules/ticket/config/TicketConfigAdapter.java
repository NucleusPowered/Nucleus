/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.config;

import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;

public class TicketConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<TicketConfig> {

    public TicketConfigAdapter() {
        super(TicketConfig.class);
    }
}
