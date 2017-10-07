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
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.typesafe.config.ConfigException;
import io.github.nucleuspowered.nucleus.api.NucleusAPITokens;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.api.service.NucleusModuleService;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarmupManagerService;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.configurate.ConfigurateHelper;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.dataservices.NameBanService;
import io.github.nucleuspowered.nucleus.dataservices.UserCacheService;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProviders;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularGeneralService;
import io.github.nucleuspowered.nucleus.internal.CatalogTypeFinalStaticProcessor;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.PreloadTasks;
import io.github.nucleuspowered.nucleus.internal.TextFileController;
import io.github.nucleuspowered.nucleus.internal.client.ClientMessageReciever;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
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
import io.github.nucleuspowered.nucleus.internal.text.NucleusTokenServiceImpl;
import io.github.nucleuspowered.nucleus.internal.text.TextParsingUtils;
import io.github.nucleuspowered.nucleus.logging.DebugLogger;
import io.github.nucleuspowered.nucleus.modules.core.CoreModule;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.modules.core.datamodules.UniqueUserCountTransientModule;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.enums.ConstructionPhase;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleDiscoveryException;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleLoaderException;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;

@Plugin(id = ID, name = NAME, version = VERSION, description = DESCRIPTION)
public class NucleusPlugin extends Nucleus {

    private static final String divider = "+------------------------------------------------------------+";
    private static final int length = divider.length() - 2;

    private final PluginContainer pluginContainer;
    private Instant gameStartedTime = null;
    private boolean modulesLoaded = false;
    private boolean hasStarted = false;
    private Throwable isErrored = null;
    private CommandsConfig commandsConfig;
    private ModularGeneralService generalService;
    private ItemDataService itemDataService;
    private UserCacheService userCacheService;
    private UserDataManager userDataManager;
    private WorldDataManager worldDataManager;
    private NameBanService nameBanService;
    private KitService kitService;
    private TextParsingUtils textParsingUtils;
    private NameUtil nameUtil;
    private final List<Reloadable> reloadableList = Lists.newArrayList();
    private DocGenCache docGenCache = null;
    private final NucleusTeleportHandler teleportHandler = new NucleusTeleportHandler();
    private NucleusTokenServiceImpl nucleusChatService;

    private final List<Text> startupMessages = Lists.newArrayList();

    private final InternalServiceManager serviceManager = new InternalServiceManager(this);
    private MessageProvider messageProvider = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
    private MessageProvider commandMessageProvider = new ResourceMessageProvider(ResourceMessageProvider.commandMessagesBundle);

    private WarmupManager warmupManager;
    private final EconHelper econHelper = new EconHelper(this);
    private final PermissionRegistry permissionRegistry = new PermissionRegistry();

    private DiscoveryModuleContainer moduleContainer;

    private final Map<String, TextFileController> textFileControllers = Maps.newHashMap();

    private final Logger logger;
    private final Path configDir;
    private final Supplier<Path> dataDir;
    private boolean isServer = false;
    private WarmupConfig warmupConfig;
    private final String versionFail;

    private boolean isDebugMode = false;
    private boolean sessionDebugMode = false;
    private int isTraceUserCreations = 0;

    @Nullable
    private static String versionCheck() {
        // So we can ensure we have the Cause Stack Manager.
        try {
            // Check the CauseStackManager exists.
            Class.forName("org.spongepowered.api.event.CauseStackManager");
        } catch (Throwable e) {
            return "CauseStackManager does not exist. Nucleus requires the CauseStackManager from API 7 to function.";
        }

        return null;
    }

    // We inject this into the constructor so we can build the config path ourselves.
    @Inject
    public NucleusPlugin(@ConfigDir(sharedRoot = true) Path configDir, Logger logger, PluginContainer container) {
        Nucleus.setNucleus(this);
        this.versionFail = versionCheck();
        this.logger = new DebugLogger(this, logger);
        this.configDir = configDir.resolve(PluginInfo.ID);
        Supplier<Path> sp;
        try {
            Path path = Sponge.getGame().getSavesDirectory().resolve("nucleus");
            sp = () -> path;
            this.isServer = true;
        } catch (NullPointerException e) {
            sp = () -> Sponge.getGame().getSavesDirectory().resolve("nucleus");
        }

        this.dataDir = sp;
        this.pluginContainer = container;
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent preInitializationEvent) {
        // Setup object mapper.
        MessageReceiver s;
        if (Sponge.getGame().isServerAvailable()) {
            s = Sponge.getServer().getConsole();
        } else {
            s = new ClientMessageReciever();
        }

        if (this.versionFail != null) {
            s.sendMessage(messageProvider.getTextMessageWithFormat("startup.nostart.compat", PluginInfo.NAME,
                    Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName(),
                    Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getVersion().orElse("unknown")));
            s.sendMessage(messageProvider.getTextMessageWithFormat("startup.nostart.compat2", this.versionFail));
            s.sendMessage(messageProvider.getTextMessageWithFormat("startup.nostart.compat3", this.versionFail));
            disable();
            return;
        }

        s.sendMessage(Text.of(TextColors.WHITE, "--------------------------"));
        s.sendMessage(messageProvider.getTextMessageWithFormat("startup.welcome", PluginInfo.NAME,
                PluginInfo.VERSION, Sponge.getPlatform().getContainer(Platform.Component.API).getVersion().orElse("unknown")));
        s.sendMessage(messageProvider.getTextMessageWithFormat("startup.welcome2"));
        s.sendMessage(messageProvider.getTextMessageWithFormat("startup.welcome3"));
        s.sendMessage(messageProvider.getTextMessageWithFormat("startup.welcome4"));
        s.sendMessage(Text.of(TextColors.WHITE, "--------------------------"));

        logger.info(messageProvider.getMessageWithFormat("startup.preinit", PluginInfo.NAME));
        Game game = Sponge.getGame();
        NucleusAPITokens.onPreInit(this);

        // Startup tasks, for the migrations I need to do.
        PreloadTasks.getPreloadTasks().forEach(x -> x.accept(this));

        // Get the mandatory config files.
        try {
            Files.createDirectories(this.configDir);
            if (this.isServer) {
                Files.createDirectories(this.dataDir.get());
            }
            commandsConfig = new CommandsConfig(Paths.get(configDir.toString(), "commands.conf"));

            DataProviders d = new DataProviders(this);
            generalService = new ModularGeneralService(d.getGeneralDataProvider());
            itemDataService = new ItemDataService(d.getItemDataProvider());
            userDataManager = new UserDataManager(this, d::getUserFileDataProviders, d::doesUserFileExist);
            worldDataManager = new WorldDataManager(this, d::getWorldFileDataProvider, d::doesWorldFileExist);
            kitService = new KitService(d.getKitsDataProvider());
            nameBanService = new NameBanService(d.getNameBanDataProvider());
            userCacheService = new UserCacheService(d.getUserCacheDataProvider());
            warmupManager = new WarmupManager();
            textParsingUtils = new TextParsingUtils(this);
            nameUtil = new NameUtil(this);

            if (this.isServer) {
                allChange(false);
            }
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
        serviceManager.registerService(WarmupManager.class, warmupManager);

        nucleusChatService = new NucleusTokenServiceImpl(this);
        serviceManager.registerService(NucleusTokenServiceImpl.class, nucleusChatService);
        Sponge.getServiceManager().setProvider(this, NucleusMessageTokenService.class, nucleusChatService);

        try {
            final String he = this.messageProvider.getMessageWithFormat("config.main-header", PluginInfo.VERSION);
            HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder();
            Optional<Asset> optionalAsset = Sponge.getAssetManager().getAsset(Nucleus.getNucleus(), "classes.json");
            DiscoveryModuleContainer.Builder db = DiscoveryModuleContainer.builder();
            if (optionalAsset.isPresent()) {
                Map<String, Map<String, List<String>>> m = new Gson().fromJson(
                        optionalAsset.get().readString(),
                        new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType()
                );

                Set<Class<?>> sc = Sets.newHashSet();
                for (String classString : m.keySet()) {
                    sc.add(Class.forName(classString));
                }

                db.setDiscoveryStrategy((string, classloader) -> sc)
                        .setConstructor(new QuickStartModuleConstructor(m));
            } else {
                db.setConstructor(new QuickStartModuleConstructor(null));
            }
            this.moduleContainer = db
                    .setConfigurationLoader(
                        builder.setDefaultOptions(
                                ConfigurateHelper.setOptions(builder.getDefaultOptions()).setHeader(he))
                            .setPath(Paths.get(configDir.toString(), "main.conf"))
                            .build())
                    .setPackageToScan(getClass().getPackage().getName() + ".modules")
                    .setLoggerProxy(new NucleusLoggerProxy(logger))
                    .setConfigurationOptionsTransformer(x -> ConfigurateHelper.setOptions(x).setHeader(he))
                    .setOnPreEnable(() -> {
                        initDocGenIfApplicable();
                        Sponge.getEventManager().post(new BaseModuleEvent.AboutToEnable(this));
                    })
                    .setOnEnable(() -> {
                        Sponge.getEventManager().post(new BaseModuleEvent.PreEnable(this));
                    })
                    .setOnPostEnable(() -> Sponge.getEventManager().post(new BaseModuleEvent.Enabled(this)))
                    .setRequireModuleDataAnnotation(true)
                    .setNoMergeIfPresent(true)
                    .setModuleConfigurationHeader(m -> {
                            StringBuilder ssb = new StringBuilder().append(divider).append("\n");
                            String name = m.getClass().getAnnotation(ModuleData.class).name();
                            int nameLength = name.length() + 2;
                            int dashes = (length - nameLength) / 2;
                            ssb.append("|");
                            for (int i = 0; i < dashes; i++) {
                                ssb.append(" ");
                            }

                            ssb.append(" ").append(name).append(" ");
                            for (int i = 0; i < dashes; i++) {
                                ssb.append(" ");
                            }

                            if (length > dashes * 2 + nameLength) {
                                ssb.append(" ");
                            }

                            return ssb.append("|").append("\n").append(divider).toString();
                    })
                    .setModuleConfigSectionName("-modules")
                    .setModuleConfigSectionDescription(this.messageProvider.getMessageWithFormat("config.module-desc"))
                    .setModuleDescriptionHandler(m -> this.messageProvider.getMessageWithFormat("config.module." +
                            m.getAnnotation(ModuleData.class).id().toLowerCase() + ".desc"))
                    .build();

            moduleContainer.startDiscover();
        } catch (Exception e) {
            isErrored = e;
            disable();
            e.printStackTrace();
        }
    }

    @Listener(order = Order.FIRST)
    public void onInit(GameInitializationEvent event) {
        if (isErrored != null) {
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.init", PluginInfo.NAME));

        try {
            CatalogTypeFinalStaticProcessor.setEventContexts();
        } catch (Exception e) {
            isErrored = e;
            disable();
            e.printStackTrace();
        }
    }

    @Listener(order = Order.FIRST)
    public void onPostInit(GamePostInitializationEvent event) {
        if (isErrored != null) {
            return;
        }

        logger.info(messageProvider.getMessageWithFormat("startup.postinit", PluginInfo.NAME));

        // Load up the general data files now, mods should have registered items by now.
        try {
            // Reloadable so that we can update the serialisers.
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
            moduleContainer.loadModules(true);

            if (moduleContainer.getConfigAdapterForModule("core", CoreConfigAdapter.class).getNodeOrDefault().isErrorOnStartup()) {
                throw new IllegalStateException("In main.conf, core.simulate-error-on-startup is set to TRUE. Remove this config entry to allow Nucleus to start. Simulating error and disabling Nucleus.");
            }

            this.isDebugMode = moduleContainer.getConfigAdapterForModule(CoreModule.ID, CoreConfigAdapter.class).getNodeOrDefault().isDebugmode();
            this.isTraceUserCreations = moduleContainer.getConfigAdapterForModule(CoreModule.ID, CoreConfigAdapter.class).getNodeOrDefault()
                    .traceUserCreations();
        } catch (Throwable construction) {
            logger.info(messageProvider.getMessageWithFormat("startup.modulenotloaded", PluginInfo.NAME));
            construction.printStackTrace();
            disable();
            isErrored = construction;
            return;
        }

        // Register a reloadable.
        CommandPermissionHandler.onReload();
        registerReloadable(CommandPermissionHandler::onReload);
        getDocGenCache().ifPresent(x -> x.addTokenDocs(nucleusChatService.getNucleusTokenParser().getTokenNames()));

        logger.info(messageProvider.getMessageWithFormat("startup.moduleloaded", PluginInfo.NAME));
        registerPermissions();
        modulesLoaded = true;
        Sponge.getEventManager().post(new BaseModuleEvent.Complete(this));

        logger.info(messageProvider.getMessageWithFormat("startup.completeinit", PluginInfo.NAME));
    }

    @Listener
    public void onGameAboutToStart(GameStartingServerEvent event) {
        if (!this.isServer) {
            try {
                Files.createDirectories(this.dataDir.get());
                allChange(true);
            } catch (IOException e) {
                isErrored = e;
                disable();
                e.printStackTrace();
            }
        }
    }

    private void allChange(boolean load) {
        this.generalService.changeFile();
        this.kitService.changeFile();
        this.nameBanService.changeFile();
        this.userCacheService.changeFile();

        this.userCacheService.load();
        this.nameBanService.load();
        if (load) {
            this.generalService.load();
            this.kitService.load();
        }
    }

    @Listener
    public void onGameStarting(GameStartingServerEvent event) {
        if (isErrored == null) {
            logger.info(messageProvider.getMessageWithFormat("startup.gamestart", PluginInfo.NAME));

            // Load up the general data files now, mods should have registered items by now.
            try {
                generalService.loadInternal();
                kitService.loadInternal();
            } catch (Exception e) {
                isErrored = e;
                disable();
                e.printStackTrace();
                return;
            }

            generalService.getTransient(UniqueUserCountTransientModule.class).resetUniqueUserCount();

            // Start the user cache walk if required, the user storage service is loaded at this point.
            Task.builder().async().execute(() -> userCacheService.startFilewalkIfNeeded()).submit(this);
            logger.info(messageProvider.getMessageWithFormat("startup.started", PluginInfo.NAME));
        }
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

            this.hasStarted = true;
            Sponge.getScheduler().createSyncExecutor(this).submit(() -> this.gameStartedTime = Instant.now());

            // What about perms and econ?
            List<Text> lt = Lists.newArrayList();
            boolean simplePerms = Sponge.getServiceManager().getRegistration(PermissionService.class)
                    .map(x -> Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).equals(x.getPlugin())).orElse(true);
            if (simplePerms) {
                addTri(lt, 0);
                lt.add(messageProvider.getTextMessageWithFormat("standard.line"));
                lt.add(messageProvider.getTextMessageWithFormat("standard.nopermplugin"));
                lt.add(messageProvider.getTextMessageWithFormat("standard.nopermplugin2"));
            }

            if (!Sponge.getServiceManager().isRegistered(EconomyService.class)) {
                if (lt.isEmpty()) {
                    addTri(lt, 0);
                }

                lt.add(messageProvider.getTextMessageWithFormat("standard.line"));
                lt.add(messageProvider.getTextMessageWithFormat("standard.noeconplugin"));
                lt.add(messageProvider.getTextMessageWithFormat("standard.noeconplugin2"));
            }

            if (!lt.isEmpty()) {
                lt.add(messageProvider.getTextMessageWithFormat("standard.line"));
                lt.add(messageProvider.getTextMessageWithFormat("standard.seesuggested"));
            }

            if (!this.startupMessages.isEmpty()) {
                if (lt.isEmpty()) {
                    addTri(lt, 0);
                }

                lt.add(messageProvider.getTextMessageWithFormat("standard.line"));
                lt.addAll(this.startupMessages);
                this.startupMessages.clear();
            }

            if (!lt.isEmpty()) {
                lt.add(messageProvider.getTextMessageWithFormat("standard.line"));
                ConsoleSource c = Sponge.getServer().getConsole();
                lt.forEach(c::sendMessage);
            }
        }
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        if (this.hasStarted && this.isErrored == null) {
            this.gameStartedTime = null;
            logger.info(messageProvider.getMessageWithFormat("startup.stopped", PluginInfo.NAME));
            saveData();
        }
    }

    @Override
    public void saveData() {
        userDataManager.saveAll();
        worldDataManager.saveAll();

        if (Sponge.getGame().getState().ordinal() > GameState.SERVER_ABOUT_TO_START.ordinal()) {
            try {
                generalService.save();
                nameBanService.save();
                userCacheService.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Path getConfigDirPath() {
        return configDir;
    }

    @Override
    public Path getDataPath() {
        return this.dataDir.get();
    }

    @Override
    public Supplier<Path> getDataPathSupplier() {
        return this.dataDir;
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

    @Override public UserCacheService getUserCacheService() {
        return userCacheService;
    }

    @Override
    public void saveSystemConfig() throws IOException {
        moduleContainer.saveSystemConfig();
    }

    @Override
    public synchronized boolean reload() {
        try {
            this.moduleContainer.reloadSystemConfig();
            reloadMessages();
            this.commandsConfig.load();
            this.itemDataService.load();
            this.warmupConfig = null;

            this.isDebugMode = getConfigValue(CoreModule.ID, CoreConfigAdapter.class, CoreConfig::isDebugmode).orElse(false);
            this.isTraceUserCreations = getConfigValue(CoreModule.ID, CoreConfigAdapter.class, CoreConfig::traceUserCreations).orElse(0);

            for (TextFileController tfc : textFileControllers.values()) {
                tfc.load();
            }

            fireReloadables();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void fireReloadables() throws Exception {
        for (Reloadable r : this.reloadableList) {
            r.onReload();
        }
    }

    @Override public void reloadMessages() {
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
            if (this.isDebugMode()) {
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

    @Override public WarmupConfig getWarmupConfig() {
        if (this.warmupConfig == null) {
            this.warmupConfig = getConfigValue(CoreModule.ID, CoreConfigAdapter.class, CoreConfig::getWarmupConfig).orElseGet(WarmupConfig::new);
        }

        return this.warmupConfig;
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

    @Override public boolean isModuleLoaded(String moduleId) {
        try {
            return getModuleContainer().isModuleLoaded(moduleId);
        } catch (NoModuleException e) {
            return false;
        }
    }

    @Override
    public <R extends NucleusConfigAdapter<?>> Optional<R> getConfigAdapter(String id, Class<R> configAdapterClass) {
        try {
            return Optional.of(getModuleContainer().getConfigAdapterForModule(id, configAdapterClass));
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return Optional.empty();
        }
    }

    @Override
    public InternalServiceManager getInternalServiceManager() {
        return serviceManager;
    }

    @Override public Optional<Instant> getGameStartedTime() {
        return Optional.ofNullable(this.gameStartedTime);
    }

    @Override
    public ModularGeneralService getGeneralService() {
        return generalService;
    }

    @Override
    public ItemDataService getItemDataService() {
        return itemDataService;
    }

    @Override public KitService getKitService() {
        return kitService;
    }

    @Override public NameBanService getNameBanService() { return nameBanService; }

    @Override
    public CommandsConfig getCommandsConfig() {
        return commandsConfig;
    }

    @Override
    public NameUtil getNameUtil() {
        return nameUtil;
    }

    public TextParsingUtils getTextParsingUtils() {
        return textParsingUtils;
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

    @Override public NucleusMessageTokenService getMessageTokenService() {
        return nucleusChatService;
    }

    @Override public boolean isDebugMode() {
        return this.isDebugMode || this.sessionDebugMode;
    }

    @Override public void printStackTraceIfDebugMode(Throwable throwable) {
        if (isDebugMode()) {
            throwable.printStackTrace();
        }
    }

    @Override public int traceUserCreations() {
        return this.isTraceUserCreations;
    }

    /**
     * Gets the {@link TextFileController}
     *
     * @param getController The ID of the {@link TextFileController}.
     * @return An {@link Optional} that might contain a {@link TextFileController}.
     */
    @Override public Optional<TextFileController> getTextFileController(String getController) {
        return Optional.ofNullable(textFileControllers.get(getController));
    }

    @Override public void addTextFileController(String id, Asset asset, Path file) throws IOException {
        if (!textFileControllers.containsKey(id)) {
            textFileControllers.put(id, new TextFileController(asset, file));
        }
    }

    @Override public void registerReloadable(Reloadable reloadable) {
        reloadableList.add(reloadable);
    }

    @Override public Optional<DocGenCache> getDocGenCache() {
        return Optional.ofNullable(docGenCache);
    }

    @Override public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Override public boolean isSessionDebug() {
        return this.sessionDebugMode;
    }

    @Override public void setSessionDebug(boolean debug) {
        this.sessionDebugMode = debug;
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

    @Override protected void registerPermissions() {
        Optional<PermissionService> ops = Sponge.getServiceManager().provide(PermissionService.class);
        ops.ifPresent(permissionService -> {
            Map<String, PermissionInformation> m = this.getPermissionRegistry().getPermissions();
            m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.ADMIN)
                    .filter(x -> x.getValue().isNormal)
                    .forEach(k -> permissionService.newDescriptionBuilder(this).assign(PermissionDescription.ROLE_ADMIN, true)
                            .description(k.getValue().description).id(k.getKey()).register());
            m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.MOD)
                    .filter(x -> x.getValue().isNormal)
                    .forEach(k -> permissionService.newDescriptionBuilder(this).assign(PermissionDescription.ROLE_STAFF, true)
                            .description(k.getValue().description).id(k.getKey()).register());
            m.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.USER)
                    .filter(x -> x.getValue().isNormal)
                    .forEach(k -> permissionService.newDescriptionBuilder(this).assign(PermissionDescription.ROLE_USER, true)
                            .description(k.getValue().description).id(k.getKey()).register());
        });
    }

    @Override
    public boolean isServer() {
        return this.isServer;
    }

    @Override public void addStartupMessage(Text message) {
        this.startupMessages.add(message);
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
        if (this.versionFail != null) {
            Sponge.getServer().getConsole().sendMessages(getIncorrectVersion());
        } else {
            Sponge.getServer().getConsole().sendMessages(getErrorMessage());
        }
    }

    private List<Text> getIncorrectVersion() {
        List<Text> messages = Lists.newArrayList();
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        messages.add(Text.of(TextColors.RED, "-   NUCLEUS FAILED TO LOAD   -"));
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        addX(messages, 7);
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        messages.add(Text.of(TextColors.RED, "-  INCORRECT SPONGE VERSION  -"));
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        messages.add(Text.EMPTY);
        messages.add(Text.of(TextColors.RED, "You are running an old version of Sponge on your server - new versions of Nucleus (like this one!) "
                + "will not run."));
        messages.add(Text.of(TextColors.RED, "Nucleus has not started. Update Sponge to the latest version and try again."));
        messages.add(Text.of(TextColors.RED, "The server has been automatically whitelisted - this is to protect your server and players if you rely on some of Nucleus' functionality (such as fly states, etc.)"));
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "Reason: "));
        messages.add(Text.of(TextColors.YELLOW, this.versionFail));
        messages.add(Text.of(TextColors.RED, "------------------------------"));
        messages.add(Text.of(TextColors.YELLOW, "Current Sponge Implementation: ",
                Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getName(), ", version ",
                Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION).getVersion().orElse("unknown"), "."));
        return messages;
    }

    private void addTri(List<Text> messages, int spacing) {
        Text space = Text.of(String.join("", Collections.nCopies(spacing, " ")));

        messages.add(Text.of(space, TextColors.YELLOW, "        /\\"));
        messages.add(Text.of(space, TextColors.YELLOW, "       /  \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "      / || \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "     /  ||  \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "    /   ||   \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "   /    ||    \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "  /            \\"));
        messages.add(Text.of(space, TextColors.YELLOW, " /      **      \\"));
        messages.add(Text.of(space, TextColors.YELLOW, "------------------"));
    }

    private void addX(List<Text> messages, int spacing) {
        Text space = Text.of(String.join("", Collections.nCopies(spacing, " ")));
        messages.add(Text.of(space, TextColors.RED, "\\              /"));
        messages.add(Text.of(space, TextColors.RED, " \\            /"));
        messages.add(Text.of(space, TextColors.RED, "  \\          /"));
        messages.add(Text.of(space, TextColors.RED, "   \\        /"));
        messages.add(Text.of(space, TextColors.RED, "    \\      /"));
        messages.add(Text.of(space, TextColors.RED, "     \\    /"));
        messages.add(Text.of(space, TextColors.RED, "      \\  /"));
        messages.add(Text.of(space, TextColors.RED, "       \\/"));
        messages.add(Text.of(space, TextColors.RED, "       /\\"));
        messages.add(Text.of(space, TextColors.RED, "      /  \\"));
        messages.add(Text.of(space, TextColors.RED, "     /    \\"));
        messages.add(Text.of(space, TextColors.RED, "    /      \\"));
        messages.add(Text.of(space, TextColors.RED, "   /        \\"));
        messages.add(Text.of(space, TextColors.RED, "  /          \\"));
        messages.add(Text.of(space, TextColors.RED, " /            \\"));
        messages.add(Text.of(space, TextColors.RED, "/              \\"));
    }

    private List<Text> getErrorMessage() {
        List<Text> messages = Lists.newArrayList();
        messages.add(Text.of(TextColors.RED, "----------------------------"));
        messages.add(Text.of(TextColors.RED, "-  NUCLEUS FAILED TO LOAD  -"));
        messages.add(Text.of(TextColors.RED, "----------------------------"));
        addX(messages, 5);
        messages.add(Text.of(TextColors.RED, "----------------------------"));

        messages.add(Text.EMPTY);
        messages.add(Text.of(TextColors.RED, "Nucleus encountered an error during server start up and did not enable successfully. No commands, listeners or tasks are registered."));
        messages.add(Text.of(TextColors.RED, "The server has been automatically whitelisted - this is to protect your server and players if you rely on some of Nucleus' functionality (such as fly states, etc.)"));
        messages.add(Text.of(TextColors.RED, "The error that Nucleus encountered will be reproduced below for your convenience."));

        messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
        if (isErrored == null) {
            messages.add(Text.of(TextColors.YELLOW, "No exception was saved."));
        } else {
            Throwable exception = isErrored;
            if (exception.getCause() != null &&
                    (exception instanceof QuickStartModuleLoaderException || exception instanceof QuickStartModuleDiscoveryException)) {
                exception = exception.getCause();
            }

            // Blegh, relocations
            if (exception instanceof IOException && exception.getCause().getClass().getName().contains(ConfigException.class.getSimpleName())) {
                exception = exception.getCause();
                messages.add(Text.of(TextColors.RED, "It appears that there is an error in your configuration file! The error is: "));
                messages.add(Text.of(TextColors.RED, exception.getMessage()));
                messages.add(Text.of(TextColors.RED, "Please correct this and restart your server."));
                messages.add(Text.of(TextColors.YELLOW, "----------------------------"));
                messages.add(Text.of(TextColors.YELLOW, "(The error that was thrown is shown below)"));
            }

            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                exception.printStackTrace(pw);
                pw.flush();
                String[] stackTrace = sw.toString().split("(\r)?\n");
                for (String s : stackTrace) {
                    messages.add(Text.of(TextColors.YELLOW, s));
                }
            } catch (IOException e) {
                exception.printStackTrace();
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
        messages.add(Text.of(TextColors.YELLOW, String.format("Sponge Version: %s %s", platform.getContainer(Platform.Component.IMPLEMENTATION).getName(),
                platform.getContainer(Platform.Component.IMPLEMENTATION).getVersion().orElse("unknown"))));
        messages.add(Text.of(TextColors.YELLOW, String.format("Sponge API Version: %s %s", platform.getContainer(Platform.Component.API).getName(),
                platform.getContainer(Platform.Component.API).getVersion().orElse("unknown"))));

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
