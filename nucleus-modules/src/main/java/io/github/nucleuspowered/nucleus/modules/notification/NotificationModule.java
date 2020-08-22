/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification;

import static io.github.nucleuspowered.nucleus.modules.notification.NotificationModule.ID;

import io.github.nucleuspowered.nucleus.modules.notification.config.NotificationConfig;
import io.github.nucleuspowered.nucleus.modules.notification.config.NotificationConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = ID, name = "Notification")
public class NotificationModule extends ConfigurableModule<NotificationConfig, NotificationConfigAdapter> {

    public static final String ID = "notification";

    @Inject
    public NotificationModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public NotificationConfigAdapter createAdapter() {
        return new NotificationConfigAdapter();
    }
}
