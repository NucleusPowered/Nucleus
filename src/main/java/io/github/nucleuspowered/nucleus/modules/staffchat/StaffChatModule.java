/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = StaffChatModule.moduleID, name = "Staff Chat")
public class StaffChatModule extends ConfigurableModule<StaffChatConfigAdapter> {

    static final String moduleID = "staff-chat";

    @Override
    public StaffChatConfigAdapter createAdapter() {
        return new StaffChatConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        StaffChatMessageChannel.INSTANCE = new StaffChatMessageChannel(plugin);
    }
}
