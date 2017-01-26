/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import static io.github.nucleuspowered.nucleus.PluginInfo.DESCRIPTION;
import static io.github.nucleuspowered.nucleus.PluginInfo.ID;
import static io.github.nucleuspowered.nucleus.PluginInfo.NAME;
import static io.github.nucleuspowered.nucleus.PluginInfo.VERSION;

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
import io.github.nucleuspowered.nucleus.configurate.ConfigurateHelper;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.dataservices.NameBanService;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProviders;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.PreloadTasks;
import io.github.nucleuspowered.nucleus.internal.TextFileController;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.internal.guice.QuickStartInjectorModule;
import io.github.nucleuspowered.nucleus.internal.guice.SubInjectorModule;
import io.github.nucleuspowered.nucleus.internal.messages.ConfigMessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.ResourceMessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.qsml.ModuleRegistrationProxyService;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusLoggerProxy;
import io.github.nucleuspowered.nucleus.internal.qsml.QuickStartModuleConstructor;
import io.github.nucleuspowered.nucleus.internal.qsml.event.BaseModuleEvent;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.internal.text.TokenHandler;
import io.github.nucleuspowered.nucleus.logging.DebugLogger;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusReloadConfigEvent;
import io.github.nucleuspowered.nucleus.spongedata.NucleusSpongeDataRegistration;
import io.github.nucleuspowered.nucleus.util.ThrowableAction;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.enums.ConstructionPhase;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleDiscoveryException;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

@Plugin(id = ID, name = NAME, version = VERSION, description = DESCRIPTION)
public class NucleusPlugin extends Nucleus {

    private final PluginContainer pluginContainer;
    private Instant gameStartedTime = null;
    private boolean modulesLoaded = false;
    private Throwable isErrored = null;
    private CommandsConfig commandsConfig;
    private GeneralService generalService;
    private ItemDataService itemDataService;
    private UserDataManager userDataManager;
    private WorldDataManager worldDataManager;
    private NameBanService nameBanService;
    private KitService kitService;
    private ChatUtil chatUtil;
    private NameUtil nameUtil;
    private Injector injector;
    private SubInjectorModule subInjectorModule = new SubInjectorModule();
    private List<ThrowableAction<? extends Exception>> reloadableList = Lists.newArrayList();
    private DocGenCache docGenCache = null;
    private NucleusTeleportHandler teleportHandler = new NucleusTeleportHandler();
    private final TokenHandler tokenHandler = new TokenHandler(this);

    private InternalServiceManager serviceManager = new InternalServiceManager(this);
    private MessageProvider messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
    private MessageProvider commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);

    private WarmupManager warmupManager;
    private EconHelper econHelper = new EconHelper(this);
    private PermissionRegistry permissionRegistry = new PermissionRegistry();

    private DiscoveryModuleContainer moduleContainer;

    private final Map<String, TextFileController> textFileControllers = Maps.newHashMap();

    private final Logger logger;
    private Path configDir;
    private Path dataDir;
    @Nullable private MixinConfigProxy mixinConfigProxy = null;

    // We inject this into the constructor so we can build the config path ourselves.
    @Inject
    public NucleusPlugin(@ConfigDir(sharedRoot = true) Path configDir, Logger logger, PluginContainer container) {
        Nucleus.setNucleus(this);
        this.configDir = configDir.resolve(PluginInfo.ID);
        this.dataDir = Sponge.getGame().getSavesDirectory().resolve("nucleus");
        this.logger = new DebugLogger(this, logger);
        this.pluginContainer = container;
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent preInitializationEvent) {
        logger.info(messageProvider.getMessageWithFormat("startup.preinit", PluginInfo.NAME));
        Game game = Sponge.getGame();

        try {
            Class.forName("io.github.nucleuspowered.nucleus.mixins.NucleusMixinSpongePlugin");
            this.mixinConfigProxy = new MixinConfigProxy();
            logger.info(messageProvider.getMessageWithFormat("startup.mixins-available"));
        } catch (ClassNotFoundException e) {
            logger.info(messageProvider.getMessageWithFormat("startup.mixins-notavailable"));
        }

        // Startup tasks, for the migrations I need to do.
        PreloadTasks.getPreloadTasks().forEach(x -> x.accept(this));

        // Get the mandatory config files.
        try {
            Files.createDirectories(this.configDir);
            Files.createDirectories(dataDir);
            commandsConfig = new CommandsConfig(Paths.get(configDir.toString(), "commands.conf"));

            DataProviders d = new DataProviders(this);
            generalService = new GeneralService(d.getGeneralDataProvider());
            itemDataService = new ItemDataService(d.getItemDataProvider());
            userDataManager = new UserDataManager(this, d::getUserFileDataProviders);
            worldDataManager = new WorldDataManager(this, d::getWorldFileDataProvider);
            kitService = new KitService(d.getKitsDataProvider());
            nameBanService = new NameBanService(d.getNameBanDataProvider());
            warmupManager = new WarmupManager();
            chatUtil = new ChatUtil(this);
            nameUtil = new NameUtil(this);
        } catch (Exception e) {
            isErrored = e;
            disable();
            e.printStackTrace();
            return;
        }

        PreloadTasks.getPreloadTasks2().forEach(x -> x.accept(this));

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
                    .setConfigurationOptionsTransformer(ConfigurateHelper::setOptions)
                    .setOnPreEnable(() -> {
                        runInjectorUpdate();
                        initDocGenIfApplicable();
                        Sponge.getEventManager().post(new BaseModuleEvent.AboutToEnable(this));
                    })
                    .setOnEnable(() -> {
                        runInjectorUpdate();
                        Sponge.getEventManager().post(new BaseModuleEvent.PreEnable(this));
                    })
                    .setOnPostEnable(() -> Sponge.getEventManager().post(new BaseModuleEvent.Enabled(this)))
                    .build();

            moduleContainer.startDiscover();
        } catch (QuickStartModuleDiscoveryException e) {
            isErrored = e;
            disable();
            e.printStackTrace();
        }
    }

    @Listener
    public void onInit(GameInitializationEvent event) {
        if (isErrored != null) {
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.init.start", PluginInfo.NAME));
        NucleusSpongeDataRegistration.register();
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        if (isErrored != null) {
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.postinit", PluginInfo.NAME));

        // Load up the general data files now, mods should have registered items by now.
        try {
            generalService.loadInternal();
            kitService.loadInternal();

            // Reload so that we can update the serialisers.
            moduleContainer.reloadSystemConfig();
        } catch (Exception e) {
            isErrored = e;
            disable();
            e.printStackTrace();
            return;
        }

        try {
            Sponge.getEventManager().post(new BaseModuleEvent.AboutToConstructEvent(this));
            logger.info(messageProvider.getMessageWithFormat("startup.moduleloading", PluginInfo.NAME));
            moduleContainer.loadModules(false);

            if (moduleContainer.getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isErrorOnStartup()) {
                throw new IllegalStateException("In main.conf, core.simulate-error-on-startup is set to TRUE. Remove this config entry to allow Nucleus to start. Simulating error and disabling Nucleus.");
            }
        } catch (Throwable construction) {
            logger.info(messageProvider.getMessageWithFormat("startup.modulenotloaded", PluginInfo.NAME));
            construction.printStackTrace();
            disable();
            isErrored = construction;
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.moduleloaded", PluginInfo.NAME));
        registerPermissions();
        modulesLoaded = true;
        Sponge.getEventManager().post(new BaseModuleEvent.Complete(this));

        // Register final services
        Game game = Sponge.getGame();
        game.getServiceManager().setProvider(this, NucleusUserLoaderService.class, userDataManager);
        game.getServiceManager().setProvider(this, NucleusWorldLoaderService.class, worldDataManager);
        logger.info(messageProvider.getMessageWithFormat("startup.started", PluginInfo.NAME));
    }

    @Listener
    public void onGameStarted(GameStartedServerEvent event) {
        if (isErrored == null) {
            try {
                // Save any additions.
                moduleContainer.refreshSystemConfig();
                fireReloadables();
            } catch (Throwable e) {
                isErrored = e;
                disable();
                errorOnStartup();
                return;
            }

            Sponge.getScheduler().createSyncExecutor(this).submit(() -> this.gameStartedTime = Instant.now());
        }
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        if (isErrored == null) {
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
            nameBanService.save();
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
    public synchronized boolean reload() {
        try {
            moduleContainer.reloadSystemConfig();
            reloadMessages();
            commandsConfig.load();
            itemDataService.load();

            for (TextFileController tfc : textFileControllers.values()) {
                tfc.load();
            }

            fireReloadables();

            Sponge.getEventManager().post(new NucleusReloadConfigEvent(this));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void fireReloadables() throws Exception {
        for (ThrowableAction<? extends Exception> r : reloadableList) {
            r.action();
        }
    }

    public void reloadMessages() {
        try {
            if (moduleContainer.getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isCustommessages()) {
                this.messageProvider = new ConfigMessageProvider(configDir.resolve("messages.conf"), ResourceMessageProvider.messagesBundle);
                this.commandMessageProvider = new ConfigMessageProvider(configDir.resolve("command-help-messages.conf"), ResourceMessageProvider.commandMessagesBundle);
            } else {
                this.messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
                this.commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);
            }
        } catch (Exception e) {
            // On error, fallback.
            logger.warn("Could not load custom messages file. Falling back.");
            try {
                if (getModuleContainer().getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isDebugmode()) {
                    e.printStackTrace();
                }
            } catch (NoModuleException | IncorrectAdapterTypeException e1) {
                e.printStackTrace();
            }

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
    public <R extends NucleusConfigAdapter<?>> Optional<R> getConfigAdapter(String id, Class<R> configAdapterClass) {
        try {
            return Optional.of(getModuleContainer().getConfigAdapterForModule(id, configAdapterClass));
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            if (isDebugMode()) {
                e.printStackTrace();
            }

            return Optional.empty();
        }
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

    public KitService getKitService() {
        return kitService;
    }

    public NameBanService getNameBanService() { return nameBanService; }

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

    @Override
    public NucleusTeleportHandler getTeleportHandler() {
        return teleportHandler;
    }

    public TokenHandler getTokenHandler() {
        return tokenHandler;
    }

    @Override public boolean isDebugMode() {
        try {
            return this.getModuleContainer().getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isDebugmode();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return true;
        }
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

    public Optional<MixinConfigProxy> getMixinConfigIfAvailable() {
        return Optional.ofNullable(this.mixinConfigProxy);
    }

    public PluginContainer getPluginContainer() {
        return pluginContainer;
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

    private void disable() {
        // Disable everything, just in case. Thanks to pie-flavor: https://forums.spongepowered.org/t/disable-plugin-disable-itself/15831/8
        Sponge.getEventManager().unregisterPluginListeners(this);
        Sponge.getCommandManager().getOwnedBy(this).forEach(Sponge.getCommandManager()::removeMapping);
        Sponge.getScheduler().getScheduledTasks(this).forEach(Task::cancel);

        // Re-register this to warn people about the error.
        Sponge.getEventManager().registerListener(this, GameStartedServerEvent.class, e -> errorOnStartup());
    }

    private void errorOnStartup() {
        Sponge.getServer().setHasWhitelist(true);
        Sponge.getServer().getConsole().sendMessages(getErrorMessage());
    }

    private List<Text> getErrorMessage() {
        List<Text> messages = Lists.newArrayList();
        messages.add(Text.of(TextColors.RED, "----------------------------"));
        messages.add(Text.of(TextColors.RED, "-  NUCLEUS FAILED TO LOAD  -"));
        messages.add(Text.of(TextColors.RED, "----------------------------"));
        messages.add(Text.EMPTY);
        messages.add(Text.of(TextColors.RED, "Nucleus encountered an error during server start up and did not enable successfully. No commands, listeners or tasks are registered."));
        messages.add(Text.of(TextColors.RED, "The server has been automatically whitelisted - this is to protect your server and players if you rely on some of Nucleus' functionality (such as fly states, etc.)"));
        messages.add(Text.of(TextColors.RED, "The error that Nucleus encountered will be reproduced below for your convenience."));

        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        if (isErrored == null) {
            messages.add(Text.of(TextColors.YELLOW, "No exception was saved."));
        } else {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                isErrored.printStackTrace(pw);
                pw.flush();
                String[] stackTrace = sw.toString().split("(\r)?\n");
                for (String s : stackTrace) {
                    messages.add(Text.of(TextColors.YELLOW, s));
                }
            } catch (IOException e) {
                isErrored.printStackTrace();
            }
        }

        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        messages.add(Text.of(TextColors.RED, "If this error persists, check your configuration files and ensure that you have the latest version of Nucleus which matches the current version of the Sponge API."));
        messages.add(Text.of(TextColors.RED, "If you do, please report this error to the Nucleus team at https://github.com/NucleusPowered/Nucleus/issues"));
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "Server Information"));
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "Nucleus version: " + PluginInfo.VERSION + ", (Git: " + PluginInfo.GIT_HASH + ")"));

        Platform platform = Sponge.getPlatform();
        messages.add(Text.of(TextColors.YELLOW, "Minecraft version: " + platform.getMinecraftVersion().getName()));
        messages.add(Text.of(TextColors.YELLOW, String.format("Sponge Version: %s %s", platform.getImplementation().getName(), platform.getImplementation().getVersion().orElse("unknown"))));
        messages.add(Text.of(TextColors.YELLOW, String.format("Sponge API Version: %s %s", platform.getApi().getName(), platform.getApi().getVersion().orElse("unknown"))));

        messages.add(Text.EMPTY);
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "Installed Plugins"));
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        Sponge.getPluginManager().getPlugins().forEach(x -> messages.add(Text.of(TextColors.YELLOW, x.getName() + " (" + x.getId() + ") version " + x.getVersion().orElse("unknown"))));

        messages.add(Text.EMPTY);
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "- END NUCLEUS ERROR REPORT -"));
        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        return messages;
    }
}
