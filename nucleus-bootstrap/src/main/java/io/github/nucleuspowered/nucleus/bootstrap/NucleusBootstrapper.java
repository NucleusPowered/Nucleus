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
import io.github.nucleuspowered.nucleus.bootstrap.error.ConfigErrorHandler;
import io.github.nucleuspowered.nucleus.core.NucleusJavaProperties;
import io.github.nucleuspowered.nucleus.core.datatypes.NucleusCoreDatatypeRegistration;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusConfigException;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusErrorHandler;
import io.github.nucleuspowered.nucleus.modules.NucleusModuleProvider;
import io.github.nucleuspowered.nucleus.bootstrap.error.InvalidVersionErrorHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin("nucleus")
public class NucleusBootstrapper {

    private static final String NO_VERSION_CHECK = "nucleus.nocheck";

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Path configDirectory;
    private final Injector injector;
    private final Game game;

    @Inject
    public NucleusBootstrapper(
            final Game game,
            final PluginContainer pluginContainer,
            final Logger logger,
            @ConfigDir(sharedRoot = false) final Path configDirectory,
            final Injector injector) {
        this.game = game;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.configDirectory = configDirectory;
        this.injector = injector;
    }

    private boolean versionCheck(final IPluginInfo pluginInfo) throws IllegalStateException {
        if (System.getProperty(NO_VERSION_CHECK) != null) {
            final Pattern matching = Pattern.compile("^(?<major>\\d+)\\.(?<minor>\\d+)");
            final ArtifactVersion v = Sponge.platform().container(Platform.Component.API).metadata().version();

            final Optional<Matcher> versionResult = Arrays.stream(pluginInfo.validVersions()).map(x -> {
                final Matcher version1 = matching.matcher(NucleusPluginInfo.SPONGE_API_VERSION);
                if (version1.find()) {
                    return version1;
                }
                return null;
            }).filter(Objects::nonNull).findFirst();
            if (!versionResult.isPresent()) {
                return false;
            }

            final Matcher version = versionResult.get();
            final int maj = Integer.parseInt(version.group("major"));
            final int min = Integer.parseInt(version.group("minor"));
            final boolean notRequiringSnapshot = !v.getQualifier().contains("SNAPSHOT");

            final int major = v.getMajorVersion();
            if (major != maj) {
                this.logger.fatal("Unable to start Nucleus: SpongeAPI major version {} does not match Nucleus expectation of {}", major, maj);
                new InvalidVersionErrorHandler(
                        this.pluginContainer,
                        NucleusJavaProperties.RUN_DOCGEN,
                        this.logger,
                        NucleusPluginInfo.SPONGE_API_VERSION,
                        pluginInfo
                        );
                return false;
            }

            int minor = v.getMinorVersion();
            final boolean serverIsSnapshot = v.getQualifier().contains("SNAPSHOT");

            if (serverIsSnapshot && notRequiringSnapshot) {
                // If we are a snapshot, and the target version is NOT a snapshot, decrement our version number.
                minor = minor - 1;
            }

            if (minor < min) {
                // not right minor version
                this.logger.fatal("Unable to start Nucleus: SpongeAPI version {} is lower than Nucleus' requirement {}", v, version);
                new InvalidVersionErrorHandler(
                        this.pluginContainer,
                        NucleusJavaProperties.RUN_DOCGEN,
                        this.logger,
                        NucleusPluginInfo.SPONGE_API_VERSION,
                        pluginInfo
                );
                return false;
            }
        }
        return true;
    }

    @Listener
    public void startPlugin(final ConstructPluginEvent event) {
        final IPluginInfo pluginInfo = new NucleusPluginInfo();
        if (this.versionCheck(pluginInfo)) {
            final NucleusErrorHandler errorHandler;
            try {
                this.logger.info("Nucleus {} running on Sponge API {} ({} version {})", pluginInfo.version(),
                        Sponge.platform().container(Platform.Component.API).metadata().version(),
                        Sponge.platform().container(Platform.Component.IMPLEMENTATION).metadata().name().orElse("Unknown Implementation"),
                        Sponge.platform().container(Platform.Component.IMPLEMENTATION).metadata().version());
                this.logger.info("Nucleus is starting...");
                final NucleusCore core =
                        new NucleusCore(this.game,
                                this.pluginContainer,
                                this.configDirectory,
                                this.logger,
                                this.injector,
                                new NucleusModuleProvider(),
                                pluginInfo);
                NucleusCoreDatatypeRegistration.registerDataTypes(this.game.dataManager());
                core.init();
                Sponge.eventManager().registerListeners(this.pluginContainer, core);
                this.logger.info("Nucleus has completed initialisation successfully. Awaiting Sponge lifecycle events.");
                return;
            } catch (final NucleusConfigException e) {
                errorHandler = new ConfigErrorHandler(this.pluginContainer, e.getWrapped(), e.isDocgen(), this.logger, e.getFileName(), pluginInfo);
            } catch (final Exception e) {
                errorHandler = new InitialisationNucleusErrorHandler(this.pluginContainer, e,
                                NucleusJavaProperties.RUN_DOCGEN,
                                this.logger,
                                pluginInfo);
            }
            this.logger.error("Nucleus did not complete initialisation. Nucleus will not boot.");
            errorHandler.generatePrettyPrint(this.logger, Level.ERROR);
        }
    }

}
