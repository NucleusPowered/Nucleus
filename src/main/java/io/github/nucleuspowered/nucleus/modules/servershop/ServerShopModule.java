/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.servershop.config.ServerShopConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = ServerShopModule.ID, name = "Server Shop")
public class ServerShopModule extends ConfigurableModule<ServerShopConfigAdapter> {

    public static final String ID = "server-shop";

    @Override
    public ServerShopConfigAdapter createAdapter() {
        return new ServerShopConfigAdapter();
    }
}
