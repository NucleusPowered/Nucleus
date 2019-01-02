/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.dataservices.NameBanService;
import io.github.nucleuspowered.nucleus.dataservices.UserCacheService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.TextFileController;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import io.github.nucleuspowered.nucleus.internal.services.PermissionResolver;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.internal.text.TextParsingUtils;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.storage.INucleusStorageManager;
import org.slf4j.Logger;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

    public abstract void addX(List<Text> messages, int spacing);

    public abstract void saveData();

    public abstract Logger getLogger();

    public abstract Path getConfigDirPath();

    public abstract Path getDataPath();

    public abstract UserCacheService getUserCacheService();

    public abstract void saveSystemConfig() throws IOException;

    public abstract boolean reload();

    public abstract boolean reloadMessages();

    public abstract WarmupManager getWarmupManager();

    public abstract WarmupConfig getWarmupConfig();

    public abstract EconHelper getEconHelper();

    public abstract PermissionRegistry getPermissionRegistry();

    public abstract DiscoveryModuleContainer getModuleContainer();

    public abstract boolean isModuleLoaded(String moduleId);

    public abstract <T extends NucleusConfigAdapter<?>> Optional<T> getConfigAdapter(String id, Class<T> configAdapterClass);

    public <R, C, T extends NucleusConfigAdapter<C>> Optional<R> getConfigValue(String id, Class<T> configAdapterClass, Function<C, R> fnToGetValue) {
        Optional<T> tOptional = getConfigAdapter(id, configAdapterClass);
        return tOptional.map(t -> fnToGetValue.apply(t.getNodeOrDefault()));

    }

    public abstract InternalServiceManager getInternalServiceManager();

    public abstract Optional<Instant> getGameStartedTime();

    public abstract ItemDataService getItemDataService();

    public abstract NameUtil getNameUtil();

    public abstract TextParsingUtils getTextParsingUtils();

    public abstract MessageProvider getMessageProvider();

    public abstract MessageProvider getCommandMessageProvider();

    public abstract int traceUserCreations();

    public abstract Optional<TextFileController> getTextFileController(String getController);

    public abstract void addTextFileController(String id, Asset asset, Path file) throws IOException;

    public abstract void registerReloadable(Reloadable reloadable);

    public abstract Optional<DocGenCache> getDocGenCache();

    public abstract NucleusTeleportHandler getTeleportHandler();

    public abstract NucleusMessageTokenService getMessageTokenService();

    public abstract boolean isDebugMode();

    public abstract void printStackTraceIfDebugMode(Throwable throwable);

    public abstract KitService getKitService();

    public abstract NameBanService getNameBanService();

    public abstract CommandsConfig getCommandsConfig();

    public abstract PluginContainer getPluginContainer();

    public abstract boolean isSessionDebug();

    public abstract void setSessionDebug(boolean debug);

    public abstract PermissionResolver getPermissionResolver();

    public abstract boolean isServer();

    public abstract void addStartupMessage(Text message);

    public abstract boolean isPrintingSavesAndLoads();

    public abstract INucleusStorageManager getStorageManager();
}
