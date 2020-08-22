/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger;

import static io.github.nucleuspowered.nucleus.modules.chatlogger.ChatLoggerModule.ID;

import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfig;
import io.github.nucleuspowered.nucleus.modules.chatlogger.config.ChatLoggingConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = ID, name = "Chat Logger")
public class ChatLoggerModule extends ConfigurableModule<ChatLoggingConfig, ChatLoggingConfigAdapter> {

    public static final String ID = "chat-logger";

    @Inject
    public ChatLoggerModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override public ChatLoggingConfigAdapter createAdapter() {
        return new ChatLoggingConfigAdapter();
    }
}
