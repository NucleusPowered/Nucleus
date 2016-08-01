/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.connection.config.ConnectionConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "connection", name = "Connection")
public class ConnectionModule extends ConfigurableModule<ConnectionConfigAdapter> {

    @Override
    public ConnectionConfigAdapter getAdapter() {
        return new ConnectionConfigAdapter();
    }
}
