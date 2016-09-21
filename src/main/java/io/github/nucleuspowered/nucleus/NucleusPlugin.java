/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import io.github.nucleuspowered.nucleus.api.service.NucleusModuleService;
import io.github.nucleuspowered.nucleus.api.service.NucleusUserLoaderService;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarmupManagerService;
import io.github.nucleuspowered.nucleus.api.service.NucleusWorldLoaderService;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.config.MessageConfig;
import io.github.nucleuspowered.nucleus.configurate.ConfigurateHelper;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProviders;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.TextFileController;
import io.github.nucleuspowered.nucleus.internal.asm.ClassTransformer;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.internal.guice.QuickStartInjectorModule;
import io.github.nucleuspowered.nucleus.internal.guice.SubInjectorModule;
import io.github.nucleuspowered.nucleus.internal.messages.ConfigMessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.ResourceMessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.qsml.ModuleRegistrationProxyService;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusLoggerProxy;
import io.github.nucleuspowered.nucleus.internal.qsml.QuickStartModuleConstructor;
import io.github.nucleuspowered.nucleus.internal.qsml.event.BaseModuleEvent;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusReloadConfigEvent;
import io.github.nucleuspowered.nucleus.signdata.SignListener;
import io.github.nucleuspowered.nucleus.util.ThrowableAction;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import uk.co.drnaylor.quickstart.enums.ConstructionPhase;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleDiscoveryException;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleLoaderException;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.nucleuspowered.nucleus.PluginInfo.*;

@Plugin(id = ID, name = NAME, version = VERSION, description = DESCRIPTION)
public class NucleusPlugin extends Nucleus {

    private Instant gameStartedTime = null;
    private boolean modulesLoaded = false;
    private boolean isErrored = false;
    private CommandsConfig commandsConfig;
    private GeneralService generalService;
    private ItemDataService itemDataService;
    private UserDataManager userDataManager;
    private WorldDataManager worldDataManager;
    private ChatUtil chatUtil;
    private NameUtil nameUtil;
    private Injector injector;
    private final SignListener signListener = new SignListener(this);
    private SubInjectorModule subInjectorModule = new SubInjectorModule();
    private List<ThrowableAction<? extends Exception>> reloadableList = Lists.newArrayList();
    private DocGenCache docGenCache = null;

    private InternalServiceManager serviceManager = new InternalServiceManager(this);
    private MessageProvider messageProvider;
    private MessageProvider commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);

    private WarmupManager warmupManager;
    private EconHelper econHelper = new EconHelper(this);
    private PermissionRegistry permissionRegistry = new PermissionRegistry();

    private DiscoveryModuleContainer moduleContainer;

    private final Map<String, TextFileController> textFileControllers = Maps.newHashMap();

    @Inject private Game game;
    private final Logger logger;
    private Path configDir;
    private Path dataDir;

    // We inject this into the constructor so we can build the config path ourselves.
    @Inject
    public NucleusPlugin(@ConfigDir(sharedRoot = true) Path configDir, Logger logger) {
        Nucleus.setNucleus(this);
        this.messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
        logger.info(messageProvider.getMessageWithFormat("startup.construct", PluginInfo.NAME));
        this.configDir = configDir.resolve(PluginInfo.ID);

        this.logger = logger;
        try {
            new ClassTransformer(logger).loadClasses();
        } catch (Exception e) {
            e.printStackTrace();
            isErrored = true;
        }
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent preInitializationEvent) {
        logger.info(messageProvider.getMessageWithFormat("startup.preinit", PluginInfo.NAME));

        dataDir = game.getSavesDirectory().resolve("nucleus");
        // Get the mandatory config files.
        try {
            Files.createDirectories(this.configDir);
            Files.createDirectories(dataDir);
            commandsConfig = new CommandsConfig(Paths.get(configDir.toString(), "commands.conf"));

            DataProviders d = new DataProviders(this);
            generalService = new GeneralService(d.getGeneralDataProvider());
            userDataManager = new UserDataManager(this, d::getUserFileDataProviders);
            worldDataManager = new WorldDataManager(this, d::getWorldFileDataProvider);
            itemDataService = new ItemDataService(d.getItemDataProvider());
            warmupManager = new WarmupManager();
            chatUtil = new ChatUtil(this);
            nameUtil = new NameUtil(this);
        } catch (Exception e) {
            isErrored = true;
            e.printStackTrace();
            return;
        }

        // We register the ModuleService NOW so that others can hook into it.
        game.getServiceManager().setProvider(this, NucleusModuleService.class, new ModuleRegistrationProxyService(this));
        game.getServiceManager().setProvider(this, NucleusWarmupManagerService.class, warmupManager);
        this.injector = Guice.createInjector(new QuickStartInjectorModule(this));
        serviceManager.registerService(WarmupManager.class, warmupManager);

        try {
            HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder();
            moduleContainer = DiscoveryModuleContainer.builder()
                    .setConstructor(new QuickStartModuleConstructor(injector))
                    .setConfigurationLoader(
                        builder.setDefaultOptions(ConfigurateHelper.setOptions(builder.getDefaultOptions()))
                            .setPath(Paths.get(configDir.toString(), "main.conf"))
                            .build())
                    .setPackageToScan(getClass().getPackage().getName() + ".modules")
                    .setLoggerProxy(new NucleusLoggerProxy(logger))
                    .setOnPreEnable(() -> {
                        runInjectorUpdate();
                        initDocGenIfApplicable();
                        Sponge.getEventManager().post(new BaseModuleEvent.AboutToEnable(this));
                    })
                    .setOnEnable(() -> {
                        runInjectorUpdate();
                        Sponge.getEventManager().post(new BaseModuleEvent.PreEnable(this));
                    })
                    .setOnPostEnable(() -> {
                        Sponge.getEventManager().post(new BaseModuleEvent.Enabled(this));
                    })
                    .build();

            moduleContainer.startDiscover();
        } catch (QuickStartModuleDiscoveryException e) {
            isErrored = true;
            e.printStackTrace();
        }
    }

    @Listener(order = Order.POST)
    public void onInit(GameInitializationEvent event) {
        if (isErrored) {
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.postinit", PluginInfo.NAME));

        // Load up the general data file now, mods should have registered items by now.
        try {
            generalService.load();
        } catch (Exception e) {
            isErrored = true;
            e.printStackTrace();
            return;
        }

        try {
            Sponge.getEventManager().post(new BaseModuleEvent.AboutToConstructEvent(this));
            logger.info(messageProvider.getMessageWithFormat("startup.moduleloading", PluginInfo.NAME));
            moduleContainer.loadModules(false);
        } catch (QuickStartModuleLoaderException.Construction | QuickStartModuleLoaderException.Enabling construction) {
            logger.info(messageProvider.getMessageWithFormat("startup.modulenotloaded", PluginInfo.NAME));
            construction.printStackTrace();
            isErrored = true;
            return;
        }

        Sponge.getEventManager().registerListeners(this, signListener);

        logger.info(messageProvider.getMessageWithFormat("startup.moduleloaded", PluginInfo.NAME));
        registerPermissions();
        modulesLoaded = true;
        Sponge.getEventManager().post(new BaseModuleEvent.Complete(this));

        // Register final services
        game.getServiceManager().setProvider(this, NucleusUserLoaderService.class, userDataManager);
        game.getServiceManager().setProvider(this, NucleusWorldLoaderService.class, worldDataManager);
        logger.info(messageProvider.getMessageWithFormat("startup.started", PluginInfo.NAME));
    }

    @Listener
    public void onGameStarted(GameStartedServerEvent event) {
        if (!isErrored) {
            Sponge.getScheduler().createSyncExecutor(this).submit(() -> this.gameStartedTime = Instant.now());
        }
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        if (!isErrored) {
            this.gameStartedTime = null;
            logger.info(messageProvider.getMessageWithFormat("startup.stopped", PluginInfo.NAME));
            saveData();
        }
    }

    @Override
    public void saveData() {
        userDataManager.saveAll();
        worldDataManager.saveAll();
        try {
            generalService.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Injector getInjector() {
        return injector;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public Path getConfigDirPath() {
        return configDir;
    }

    public Path getDataPath() {
        return dataDir;
    }

    /**
     * Gets whether the modules are loaded.
     *
     * @return Whether the modules are loaded.
     */
    public boolean areModulesLoaded() {
        return this.modulesLoaded;
    }

    @Override
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }

    @Override
    public WorldDataManager getWorldDataManager() {
        return worldDataManager;
    }

    @Override
    public void saveSystemConfig() throws IOException {
        moduleContainer.saveSystemConfig();
    }

    @Override
    public void reload() {
        try {
            moduleContainer.reloadSystemConfig();
            reloadMessages();
            commandsConfig.load();
            itemDataService.load();

            for (TextFileController tfc : textFileControllers.values()) {
                tfc.load();
            }

            for (ThrowableAction<? extends Exception> r : reloadableList) {
                r.action();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sponge.getEventManager().post(new NucleusReloadConfigEvent(this));
    }

    public void reloadMessages() {
        try {
            if (moduleContainer.getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isCustommessages()) {
                this.messageProvider = new ConfigMessageProvider(new MessageConfig(configDir.resolve("messages.conf")), ResourceMessageProvider.messagesBundle);
                this.commandMessageProvider = new ConfigMessageProvider(new MessageConfig(configDir.resolve("command-help-messages.conf")), ResourceMessageProvider.messagesBundle);
            } else {
                this.messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
                this.commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);
            }
        } catch (Exception e) {
            // On error, fallback.
            this.messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
            this.commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);
        }
    }

    @Override
    public WarmupManager getWarmupManager() {
        return warmupManager;
    }

    @Override
    public EconHelper getEconHelper() {
        return econHelper;
    }

    @Override
    public PermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }

    @Override
    public DiscoveryModuleContainer getModuleContainer() {
        return moduleContainer;
    }

    @Override
    public InternalServiceManager getInternalServiceManager() {
        return serviceManager;
    }

    @Override
    public GeneralService getGeneralService() {
        return generalService;
    }

    @Override
    public ItemDataService getItemDataService() {
        return itemDataService;
    }

    public CommandsConfig getCommandsConfig() {
        return commandsConfig;
    }

    @Override
    public NameUtil getNameUtil() {
        return nameUtil;
    }

    @Override
    public ChatUtil getChatUtil() {
        return chatUtil;
    }

    @Override
    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public MessageProvider getCommandMessageProvider() {
        return commandMessageProvider;
    }

    public <T> void preInjectorUpdate(Class<T> clazz, T instance) {
        if (injector.getExistingBinding(Key.get(clazz)) == null) {
            subInjectorModule.addInjection(clazz, instance);
        } else {
            logger.warn(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("nucleus.injector.duplicate", clazz.getName()));
        }
    }

    /**
     * Gets the {@link TextFileController}
     *
     * @param getController The ID of the {@link TextFileController}.
     * @return An {@link Optional} that might contain a {@link TextFileController}.
     */
    public Optional<TextFileController> getTextFileController(String getController) {
        return Optional.ofNullable(textFileControllers.get(getController));
    }

    public void addTextFileController(String id, Asset asset, Path file) throws IOException {
        if (!textFileControllers.containsKey(id)) {
            textFileControllers.put(id, new TextFileController(asset, file));
        }
    }

    public void registerReloadable(ThrowableAction<? extends Exception> reloadable) {
        reloadableList.add(reloadable);
    }

    public Optional<DocGenCache> getDocGenCache() {
        return Optional.ofNullable(docGenCache);
    }

    public Optional<Instant> getGameStartedTime() {
        return Optional.ofNullable(this.gameStartedTime);
    }

    public SignListener getSignListener() {
        return signListener;
    }

    public boolean isModulesLoaded() {
        return this.modulesLoaded;
    }

    private Injector runInjectorUpdate() {
        if (subInjectorModule.isEmpty()) {
            return injector;
        }

        injector = injector.createChildInjector(subInjectorModule);
        subInjectorModule = new SubInjectorModule();
        return injector;
    }

    private void initDocGenIfApplicable() {
        if (moduleContainer.getCurrentPhase() == ConstructionPhase.ENABLING) {
            // If enable-doc-gen is enabled, we init the DocGen system here.
            try {
                if (moduleContainer.getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isEnableDocGen()) {
                    docGenCache = new DocGenCache(logger);
                }
            } catch (NoModuleException | IncorrectAdapterTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerPermissions() {
        Optional<PermissionService> ops = Sponge.getServiceManager().provide(PermissionService.class);
        if (ops.isPresent()) {
            Optional<PermissionDescription.Builder> opdb = ops.get().newDescriptionBuilder(this);
            if (opdb.isPresent()) {
                Map<String, PermissionInformation> m = this.getPermissionRegistry().getPermissions();
                m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.ADMIN).forEach(k -> ops.get().newDescriptionBuilder(this).get().assign(PermissionDescription.ROLE_ADMIN, true).description(k.getValue().description).id(k.getKey()).register());
                m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.MOD).forEach(k -> ops.get().newDescriptionBuilder(this).get().assign(PermissionDescription.ROLE_STAFF, true).description(k.getValue().description).id(k.getKey()).register());
                m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.USER).forEach(k -> ops.get().newDescriptionBuilder(this).get().assign(PermissionDescription.ROLE_USER, true).description(k.getValue().description).id(k.getKey()).register());
            }
        }
    }
}
