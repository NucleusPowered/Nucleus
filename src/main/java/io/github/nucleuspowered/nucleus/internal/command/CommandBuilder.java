/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.command;

import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.PluginInfo;
import io.github.nucleuspowered.nucleus.internal.annotations.SkipOnError;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public class CommandBuilder {

    private final Nucleus plugin;
    private final Set<Class<? extends AbstractCommand<?>>> commandSet;
    private final SimpleCommentedConfigurationNode sn;
    private final String moduleID;
    private final String moduleName;

    private final static Set<Class<? extends AbstractCommand>> registeredCommands = Sets.newHashSet();

    public static boolean isCommandRegistered(Class<? extends AbstractCommand> command) {
        return registeredCommands.contains(command);
    }

    public CommandBuilder(Nucleus plugin, Set<Class<? extends AbstractCommand<?>>> commandSet, String moduleID, String moduleName) {
        this.plugin = plugin;
        this.commandSet = commandSet;
        this.sn = SimpleCommentedConfigurationNode.root();
        this.moduleID = moduleID;
        this.moduleName = moduleName;
    }

    public <T extends AbstractCommand<?>> Optional<T> buildCommand(Class<T> commandClass) {
        return buildCommand(commandClass, true);
    }

    <T extends AbstractCommand<?>> Optional<T> buildCommand(Class<T> commandClass, boolean rootCmd) {
        Optional<T> optionalCommand = getInstance(commandClass);
        if (!optionalCommand.isPresent()) {
            return Optional.empty();
        }

        T c = optionalCommand.get();
        try {
            c.setModuleName(moduleID, moduleName);
            c.setModuleCommands(commandSet);
            c.setCommandBuilder(this);
            c.postInit();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }

        // If we are using DocGen, add the command information to the system.
        plugin.getDocGenCache().ifPresent(x -> x.addCommand(moduleID, c));

        String commandSection = c.getAliases()[0].toLowerCase();
        sn.getNode(commandSection).setValue(c.getDefaults());

        if (plugin.getCommandsConfig().getCommandNode(commandSection).getNode("enabled").getBoolean(true)) {
            ConfigurationNode cn = plugin.getCommandsConfig().getCommandNode(commandSection);
            ConfigurationNode node = cn.getNode("aliases");
            if (node.getValue() == null) {
                cn.removeChild("aliases");
            }

            try {
                // Register the commands.
                if (rootCmd) {
                    // This will return true for the first anyway
                    String first = c.getAliases()[0];
                    String[] aliases = Arrays.stream(c.getAliases()).filter(x -> x.equals(first) || node.getNode(x).getBoolean(true))
                            .toArray(String[]::new);
                    checkMapping(Sponge.getCommandManager().register(plugin, c, aliases).orElse(null), aliases);
                }

                // Register as another full blown command.
                for (String st : c.getRootCommandAliases()) {
                    if (cn.getNode("aliases", st).getBoolean(true)) {
                        checkMapping(Sponge.getCommandManager().register(plugin, c, st).orElse(null), new String[] { st });
                        // Sponge.getCommandManager().register(plugin, c, st);
                    }
                }

                if (c instanceof Reloadable) {
                    plugin.registerReloadable(((Reloadable) c));
                }
            } catch (Exception e) {
                throw new IllegalStateException(plugin.getMessageProvider().getMessageWithFormat("startup.commandfailiure", c.getAliases()[0],
                        commandClass.getName()));
            }

            registeredCommands.add(c.getClass());
            return Optional.of(c);
        }

        return Optional.empty();
    }

    private <T extends AbstractCommand<?>> Optional<T> getInstance(Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            if (instance.canLoad()) {
                return Optional.of(instance);
            }

            return Optional.empty();

            // I can't believe I have to do this...
        } catch (IllegalAccessException | InstantiationException | RuntimeException | NoClassDefFoundError e) {
            if (clazz.isAnnotationPresent(SkipOnError.class)) {
                plugin.getLogger().warn(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("startup.injectablenotloaded", clazz.getName()));
                return Optional.empty();
            }

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public CommentedConfigurationNode getNodeToMerge() {
        return sn;
    }

    private void checkMapping(@Nullable CommandMapping commandMapping, String[] aliases) {
        if (commandMapping != null) {
            for (String a : aliases) {
                Optional<PluginContainer> opc = Sponge.getCommandManager().get(aliases[0])
                        .map(x -> Sponge.getCommandManager().getOwner(x).orElse(null));
                if (opc.isPresent() && !opc.get().getId().equalsIgnoreCase(PluginInfo.ID)) {
                    Nucleus.getNucleus().addStartupMessage(
                            Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("startup.command.plugin",
                                    a, opc.get().getName(), PluginInfo.ID, aliases[0]));
                } else if (!opc.isPresent()) {
                    Nucleus.getNucleus().addStartupMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("startup.command.fallback",
                            a, PluginInfo.ID, aliases[0]));
                }
            }
        } else {
            Optional<PluginContainer> ocm = Sponge.getCommandManager().get(aliases[0])
                    .map(x -> Sponge.getCommandManager().getOwner(x).orElse(null));
            if (ocm.isPresent()) {
                Nucleus.getNucleus().addStartupMessage(
                        Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("startup.command.noregisterplugin",
                                aliases[0], ocm.get().getName()));
            } else {
                Nucleus.getNucleus().addStartupMessage(
                        Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("startup.command.couldnotregister",
                                aliases[0]));
            }
        }
    }
}
