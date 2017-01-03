/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = "connection-messages", name = "Connection Messages")
public class ConnectionMessagesModule extends ConfigurableModule<ConnectionMessagesConfigAdapter> {

    @Override
    public ConnectionMessagesConfigAdapter createAdapter() {
        return new ConnectionMessagesConfigAdapter();
    }
}
