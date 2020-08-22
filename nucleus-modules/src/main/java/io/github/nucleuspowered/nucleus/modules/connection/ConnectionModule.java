/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connection;

import io.github.nucleuspowered.nucleus.modules.connection.config.ConnectionConfig;
import io.github.nucleuspowered.nucleus.modules.connection.config.ConnectionConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.annotation.ServerOnly;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ServerOnly
@ModuleData(id = ConnectionModule.ID, name = "Connection")
public class ConnectionModule extends ConfigurableModule<ConnectionConfig, ConnectionConfigAdapter> {

    public static final String ID = "connection";

    @Inject
    public ConnectionModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }


    @Override
    public ConnectionConfigAdapter createAdapter() {
        return new ConnectionConfigAdapter();
    }

}
