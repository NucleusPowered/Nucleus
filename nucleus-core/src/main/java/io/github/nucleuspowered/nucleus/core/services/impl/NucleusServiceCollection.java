/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.guice.ConfigDirectory;
import io.github.nucleuspowered.nucleus.core.guice.DataDirectory;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandElementSupplier;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICompatibilityService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICooldownService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IDocumentationGenerationService;
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
import io.github.nucleuspowered.nucleus.core.util.LazyLoad;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.lifecycle.RegisterFactoryEvent;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class NucleusServiceCollection implements INucleusServiceCollection {

    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();
    private final Map<Class<?>, Object> apiFactories = new HashMap<>();

    private final Supplier<IMessageProviderService> messageProviderService;
    private final Supplier<IEconomyServiceProvider> economyServiceProvider;
    private final Supplier<IWarmupService> warmupService;
    private final Supplier<ICooldownService> cooldownService;
    private final Supplier<IPermissionService> permissionCheckService;
    private final Supplier<IReloadableService> reloadableService;
    private final Supplier<IPlayerOnlineService> playerOnlineService;
    private final Supplier<IStorageManager> storageManager;
    private final Supplier<IUserPreferenceService> userPreferenceService;
    private final Supplier<ICommandMetadataService> commandMetadataService;
    private final Supplier<IPlayerDisplayNameService> playerDisplayNameService;
    private final Supplier<IConfigProvider> moduleConfigProvider;
    private final Supplier<INucleusLocationService> nucleusTeleportServiceProvider;
    private final Supplier<ICommandElementSupplier> commandElementSupplierProvider;
    private final Supplier<INucleusTextTemplateFactory> nucleusTextTemplateFactoryProvider;
    private final Supplier<ITextFileControllerCollection> textFileControllerCollectionProvider;
    private final Supplier<IUserCacheService> userCacheServiceProvider;
    private final Supplier<IPlayerInformationService> playerInformationServiceProvider;
    private final Supplier<IConfigurateHelper> configurateHelperProvider;
    private final Supplier<IPlatformService> platformServiceProvider;
    private final Supplier<ICompatibilityService> compatibilityServiceProvider;
    private final Supplier<IChatMessageFormatterService> chatMessageFormatterProvider;
    private final Supplier<IPlaceholderService> placeholderServiceProvider;
    private final Supplier<IDocumentationGenerationService> documentationGenerationServiceProvider;
    private final Supplier<ITextStyleService> textStyleServiceProvider;
    private final Supplier<IModuleReporter> moduleReporterSupplier;
    private final Supplier<ISchedulerService> schedulerServiceProvider;
    private final Supplier<ITimingsService> timingsServiceProvider;
    private final Game game;
    private final Injector injector;
    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Supplier<Path> dataDir;
    private final Path configPath;

    @Inject
    public NucleusServiceCollection(
            final Game game,
            final Injector injector,
            final PluginContainer pluginContainer,
            final Logger logger,
            @DataDirectory final Supplier<Path> dataPath,
            @ConfigDirectory final Path configPath) {
        this.messageProviderService = new LazyLoad<>(this, injector, IMessageProviderService.class);
        this.economyServiceProvider = new LazyLoad<>(this, injector, IEconomyServiceProvider.class);
        this.warmupService = new LazyLoad<>(this, injector, IWarmupService.class);
        this.cooldownService = new LazyLoad<>(this, injector, ICooldownService.class);
        this.userPreferenceService = new LazyLoad<>(this, injector, IUserPreferenceService.class);
        this.permissionCheckService = new LazyLoad<>(this, injector, IPermissionService.class);
        this.reloadableService = new LazyLoad<>(this, injector, IReloadableService.class);
        this.playerOnlineService = new LazyLoad<>(this, injector, IPlayerOnlineService.class);
        this.storageManager = new LazyLoad<>(this, injector, IStorageManager.class);
        this.commandMetadataService = new LazyLoad<>(this, injector, ICommandMetadataService.class);
        this.playerDisplayNameService = new LazyLoad<>(this, injector, IPlayerDisplayNameService.class);
        this.moduleConfigProvider = new LazyLoad<>(this, injector, IConfigProvider.class);
        this.nucleusTeleportServiceProvider = new LazyLoad<>(this, injector, INucleusLocationService.class);
        this.textStyleServiceProvider = new LazyLoad<>(this, injector, ITextStyleService.class);
        this.commandElementSupplierProvider = new LazyLoad<>(this, injector, ICommandElementSupplier.class);
        this.nucleusTextTemplateFactoryProvider = new LazyLoad<>(this, injector, INucleusTextTemplateFactory.class);
        this.textFileControllerCollectionProvider = new LazyLoad<>(this, injector, ITextFileControllerCollection.class);
        this.userCacheServiceProvider = new LazyLoad<>(this, injector, IUserCacheService.class);
        this.playerInformationServiceProvider = new LazyLoad<>(this, injector, IPlayerInformationService.class);
        this.configurateHelperProvider = new LazyLoad<>(this, injector, IConfigurateHelper.class);
        this.platformServiceProvider = new LazyLoad<>(this, injector, IPlatformService.class);
        this.compatibilityServiceProvider = new LazyLoad<>(this, injector, ICompatibilityService.class);
        this.chatMessageFormatterProvider = new LazyLoad<>(this, injector, IChatMessageFormatterService.class);
        this.placeholderServiceProvider = new LazyLoad<>(this, injector, IPlaceholderService.class);
        this.documentationGenerationServiceProvider = new LazyLoad<>(this, injector, IDocumentationGenerationService.class);
        this.moduleReporterSupplier = new LazyLoad<>(this, injector, IModuleReporter.class);
        this.schedulerServiceProvider = new LazyLoad<>(this, injector, ISchedulerService.class);
        this.timingsServiceProvider = new LazyLoad<>(this, injector, ITimingsService.class);
        this.injector = injector;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.dataDir = dataPath;
        this.configPath = configPath;
        this.game = game;
    }

    @Override
    public IMessageProviderService messageProvider() {
        return this.messageProviderService.get();
    }

    @Override
    public IPermissionService permissionService() {
        return this.permissionCheckService.get();
    }

    @Override
    public IEconomyServiceProvider economyServiceProvider() {
        return this.economyServiceProvider.get();
    }

    @Override
    public IWarmupService warmupService() {
        return this.warmupService.get();
    }

    @Override
    public ICooldownService cooldownService() {
        return this.cooldownService.get();
    }

    @Override
    public IUserPreferenceService userPreferenceService() {
        return this.userPreferenceService.get();
    }

    @Override
    public IReloadableService reloadableService() {
        return this.reloadableService.get();
    }

    @Override public IPlayerOnlineService playerOnlineService() {
        return this.playerOnlineService.get();
    }

    @Override public IStorageManager storageManager() {
        return this.storageManager.get();
    }

    @Override public ICommandMetadataService commandMetadataService() {
        return this.commandMetadataService.get();
    }

    @Override public IPlayerDisplayNameService playerDisplayNameService() {
        return this.playerDisplayNameService.get();
    }

    @Override public IConfigProvider configProvider() {
        return this.moduleConfigProvider.get();
    }

    @Override public INucleusLocationService teleportService() {
        return this.nucleusTeleportServiceProvider.get();
    }

    @Override public ICommandElementSupplier commandElementSupplier() {
        return this.commandElementSupplierProvider.get();
    }

    @Override public INucleusTextTemplateFactory textTemplateFactory() {
        return this.nucleusTextTemplateFactoryProvider.get();
    }

    @Override public ITextFileControllerCollection textFileControllerCollection() {
        return this.textFileControllerCollectionProvider.get();
    }

    @Override public ITextStyleService textStyleService() {
        return this.textStyleServiceProvider.get();
    }

    @Override public IPlayerInformationService playerInformationService() {
        return this.playerInformationServiceProvider.get();
    }

    @Override public IConfigurateHelper configurateHelper() {
        return this.configurateHelperProvider.get();
    }

    @Override public ICompatibilityService compatibilityService() {
        return this.compatibilityServiceProvider.get();
    }

    @Override public IPlaceholderService placeholderService() {
        return this.placeholderServiceProvider.get();
    }

    @Override public ISchedulerService schedulerService() {
        return this.schedulerServiceProvider.get();
    }

    @Override public ITimingsService timingsService() {
        return this.timingsServiceProvider.get();
    }

    @Override public IUserCacheService userCacheService() {
        return this.userCacheServiceProvider.get();
    }

    @Override public IPlatformService platformService() {
        return this.platformServiceProvider.get();
    }

    @Override public IChatMessageFormatterService chatMessageFormatter() {
        return this.chatMessageFormatterProvider.get();
    }

    @Override public IDocumentationGenerationService documentationGenerationService() {
        return this.documentationGenerationServiceProvider.get();
    }

    @Override public IModuleReporter moduleReporter() {
        return this.moduleReporterSupplier.get();
    }

    @Override
    public Injector injector() {
        return this.injector;
    }

    @Override
    public PluginContainer pluginContainer() {
        return this.pluginContainer;
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public <I, C extends I> void registerService(final Class<I> key, final C service, final boolean rereg) {
        if (!rereg && (this.instances.containsKey(key) || this.suppliers.containsKey(key))) {
            return;
        }

        this.suppliers.remove(key);
        this.instances.put(key, service);
        if (service.getClass().isAnnotationPresent(APIService.class)) {
            this.apiFactories.put(service.getClass().getAnnotation(APIService.class).value(), service);
        }
        if (service instanceof IReloadableService.Reloadable) {
            this.reloadableService.get().registerReloadable((IReloadableService.Reloadable) service);
        }
        if (service instanceof IReloadableService.DataLocationReloadable) {
            this.reloadableService.get().registerDataFileReloadable((IReloadableService.DataLocationReloadable) service);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public <I> Optional<I> getService(final Class<I> key) {
        if (this.instances.containsKey(key)) {
            return Optional.of((I) this.instances.get(key));
        } else if (this.suppliers.containsKey(key)) {
            return Optional.of((I) this.suppliers.get(key).get());
        }

        return Optional.empty();
    }

    @Override @SuppressWarnings("unchecked")
    public <I> I getServiceUnchecked(final Class<I> key) {
        if (this.instances.containsKey(key)) {
            return (I) this.instances.get(key);
        } else if (this.suppliers.containsKey(key)) {
            return (I) this.suppliers.get(key).get();
        }

        throw new NoSuchElementException(key.getName());
    }

    @Override public Path configDir() {
        return this.configPath;
    }

    @Override public Supplier<Path> dataDir() {
        return this.dataDir;
    }

    @Override
    public Game game() {
        return this.game;
    }

    @Override public void registerFactories(final RegisterFactoryEvent event) {
        for (final Map.Entry<Class<?>, Object> entry : this.apiFactories.entrySet()) {
            this.registerFactory(event, entry.getKey(), entry.getValue());
        }
    }

    private <T> void registerFactory(final RegisterFactoryEvent event, final Class<T> c, final Object instance) {
        event.register(c, c.cast(instance));
    }
}
