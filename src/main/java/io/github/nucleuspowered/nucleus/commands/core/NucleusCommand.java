/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.commands.core;

import io.github.nucleuspowered.nucleus.api.PluginModule;
import io.github.nucleuspowered.nucleus.api.service.NucleusModuleService;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static io.github.nucleuspowered.nucleus.PluginInfo.*;

/**
 * Gives information about Nucleus.
 *
 * Command Usage: /nucleus
 * Permission: nucleus.nucleus.base
 */
@RunAsync
@Permissions
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({ "nucleus" })
public class NucleusCommand extends CommandBase<CommandSource> {

    private final Text version = Text.of(MESSAGE_PREFIX, TextColors.GREEN, NAME + " version " + VERSION);
    private Text modules = null;

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().children(this.createChildCommands()).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        if (modules == null) {
            NucleusModuleService qs = Sponge.getServiceManager().provideUnchecked(NucleusModuleService.class);

            Text.Builder tb = Text.builder("Modules: ").color(TextColors.GREEN);

            boolean addComma = false;
            for (PluginModule x : PluginModule.values()) {
                if (addComma) {
                    tb.append(Text.of(TextColors.GREEN, ", "));
                }

                tb.append(Text.of(qs.getModulesToLoad().contains(x) ? TextColors.GREEN : TextColors.RED, x.getKey()));
                addComma = true;
            }

            modules = tb.append(Text.of(TextColors.GREEN, ".")).build();
        }

        src.sendMessage(version);
        src.sendMessage(modules);
        return CommandResult.success();
    }
}
