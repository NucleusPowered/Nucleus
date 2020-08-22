/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class ConnectionConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<ConnectionConfig> {

    public ConnectionConfigAdapter() {
        super(ConnectionConfig.class);
    }
}
