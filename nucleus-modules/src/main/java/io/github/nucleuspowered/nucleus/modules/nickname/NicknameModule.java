/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname;

import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfig;
import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = NicknameModule.ID, name = "Nickname")
public class NicknameModule extends ConfigurableModule<NicknameConfig, NicknameConfigAdapter> {

    public final static String ID = "nickname";

    @Inject
    public NicknameModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override public void performPostTasks(INucleusServiceCollection serviceCollection) {
        // Register resolver and query.
        serviceCollection.getServiceUnchecked(NicknameService.class).injectResolver(serviceCollection);
    }

    @Override
    public NicknameConfigAdapter createAdapter() {
        return new NicknameConfigAdapter();
    }
}
