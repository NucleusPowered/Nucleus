/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.core.CoreModule;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfig;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = TeleportModule.ID, name = "Teleport", dependencies = CoreModule.ID)
public class TeleportModule extends ConfigurableModule<TeleportConfig, TeleportConfigAdapter> {

    public static final String ID = "teleport";

    @Inject
    public TeleportModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public TeleportConfigAdapter createAdapter() {
        return new TeleportConfigAdapter();
    }

    @Override protected ImmutableMap<String, String> remapCommand() {
        return ImmutableMap.<String, String>builder()
                .put("tpn", "minecraft:tp")
                .build();
    }
}
