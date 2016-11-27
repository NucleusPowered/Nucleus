/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.PluginInfo;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.TextMessageException;
import uk.co.drnaylor.quickstart.ModuleContainer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Permissions(prefix = "nucleus", suggestedLevel = SuggestedLevel.NONE)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand(value = "info", subcommandOf = NucleusCommand.class)
public class InfoCommand extends AbstractCommand<CommandSource> {

    private final String sep = "------------";

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        // Sponge versions
        List<String> information = Lists.newArrayList();

        information.add(sep);
        information.add("Nucleus Diagnostics");
        information.add(sep);

        information.add("This file contains information about Nucleus and the environment it runs in.");

        information.add(sep);
        information.add("Environment");
        information.add(sep);

        Platform platform = Sponge.getPlatform();
        information.add("Minecraft Version: " + platform.getMinecraftVersion().getName());
        information.add(String.format("Sponge Version: %s %s", platform.getImplementation().getName(), platform.getImplementation().getVersion().orElse("unknown")));
        information.add(String.format("Sponge API Version: %s %s", platform.getApi().getName(), platform.getApi().getVersion().orElse("unknown")));
        information.add("Nucleus Version: " + PluginInfo.VERSION + " (Git: " + PluginInfo.GIT_HASH + ")");

        plugin.getMixinConfigIfAvailable().ifPresent(x -> {
            information.add(sep);
            information.add("Nucleus Mixins");
            information.add(sep);
            information.add("World Generation mixins: " + (x.get().config.isWorldgeneration() ? "on" : "off"));
            information.add("/invsee mixins: " + (x.get().config.isInvsee() ? "on" : "off"));
        });

        information.add(sep);
        information.add("Plugins");
        information.add(sep);

        Sponge.getPluginManager().getPlugins().forEach(x -> information.add(x.getName() + " (" + x.getId() + ") version " + x.getVersion().orElse("unknown")));

        information.add(sep);
        information.add("Registered Commands");
        information.add(sep);

        final CommandManager manager = Sponge.getCommandManager();
        manager.getPrimaryAliases().stream().sorted().forEachOrdered(x -> {
            Optional<? extends CommandMapping> ocm = manager.get(x);
            if (ocm.isPresent()) {
                Optional<PluginContainer> optionalPC = manager.getOwner(ocm.get());
                if (optionalPC.isPresent()) {
                    PluginContainer container = optionalPC.get();
                    information.add("/" + x + " - " + container.getName() + " (" + container.getId() + ") version " + container.getVersion().orElse("unknown"));
                } else {
                    information.add("/" + x + " - unknown (plugin container not present)");
                }
            } else {
                information.add("/" + x + " - unknown (mapping not present)");
            }
        });

        information.add(sep);
        information.add("Nucleus: Enabled Modules");
        information.add(sep);

        plugin.getModuleContainer().getModules().stream().sorted().forEach(information::add);

        Set<String> disabled = plugin.getModuleContainer().getModules(ModuleContainer.ModuleStatusTristate.DISABLE);
        if (!disabled.isEmpty()) {
            information.add(sep);
            information.add("Nucleus: Disabled Modules");
            information.add(sep);

            disabled.stream().sorted().forEach(information::add);
        }


        String fileName = "nucleus-info-" + DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()) + "-" + DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now()) + ".txt";
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(fileName, false))) {
            for (String s : information) {
                fw.write(s);
                fw.newLine();
            }

            fw.flush();
        } catch (Exception e) {
            throw new TextMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.nucleus.info.fileerror"), e);
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nucleus.info.saved", fileName));
        return CommandResult.success();
    }
}
