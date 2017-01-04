/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.services.ScheduledManager;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import org.slf4j.Logger;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.util.Optional;

public abstract class Nucleus {

    private static Nucleus nucleus;

    static void setNucleus(Nucleus nucleus) {
        if (Nucleus.nucleus == null) {
            Nucleus.nucleus = nucleus;
        }
    }

    public static Nucleus getNucleus() {
        return nucleus;
    }

    public abstract void saveData();

    public abstract Logger getLogger();

    public abstract UserDataManager getUserDataManager();

    public abstract WorldDataManager getWorldDataManager();

    public abstract void saveSystemConfig() throws IOException;

    public abstract void reload();

    public abstract WarmupManager getWarmupManager();

    public abstract ScheduledManager getScheduledManager();

    public abstract EconHelper getEconHelper();

    public abstract PermissionRegistry getPermissionRegistry();

    public abstract DiscoveryModuleContainer getModuleContainer();

    public abstract InternalServiceManager getInternalServiceManager();

    public abstract GeneralService getGeneralService();

    public abstract ItemDataService getItemDataService();

    public abstract NameUtil getNameUtil();

    public abstract ChatUtil getChatUtil();

    public abstract MessageProvider getMessageProvider();

    public abstract MessageProvider getCommandMessageProvider();

    public abstract Optional<MixinConfigProxy> getMixinConfigIfAvailable();

    public abstract NucleusTeleportHandler getTeleportHandler();

    public abstract boolean isDebugMode();
}
