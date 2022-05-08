/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services;

import com.google.inject.ImplementedBy;
import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.core.IPropertyHolder;
import io.github.nucleuspowered.nucleus.core.services.impl.NucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandElementSupplier;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICompatibilityService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICooldownService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IEconomyServiceProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IModuleReporter;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlaceholderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlatformService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerInformationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ISchedulerService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextFileControllerCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITimingsService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserCacheService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IWarmupService;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.lifecycle.RegisterFactoryEvent;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

@ImplementedBy(NucleusServiceCollection.class)
public interface INucleusServiceCollection {

    IMessageProviderService messageProvider();

    IPermissionService permissionService();

    IEconomyServiceProvider economyServiceProvider();

    IWarmupService warmupService();

    ICooldownService cooldownService();

    IUserPreferenceService userPreferenceService();

    IReloadableService reloadableService();

    IPlayerOnlineService playerOnlineService();

    IStorageManager storageManager();

    ICommandMetadataService commandMetadataService();

    IPlayerDisplayNameService playerDisplayNameService();

    IConfigProvider configProvider();

    INucleusLocationService teleportService();

    ICommandElementSupplier commandElementSupplier();

    INucleusTextTemplateFactory textTemplateFactory();

    ITextFileControllerCollection textFileControllerCollection();

    IUserCacheService userCacheService();

    IPlatformService platformService();

    IModuleReporter moduleReporter();

    Injector injector();

    PluginContainer pluginContainer();

    ITextStyleService textStyleService();

    IPlayerInformationService playerInformationService();

    IConfigurateHelper configurateHelper();

    ICompatibilityService compatibilityService();

    IPlaceholderService placeholderService();

    ISchedulerService schedulerService();

    ITimingsService timingsService();

    Logger logger();

    IPropertyHolder propertyHolder();

    <I, C extends I> void registerService(Class<I> key, C service, boolean rereg);

    <I> Optional<I> getService(Class<I> key);

    <I> I getServiceUnchecked(Class<I> key);

    Path configDir();

    Supplier<Path> dataDir();

    IChatMessageFormatterService chatMessageFormatter();

    Game game();

    void registerFactories(final RegisterFactoryEvent event);
}
