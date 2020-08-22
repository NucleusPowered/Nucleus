/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfig;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = StaffChatModule.ID, name = "Staff Chat")
public class StaffChatModule extends ConfigurableModule<StaffChatConfig, StaffChatConfigAdapter> {

    public static final String ID = "staff-chat";

    @Inject
    public StaffChatModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public StaffChatConfigAdapter createAdapter() {
        return new StaffChatConfigAdapter();
    }

    @Override public void performPreTasks(INucleusServiceCollection serviceCollection) throws Exception {
        super.performPreTasks(serviceCollection);

        // Registers itself.
        new StaffChatMessageChannel(serviceCollection);
    }
}
