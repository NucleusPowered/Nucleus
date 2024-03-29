/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core;

import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.api.core.event.NucleusRegisterPreferenceKeyEvent;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanner;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.core.core.CoreKeys;
import io.github.nucleuspowered.nucleus.core.core.CoreModuleProvider;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.core.services.UniqueUserService;
import io.github.nucleuspowered.nucleus.core.core.teleport.filters.NoCheckFilter;
import io.github.nucleuspowered.nucleus.core.core.teleport.filters.WallCheckFilter;
import io.github.nucleuspowered.nucleus.core.core.teleport.scanners.NoTeleportScanner;
import io.github.nucleuspowered.nucleus.core.core.teleport.scanners.VerticalTeleportScanner;
import io.github.nucleuspowered.nucleus.core.event.NucleusRegisterModuleEvent;
import io.github.nucleuspowered.nucleus.core.event.RegisterPreferenceKeyEvent;
import io.github.nucleuspowered.nucleus.core.guice.NucleusInjectorModule;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.module.IModuleProvider;
import io.github.nucleuspowered.nucleus.core.module.ModuleContainer;
import io.github.nucleuspowered.nucleus.core.module.ModuleEvent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifierFactory;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.impl.CooldownModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.impl.CostModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.impl.RequiresEconomyModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.impl.WarmupModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerReloadableWrapper;
import io.github.nucleuspowered.nucleus.core.scaffold.task.SyncTaskBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.NucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusConfigException;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusErrorHandler;
import io.leangen.geantyref.TypeToken;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.RegisterFactoryEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.placeholder.PlaceholderParser;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class NucleusCore {

    private final PluginContainer pluginContainer;
    private final Path configDirectory;
    private final Logger logger;
    private final Injector injector;
    private final IModuleProvider coreModuleProvider = new CoreModuleProvider();
    private final IPropertyHolder propertyHolder;
    private final List<Runnable> onStartedActions = new LinkedList<>();
    private final IPluginInfo pluginInfo;

    @Nullable private Path dataDirectory;

    private final INucleusServiceCollection serviceCollection;
    private final Game game;

    public NucleusCore(
            final Game game,
            final PluginContainer pluginContainer,
            final Path configDirectory,
            final Logger logger,
            final Injector injector,
            final IPluginInfo pluginInfo,
            final IPropertyHolder propertyHolder) {
        this.game = game;
        this.pluginContainer = pluginContainer;
        this.configDirectory = configDirectory;
        this.logger = logger;
        this.pluginInfo = pluginInfo;
        this.injector = injector.createChildInjector(new NucleusInjectorModule(() -> this, pluginInfo, propertyHolder));
        this.propertyHolder = propertyHolder;
        this.serviceCollection = new NucleusServiceCollection(
                this.game,
                this.injector,
                this.pluginContainer,
                this.logger,
                this.propertyHolder,
                this::getDataDirectory,
                this.configDirectory
        );
    }

    /**
     * Begin setup, read module config, load modules that are important.
     */
    public void init() throws NucleusConfigException {
        final Collection<Tuple<ModuleContainer, IModule>> tuple = this.startModuleLoading();
        this.serviceCollection.configurateHelper().complete();
        final IConfigProvider provider = this.serviceCollection.configProvider();
        try {
            provider.prepareCoreConfig(this.coreConfigurationTransformations());
        } catch (final ConfigurateException e) {
            throw new NucleusConfigException(
                    "Could not load Nucleus core config. Aborting initialisation.",
                    provider.getCoreConfigFileName(),
                    this.propertyHolder.shutdownOnError(),
                    e
            );
        }
        try {
            provider.prepareModuleConfig();
        } catch (final ConfigurateException e) {
            throw new NucleusConfigException(
                    "Could not load Nucleus module config. Aborting initialisation.",
                    provider.getModuleConfigFileName(),
                    this.propertyHolder.shutdownOnError(),
                    e
            );
        }
        this.completeModuleInit(tuple);
    }

    public INucleusServiceCollection getServiceCollection() {
        return this.serviceCollection;
    }

    public Path getDataDirectory() {
        if (this.dataDirectory == null) {
            this.dataDirectory = this.game.gameDirectory().resolve("nucleus");
            try {
                Files.createDirectories(this.dataDirectory);
            } catch (final IOException e) {
                this.logger.fatal("Could not create directory '" + this.dataDirectory.toAbsolutePath() + "'", e);
            }
        }

        return this.dataDirectory;
    }

    public Path getConfigDirectory() {
        return this.configDirectory;
    }

    @Listener
    public void establishNewRegistries(final RegisterRegistryEvent.GameScoped event) {
        try {
            event.register(this.serviceCollection.userPreferenceService().getRegistryResourceKey(), true, () -> {
                final Map<ResourceKey, NucleusUserPreferenceService.PreferenceKey<?>> factoryMap = new HashMap<>();
                factoryMap.put(CoreKeys.LOCALE_PREFERENCE_KEY.getKey(), CoreKeys.LOCALE_PREFERENCE_KEY);
                return factoryMap;
            });
            event.register(Registry.Keys.COMMAND_MODIFIER_FACTORY_KEY, false, () -> {
                final Map<ResourceKey, CommandModifierFactory> factoryMap = new HashMap<>();

                factoryMap.put(ResourceKey.resolve(CommandModifiers.HAS_COOLDOWN),
                        new CommandModifierFactory.Simple(CommandModifiers.HAS_COOLDOWN, new CooldownModifier()));
                factoryMap.put(ResourceKey.resolve(CommandModifiers.HAS_COST),
                        new CommandModifierFactory.Simple(CommandModifiers.HAS_COST, new CostModifier()));
                factoryMap.put(ResourceKey.resolve(CommandModifiers.HAS_WARMUP),
                        new CommandModifierFactory.Simple(CommandModifiers.HAS_WARMUP, new WarmupModifier()));
                factoryMap.put(ResourceKey.resolve(CommandModifiers.REQUIRE_ECONOMY),
                        new CommandModifierFactory.Simple(CommandModifiers.REQUIRE_ECONOMY, new RequiresEconomyModifier()));
                return factoryMap;
            });
            event.register(TeleportScanners.REGISTRY_KEY, true, () -> {
                final Map<ResourceKey, TeleportScanner> factoryMap = new HashMap<>();
                factoryMap.put(NoTeleportScanner.KEY, new NoTeleportScanner());
                factoryMap.put(VerticalTeleportScanner.Ascending.KEY, new VerticalTeleportScanner.Ascending());
                factoryMap.put(VerticalTeleportScanner.Descending.KEY, new VerticalTeleportScanner.Descending());
                return factoryMap;
            });
            event.register(Registry.Keys.STORAGE_REPOSITORY_KEY, true,
                    () -> Collections.singletonMap(Registry.Keys.FLAT_FILE_STORAGE_KEY, this.serviceCollection.storageManager().getFlatFileRepositoryFactory()));
        } catch (final Exception e) {
            new NucleusErrorHandler(this.pluginContainer, e, this.propertyHolder.shutdownOnError(), this.logger, this.pluginInfo)
                    .generatePrettyPrint(this.logger, Level.ERROR);
        }
    }

    @Listener
    public void onGlobalRegistryValueRegistrationEvent(final RegisterRegistryValueEvent.GameScoped event) {
        event.registry(RegistryTypes.TELEPORT_HELPER_FILTER)
                .register(NoCheckFilter.KEY, new NoCheckFilter())
                .register(WallCheckFilter.KEY, new WallCheckFilter());

        final RegisterRegistryValueEvent.RegistryStep<PlaceholderParser> placeholderParserRegistryStep =
                event.registry(RegistryTypes.PLACEHOLDER_PARSER);
        this.serviceCollection.placeholderService().getNucleusParsers().forEach((key, value) -> {
            if (!value.isDuplicate()) {
                placeholderParserRegistryStep.register(ResourceKey.of(this.pluginContainer, key), value.getParser());
            }
        });
        this.serviceCollection.logger().info("Registered placeholder parsers.");

        final NucleusRegisterPreferenceKeyEvent registerPreferenceKeyEvent = new RegisterPreferenceKeyEvent(
                this.serviceCollection.userPreferenceService().getRegistryResourceType(),
                event.cause().with(this.pluginContainer)
        );
        this.game.eventManager().post(registerPreferenceKeyEvent);
        this.serviceCollection.logger().info("Registered user preference keys.");
    }

    @Listener
    public void establishFactories(final RegisterFactoryEvent event) {
        try {
            this.serviceCollection.registerFactories(event);
        } catch (final Exception e) {
            new NucleusErrorHandler(this.pluginContainer, e, this.propertyHolder.shutdownOnError(), this.logger, this.pluginInfo)
                    .generatePrettyPrint(this.logger, Level.ERROR);
        }
    }

    @Listener(order = Order.LAST)
    public void establishCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        try {
            final ICommandMetadataService metadataService = this.serviceCollection.commandMetadataService();
            metadataService.reset(); // for clients.
            metadataService.completeRegistrationPhase(this.serviceCollection, event);
        } catch (final Exception e) {
            new NucleusErrorHandler(this.pluginContainer, e, this.propertyHolder.shutdownOnError(), this.logger, this.pluginInfo)
                    .generatePrettyPrint(this.logger, Level.ERROR);
        }
    }

    @Listener
    public void serverStarting(final StartingEngineEvent<Server> event) {
        // Setup the data directory here.
        this.resetDataPath();
        final IStorageManager manager = this.serviceCollection.storageManager();

        // in case.
        manager.saveAndInvalidateAllCaches().whenComplete((v, t) -> manager.detachAll()).join();
        manager.detachAll();
        this.dataDirectory = null;

        manager.attachAll();
        final IReloadableService reloadableService = this.serviceCollection.reloadableService();
        reloadableService.fireDataFileReloadables(this.serviceCollection);
        reloadableService.fireReloadables(this.serviceCollection);
    }

    @Listener
    public void serverStarted(final StartedEngineEvent<Server> event) {
        this.onStartedActions.forEach(Runnable::run);
        this.serviceCollection.getServiceUnchecked(UniqueUserService.class).resetUniqueUserCount();
        this.game.asyncScheduler().executor(this.pluginContainer)
                .submit(() -> this.serviceCollection.userCacheService().startFilewalkIfNeeded());
        this.serviceCollection.platformService().setGameStartedTime();
    }

    @Listener
    public void serverStopping(final StoppingEngineEvent<Server> event) {
        // Teardown data here
        final IStorageManager manager = this.serviceCollection.storageManager();
        manager.saveAndInvalidateAllCaches().whenComplete((v, t) -> manager.detachAll());
        Sponge.asyncScheduler().tasks(this.pluginContainer).forEach(ScheduledTask::cancel);
    }

    @Listener
    public void reloadConfig(final RefreshGameEvent event) {
        this.serviceCollection.configProvider().reload();
        this.serviceCollection.reloadableService().fireReloadables(this.serviceCollection);
    }

    // -- Module loading

    private LinkedList<Tuple<ModuleContainer, IModule>> startModuleLoading() {
        final io.vavr.collection.Set<ModuleContainer> initialModuleContainerCollection = io.vavr.collection.LinkedHashSet
                .ofAll(this.coreModuleProvider.getModules());

        // Now ask other plugins for their modules
        final NucleusRegisterModuleEvent event = new NucleusRegisterModuleEvent(Cause.of(EventContext.empty(), this.pluginContainer));
        Sponge.eventManager().post(event);
        final io.vavr.collection.Set<ModuleContainer> moduleContainerCollection = initialModuleContainerCollection
                .addAll(io.vavr.collection.LinkedHashSet.ofAll(event.getProviders()).flatMap(IModuleProvider::getModules));

        final LinkedList<Tuple<ModuleContainer, IModule>> modules = new LinkedList<>();
        for (final ModuleContainer container : this.filterModules(moduleContainerCollection)) {
            final IModule module;
            try {
                module = container.construct(this.injector);
            } catch (final RuntimeException e) {
                this.logger.error("Could not load module {}. Skipping...", container.getId(), e);
                continue;
            }

            module.init(this.serviceCollection);
            if (module instanceof IModule.Configurable) {
                this.registerConfigurableModule(container, (IModule.Configurable<?>) module);
            }

            modules.add(Tuple.of(container, module));
        }

        return modules;
    }

    private <T> void registerConfigurableModule(final ModuleContainer container, final IModule.Configurable<T> configurable) {
        this.serviceCollection.configurateHelper().addTypeSerialiser(configurable.moduleTypeSerializers());
        this.serviceCollection.configProvider().registerModuleConfig(
                container.getId(),
                configurable.getConfigClass(),
                configurable::createInstance,
                configurable.getTransformations());
    }

    private void completeModuleInit(final Collection<Tuple<ModuleContainer, IModule>> modules) {
        for (final Tuple<ModuleContainer, IModule> tuple : modules) {
            final IModule module = tuple.second();
            final ModuleContainer container = tuple.first();
            // listeners
            Sponge.eventManager().registerListeners(this.pluginContainer, module);

            for (final Class<? extends ListenerBase> listenerClass :
                    Objects.requireNonNull(module.getListeners(), "Module " + tuple.first().getId() + " has a null listener call.")) {
                final ListenerBase listener = this.injector.getInstance(listenerClass);
                if (listener instanceof ListenerBase.Conditional) {
                    this.serviceCollection.reloadableService().registerReloadable(new ListenerReloadableWrapper((ListenerBase.Conditional) listener));
                }

                Sponge.eventManager().registerListeners(this.pluginContainer, listener);
                if (listener instanceof IReloadableService.Reloadable) {
                    this.serviceCollection.reloadableService().registerReloadable((IReloadableService.Reloadable) listener);
                }
            }

            // commands
            this.serviceCollection.commandMetadataService().registerCommands(container.getId(), container.getName(), module.getCommands());

            // tasks
            for (final Class<? extends TaskBase> taskBaseClass : module.getAsyncTasks()) {
                final TaskBase taskBase = this.injector.getInstance(taskBaseClass);
                if (taskBase instanceof IReloadableService.Reloadable) {
                    this.serviceCollection.reloadableService().registerReloadable((IReloadableService.Reloadable) taskBase);
                }
                this.onStartedActions.add(() -> Sponge.asyncScheduler()
                        .executor(this.pluginContainer)
                        .scheduleAtFixedRate(
                                taskBase,
                                taskBase.interval().getSeconds(),
                                taskBase.interval().getSeconds(),
                                TimeUnit.SECONDS));
            }

            for (final Class<? extends SyncTaskBase> taskBaseClass : module.getSyncTasks()) {
                final SyncTaskBase taskBase = this.injector.getInstance(taskBaseClass);
                if (taskBase instanceof IReloadableService.Reloadable) {
                    this.serviceCollection.reloadableService().registerReloadable((IReloadableService.Reloadable) taskBase);
                }
                this.onStartedActions.add(() -> Sponge.server().scheduler()
                        .submit(
                                Task.builder().plugin(this.pluginContainer)
                                        .delay(taskBase.interval().getSeconds(), TimeUnit.SECONDS)
                                        .interval(taskBase.interval().getSeconds(), TimeUnit.SECONDS)
                                        .execute(taskBase)
                                        .build()
                        ));
            }

            // Player info service
            module.getInfoProvider().ifPresent(x -> this.serviceCollection.playerInformationService().registerProvider(x));

            // Register permissions in the description builder
            module.getPermissions().ifPresent(x -> this.serviceCollection.permissionService().register(container.getId(), x));
        }

        // Once everything is said and done, run post-init on the loaded modules
        modules.forEach(tuple -> tuple.second().postLoad(this.serviceCollection));
    }

    private Collection<ModuleContainer> filterModules(final Set<ModuleContainer> moduleContainers) {
        final CommentedConfigurationNode defaults = this.serviceCollection.configurateHelper().createConfigNode();
        for (final ModuleContainer moduleContainer : moduleContainers) {
            try {
                defaults.node(moduleContainer.getId()).set(ModuleState.class, ModuleState.TRUE);
            } catch (final SerializationException e) {
                // ignored
            }
        }
        final Set<String> modules = moduleContainers.map(ModuleContainer::getId);
        this.serviceCollection.moduleReporter().provideDiscoveredModules(modules.toJavaList());

        final ConfigurationLoader<CommentedConfigurationNode> moduleConfig = HoconConfigurationLoader.builder()
                .path(this.configDirectory.resolve("modules.conf"))
                .build();

        CommentedConfigurationNode node;
        try {
            node = moduleConfig.load(this.serviceCollection.configurateHelper().getOptions());
            node.mergeFrom(defaults);
            moduleConfig.save(node);
        } catch (final ConfigurateException e) {
            node = defaults;
            this.logger.error("Could not load module config. Defaulting all to TRUE.", e);
        }

        final CommentedConfigurationNode finalNode = node;
        final Set<String> containerIds = HashSet
                .ofAll(moduleContainers)
                .map(ModuleContainer::getId)
                .filter(x -> {
                    try {
                        return finalNode.node(x).get(TypeToken.get(ModuleState.class), ModuleState.TRUE).isShouldLoad();
                    } catch (final SerializationException e) {
                        return true;
                    }
                });
        final Map<String, ModuleState> stateMap = HashSet
                .ofAll(moduleContainers)
                .toJavaMap(container -> {
                    final String id = container.getId();
                    final ModuleState state = Try.of(() ->
                            container.isRequired() ? ModuleState.FORCE :
                                    finalNode.node(id).get(TypeToken.get(ModuleState.class), ModuleState.TRUE)
                    ).recover(thr -> ModuleState.TRUE).get();
                    return new Tuple2<>(id, state);
                });

        final ModuleEvent event = new ModuleEvent(
                Cause.of(EventContext.empty(), this.pluginContainer),
                containerIds.toJavaSet(),
                stateMap);
        this.game.eventManager().post(event);
        final ArrayList<ModuleContainer> containersToReturn = new ArrayList<>();
        for (final ModuleContainer moduleContainer : moduleContainers) {
            if (event.shouldLoad(moduleContainer.getId())) { // will return true if it doesn't exist - i.e. force load.
                containersToReturn.add(moduleContainer);
            }
        }
        containersToReturn.forEach(this.serviceCollection.moduleReporter()::provideEnabledModule);
        return containersToReturn;
    }

    private void resetDataPath() {
        final CoreConfig config = this.serviceCollection.configProvider().getCoreConfig();
        final String dataFileLocation = config.getDataFileLocation();
        if (dataFileLocation == null || dataFileLocation.equalsIgnoreCase("default")) {
            this.dataDirectory = null; // this will be set later.
            return;
        }

        final Path path;
        try {
            path = Paths.get(dataFileLocation);
        } catch (final InvalidPathException ex) {
            // Tell the user we're going default.
            this.dataDirectory = null;
            this.logger.error("The data path '" + dataFileLocation + "' is not a valid path. Using default data directory.");
            return;
        }

        if (!Files.isDirectory(path)) {
            // warning
            this.logger.error(this.serviceCollection.messageProvider().getMessageString("nucleus.custompath.error",
                    path.toAbsolutePath().toString()));
            this.dataDirectory = null;
            return;
        }

        final Path currentDataDir = path.resolve("nucleus");
        try {
            Files.createDirectories(currentDataDir);
            this.dataDirectory = currentDataDir;
            this.logger.info(this.serviceCollection.messageProvider().getMessageString("nucleus.custompath.info",
                    currentDataDir.toAbsolutePath().toString()));
        } catch (final IOException e) {
            this.logger.fatal("Could not create directory '{}', using default data directory.", currentDataDir.toAbsolutePath(), e);
        }

    }

    private Collection<ConfigurationTransformation> coreConfigurationTransformations() {
        return Collections.emptyList();
    }

    public enum ModuleState {

        FORCE(true),
        TRUE(true),
        FALSE(false);

        private final boolean shouldLoad;

        ModuleState(final boolean shouldLoad) {
            this.shouldLoad = shouldLoad;
        }

        public boolean isShouldLoad() {
            return this.shouldLoad;
        }
    }

}
