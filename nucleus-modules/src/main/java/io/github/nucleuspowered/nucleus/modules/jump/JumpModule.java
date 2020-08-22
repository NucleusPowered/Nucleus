/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump;

import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfig;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = JumpModule.ID, name = "Jump")
public class JumpModule extends ConfigurableModule<JumpConfig, JumpConfigAdapter> {

    public final static String ID = "jump";

    @Inject
    public JumpModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public JumpConfigAdapter createAdapter() {
        return new JumpConfigAdapter();
    }
}
