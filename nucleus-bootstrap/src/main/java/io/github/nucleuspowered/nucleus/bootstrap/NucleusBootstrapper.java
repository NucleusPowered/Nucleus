/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.bootstrap.error.InitialisationNucleusErrorHandler;
import io.github.nucleuspowered.nucleus.core.IPluginInfo;
import io.github.nucleuspowered.nucleus.core.NucleusCore;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusErrorHandler;
import io.github.nucleuspowered.nucleus.modules.NucleusModuleProvider;
import io.github.nucleuspowered.nucleus.bootstrap.error.InvalidVersionErrorHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin("nucleus")
public class NucleusBootstrapper {

    private static final String NO_VERSION_CHECK = "nucleus.nocheck";

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Path configDirectory;
    private final Injector injector;

    @Inject
    public NucleusBootstrapper(
            final PluginContainer pluginContainer,
            final Logger logger,
            @ConfigDir(sharedRoot = false) final Path configDirectory,
            final Injector injector) {
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.configDirectory = configDirectory;
        this.injector = injector;
    }

    private boolean versionCheck(final IPluginInfo pluginInfo) throws IllegalStateException {
        if (System.getProperty(NO_VERSION_CHECK) != null) {
            final Pattern matching = Pattern.compile("^(?<major>\\d+)\\.(?<minor>\\d+)");
            final String v = Sponge.platform().container(Platform.Component.API).getMetadata().getVersion();

            final Matcher version = matching.matcher(NucleusPluginInfo.SPONGE_API_VERSION);
            if (!version.find()) {
                return false; // can't compare.
            }

            final int maj = Integer.parseInt(version.group("major"));
            final int min = Integer.parseInt(version.group("minor"));
            //noinspection MismatchedStringCase,ConstantConditions
            final boolean notRequiringSnapshot = !NucleusPluginInfo.SPONGE_API_VERSION.contains("SNAPSHOT");

            final Matcher m = matching.matcher(v);
            if (m.find()) {
                final int major = Integer.parseInt(m.group("major"));
                if (major != maj) {
                    this.logger.fatal("Unable to start Nucleus: SpongeAPI major version {} does not match Nucleus expectation of {}", major, maj);
                    new InvalidVersionErrorHandler(
                            this.pluginContainer,
                            System.getProperty(NucleusCore.DOCGEN_PROPERTY) != null,
                            this.logger,
                            NucleusPluginInfo.SPONGE_API_VERSION,
                            pluginInfo
                            );
                    return false;
                }

                int minor = Integer.parseInt(m.group("minor"));
                final boolean serverIsSnapshot = v.contains("SNAPSHOT");

                //noinspection ConstantConditions
                if (serverIsSnapshot && notRequiringSnapshot) {
                    // If we are a snapshot, and the target version is NOT a snapshot, decrement our version number.
                    minor = minor - 1;
                }

                if (minor < min) {
                    // not right minor version
                    this.logger.fatal("Unable to start Nucleus: SpongeAPI version {} is lower than Nucleus' requirement {}", v, version);
                    new InvalidVersionErrorHandler(
                            this.pluginContainer,
                            System.getProperty(NucleusCore.DOCGEN_PROPERTY) != null,
                            this.logger,
                            NucleusPluginInfo.SPONGE_API_VERSION,
                            pluginInfo
                    );
                    return false;
                }
            }
        }
        return true;
    }

    @Listener
    public void startPlugin(final ConstructPluginEvent event) {
        final IPluginInfo pluginInfo = new NucleusPluginInfo();
        if (this.versionCheck(pluginInfo)) {
            try {
                this.logger.info("Nucleus {} running on Sponge API {} ({} version {})", pluginInfo.version(),
                        Sponge.platform().container(Platform.Component.API).getMetadata().getVersion(),
                        Sponge.platform().container(Platform.Component.IMPLEMENTATION).getMetadata().getName().orElse("Unknown Implementation"),
                        Sponge.platform().container(Platform.Component.IMPLEMENTATION).getMetadata().getVersion());
                this.logger.info("Nucleus is starting...");
                final NucleusCore core =
                        new NucleusCore(this.pluginContainer, this.configDirectory, this.logger, this.injector, new NucleusModuleProvider(),
                                pluginInfo);
                core.init();
                Sponge.eventManager().registerListeners(this.pluginContainer, core);
                this.logger.info("Nucleus has completed initialisation successfully. Awaiting Sponge lifecycle events.");
            } catch (final Exception e) {
                this.logger.fatal("Nucleus did not complete initialisation. Nucleus will not boot.");
                // Inserts itself into the listeners
                final NucleusErrorHandler errorHandler =
                        new InitialisationNucleusErrorHandler(this.pluginContainer, e,
                                System.getProperty(NucleusCore.DOCGEN_PROPERTY) != null,
                                this.logger,
                                pluginInfo);
                errorHandler.generatePrettyPrint(this.logger, Level.ERROR);
            }
        }
    }

}
