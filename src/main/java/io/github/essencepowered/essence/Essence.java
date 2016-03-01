/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.exceptions.ModulesLoadedException;
import io.github.essencepowered.essence.api.exceptions.UnremovableModuleException;
import io.github.essencepowered.essence.api.service.*;
import io.github.essencepowered.essence.config.AbstractConfig;
import io.github.essencepowered.essence.config.CommandsConfig;
import io.github.essencepowered.essence.config.MainConfig;
import io.github.essencepowered.essence.config.WarpsConfig;
import io.github.essencepowered.essence.internal.ConfigMap;
import io.github.essencepowered.essence.internal.EconHelper;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.PluginSystemsLoader;
import io.github.essencepowered.essence.internal.guice.QuickStartInjectorModule;
import io.github.essencepowered.essence.internal.services.*;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.github.essencepowered.essence.PluginInfo.*;

@Plugin(id = ID, name = NAME, version = INFORMATIVE_VERSION)
public class Essence {

    private ModuleRegistration moduleRegistration;
    private boolean modulesLoaded = false;
    private boolean isErrored = false;
    private final ConfigMap configMap = new ConfigMap();
    private UserConfigLoader configLoader;
    private Injector injector;
    private MessageHandler messageHandler = new MessageHandler();
    private MailHandler mailHandler;
    private JailHandler jailHandler;
    private WarmupManager warmupManager;
    private TeleportHandler tpHandler = new TeleportHandler(this);
    private EconHelper econHelper = new EconHelper(this);
    private PermissionRegistry permissionRegistry = new PermissionRegistry();

    private AFKHandler afkHandler = new AFKHandler();

    @Inject private Game game;
    @Inject private Logger logger;
    @Inject @DefaultConfig(sharedRoot = false) private Path path;
    @Inject @ConfigDir(sharedRoot = false) private Path configDir;
    private Path dataDir;

    @Listener
    public void onPreInit(GamePreInitializationEvent preInitializationEvent) {
        dataDir = game.getSavesDirectory().resolve("essence");
        // Get the mandatory config files.
        try {
            Files.createDirectories(dataDir);
            configMap.putConfig(ConfigMap.MAIN_CONFIG, new MainConfig(path));
            configMap.putConfig(ConfigMap.COMMANDS_CONFIG, new CommandsConfig(Paths.get(configDir.toString(), "commands.conf")));
            configLoader = new UserConfigLoader(this);
            moduleRegistration = new ModuleRegistration(this);
            warmupManager = new WarmupManager();
        } catch (IOException | ObjectMappingException e) {
            isErrored = true;
            e.printStackTrace();
            return;
        }

        // We register the ModuleService NOW so that others can hook into it.
        game.getServiceManager().setProvider(this, EssenceModuleService.class, moduleRegistration);
        game.getServiceManager().setProvider(this, EssenceWarmupManagerService.class, warmupManager);
        this.injector = Guice.createInjector(new QuickStartInjectorModule(this, configLoader));
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        if (isErrored) {
            return;
        }

        Set<PluginModule> modules = moduleRegistration.getModulesToLoad();

        // Load the following services only if necessary.
        if (modules.contains(PluginModule.WARPS)) {
            try {
                configMap.putConfig(ConfigMap.WARPS_CONFIG, new WarpsConfig(Paths.get(dataDir.toString(), "warp.json")));

                // Put the warp service into the service manager.
                game.getServiceManager().setProvider(this, EssenceWarpService.class, configMap.getConfig(ConfigMap.WARPS_CONFIG).get());
            } catch (IOException | ObjectMappingException ex) {
                try {
                    moduleRegistration.removeModule(PluginModule.WARPS);
                } catch (ModulesLoadedException | UnremovableModuleException e) {
                    // Nope.
                }

                logger.warn("Could not load the warp module for the reason below.");
                ex.printStackTrace();
            }
        }

        if (modules.contains(PluginModule.JAILS)) {
            try {
                configMap.putConfig(ConfigMap.JAILS_CONFIG, new WarpsConfig(Paths.get(dataDir.toString(), "jails.json")));

                jailHandler = new JailHandler(this);
                game.getServiceManager().setProvider(this, EssenceJailService.class, jailHandler);
            } catch (IOException | ObjectMappingException ex) {
                try {
                    moduleRegistration.removeModule(PluginModule.JAILS);
                } catch (ModulesLoadedException | UnremovableModuleException e) {
                    // Nope.
                }

                logger.warn("Could not load the jail module for the reason below.");
                ex.printStackTrace();
            }
        }

        if (modules.contains(PluginModule.MAILS)) {
            mailHandler = new MailHandler(game, this);
            game.getServiceManager().setProvider(this, EssenceMailService.class, mailHandler);
        }

        modulesLoaded = true;

        // Register commands, events and runnables.
        try {
            new PluginSystemsLoader(this).load();
        } catch (IOException e) {
            e.printStackTrace();
            isErrored = true;
            return;
        }

        // Register services
        game.getServiceManager().setProvider(this, EssenceUserService.class, configLoader);

        // Start tasks, save every thirty seconds
        game.getScheduler().createTaskBuilder().async().name("Essence Cleanup Task").delay(30, TimeUnit.SECONDS).interval(30, TimeUnit.SECONDS)
            .execute(() -> {
                this.getUserLoader().purgeNotOnline();
                this.configLoader.saveAll();
            }).submit(this);
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        if (!isErrored) {
            configLoader.saveAll();
        }
    }

    public Injector getInjector() {
        return injector;
    }

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

    public UserConfigLoader getUserLoader() {
        return configLoader;
    }

    /**
     * Gets the configuration file
     *
     * @param key The {@link ConfigMap.Key} of the config to get (see T).
     * @param <T> The type of {@link AbstractConfig} to get.
     * @return An {@link Optional} that might contain the config, if it exists.
     */
    public <T extends AbstractConfig> Optional<T> getConfig(ConfigMap.Key<T> key) {
        return configMap.getConfig(key);
    }

    public void reload() {
        configMap.reloadAll();
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public MailHandler getMailHandler() { return mailHandler; }

    public AFKHandler getAfkHandler() {
        return afkHandler;
    }

    public JailHandler getJailHandler() {
        return jailHandler;
    }

    public WarmupManager getWarmupManager() {
        return warmupManager;
    }

    public TeleportHandler getTpHandler() {
        return tpHandler;
    }

    public EconHelper getEconHelper() {
        return econHelper;
    }

    public PermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }
}
