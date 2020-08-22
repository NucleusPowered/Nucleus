/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.deathmessage;

import static io.github.nucleuspowered.nucleus.modules.deathmessage.DeathMessageModule.ID;

import io.github.nucleuspowered.nucleus.modules.deathmessage.config.DeathMessageConfig;
import io.github.nucleuspowered.nucleus.modules.deathmessage.config.DeathMessageConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = ID, name = "Death Messages")
public class DeathMessageModule extends ConfigurableModule<DeathMessageConfig, DeathMessageConfigAdapter> {

    public static final String ID = "death-message";

    @Inject
    public DeathMessageModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public DeathMessageConfigAdapter createAdapter() {
        return new DeathMessageConfigAdapter();
    }
}
