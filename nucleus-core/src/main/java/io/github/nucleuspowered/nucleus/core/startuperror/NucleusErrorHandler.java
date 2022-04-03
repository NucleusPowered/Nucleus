/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.startuperror;

import io.github.nucleuspowered.nucleus.core.IPluginInfo;
import io.github.nucleuspowered.nucleus.core.util.PrettyPrinter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.plugin.PluginContainer;

import java.util.List;

public class NucleusErrorHandler {

    private final PluginContainer pluginContainer;
    private final boolean shouldShutdown;
    protected final Throwable capturedThrowable;
    private final Logger logger;
    private final IPluginInfo pluginInfo;

    public NucleusErrorHandler(
            final PluginContainer pluginContainer,
            final Throwable throwable,
            final boolean shouldShutdown,
            final Logger logger,
            final IPluginInfo pluginInfo) {
        this.pluginContainer = pluginContainer;
        this.capturedThrowable = throwable;
        this.shouldShutdown = shouldShutdown;
        this.logger = logger;
        this.pluginInfo = pluginInfo;
        this.disable();
    }

    public void disable() {
        Sponge.eventManager().unregisterListeners(this.pluginContainer);
        Sponge.asyncScheduler().tasks(this.pluginContainer).forEach(ScheduledTask::cancel);

        // Re-register this to warn people about the error.
        Sponge.eventManager().registerListeners(this.pluginContainer, this);
    }

    @Listener
    public void errorOnStartup(final StartedEngineEvent<Server> event) {
        try {
            if (Sponge.platform().type().isServer()) {
                Sponge.server().setHasWhitelist(true);
            }
        } catch (final Throwable e) {
            //ignored
        }

        this.generatePrettyPrint(this.logger, Level.ERROR);

        if (this.shouldShutdown) {
            Sponge.server().shutdown();
        }
    }

    public String getTitle() {
        return "NUCLEUS FAILED TO LOAD";
    }

    public void generatePrettyPrint(final Logger logger, final Level loggerLevel) {
        final PrettyPrinter prettyPrinter = new PrettyPrinter(80);
        prettyPrinter.add(this.getTitle()).centre();
        prettyPrinter.hr('-');
        this.addX(prettyPrinter);
        prettyPrinter.hr('-');
        this.createTopLevelMessage(prettyPrinter);
        this.printStackTraceIfAny(prettyPrinter);
        prettyPrinter.add("If this error persists, check your configuration files and ensure that you have the latest version of Nucleus which "
                + "matches the current version of the Sponge API.");
        this.createPostStackTraceMessage(prettyPrinter);
        prettyPrinter.add();
        prettyPrinter.hr('*');
        prettyPrinter.add("Server Information").centre();
        prettyPrinter.hr('*');
        prettyPrinter.add();

        prettyPrinter.add("Nucleus version: " + this.pluginInfo.version() + ", (Git: " + this.pluginInfo.gitHash() + ")");
        final Platform platform = Sponge.platform();
        final PluginContainer api = platform.container(Platform.Component.API);
        final PluginContainer impl = platform.container(Platform.Component.IMPLEMENTATION);
        prettyPrinter.add("Minecraft version: %s", platform.minecraftVersion().name());
        prettyPrinter.add("Sponge Implementation: %s %s",
                impl.metadata().name().orElse("unknown"),
                impl.metadata().version());
        prettyPrinter.add("SpongeAPI: %s %s",
                api.metadata().name().orElse("unknown"),
                api.metadata().version());
        prettyPrinter.log(logger, loggerLevel);
    }

    protected void createTopLevelMessage(final PrettyPrinter prettyPrinter) {
        prettyPrinter.add("Nucleus encountered an error during server start up and did not enable successfully.");
    }

    protected void printStackTrace(final PrettyPrinter prettyPrinter) {
        prettyPrinter.add("The error that Nucleus encountered will be reproduced below for your convenience.");
        prettyPrinter.hr('-');

        if (this.capturedThrowable == null) {
            prettyPrinter.add("No exception was raised.");
        } else {
            prettyPrinter.add("Captured stacktace:");
            prettyPrinter.add(this.capturedThrowable);
        }
    }

    private void printStackTraceIfAny(final PrettyPrinter prettyPrinter) {
        prettyPrinter.add();
        prettyPrinter.add("No commands, listeners or tasks are registered.");
        if (Sponge.platform().type().isServer()) {
            prettyPrinter
                    .add("The server has been automatically whitelisted - this is to protect your server and players if you rely on some of "
                            + "Nucleus' functionality (such as fly states, etc.)");
        }
        this.printStackTrace(prettyPrinter);
        prettyPrinter.hr('-');
    }

    protected void createPostStackTraceMessage(final PrettyPrinter prettyPrinter) {
        prettyPrinter.add("If you continue to have this error, please report this error to the Nucleus "
                + "team at https://github.com/NucleusPowered/Nucleus/issues");
    }

    protected final void addTri(final List<String> messages) {
        messages.add("        /\\");
        messages.add("       /  \\");
        messages.add("      / || \\");
        messages.add("     /  ||  \\");
        messages.add("    /   ||   \\");
        messages.add("   /    ||    \\");
        messages.add("  /            \\");
        messages.add(" /      **      \\");
        messages.add("------------------");
    }

    protected final void addX(final PrettyPrinter prettyPrinter) {
        prettyPrinter.add("\\              /").centre();
        prettyPrinter.add(" \\            / ").centre();
        prettyPrinter.add("  \\          /  ").centre();
        prettyPrinter.add("   \\        /   ").centre();
        prettyPrinter.add("    \\      /    ").centre();
        prettyPrinter.add("     \\    /     ").centre();
        prettyPrinter.add("      \\  /      ").centre();
        prettyPrinter.add("       \\/       ").centre();
        prettyPrinter.add("       /\\       ").centre();
        prettyPrinter.add("      /  \\      ").centre();
        prettyPrinter.add("     /    \\     ").centre();
        prettyPrinter.add("    /      \\    ").centre();
        prettyPrinter.add("   /        \\   ").centre();
        prettyPrinter.add("  /          \\  ").centre();
        prettyPrinter.add(" /            \\ ").centre();
        prettyPrinter.add("/              \\").centre();
    }

    
}
