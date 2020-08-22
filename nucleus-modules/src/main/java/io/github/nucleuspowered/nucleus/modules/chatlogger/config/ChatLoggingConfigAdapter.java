/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chatlogger.config;

import io.github.nucleuspowered.nucleus.quickstart.NucleusConfigAdapter;

public class ChatLoggingConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<ChatLoggingConfig> {

    public ChatLoggingConfigAdapter() {
        super(ChatLoggingConfig.class);
    }
}
