/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.core;

import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.service.EssenceModuleService;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static io.github.essencepowered.essence.PluginInfo.*;

/**
 * Gives information about Essence.
 *
 * Command Usage: /essence
 * Permission: essence.essence.base
 */
@RunAsync
@Permissions
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({ "essence" })
public class EssenceCommand extends CommandBase<CommandSource> {

    private final Text version = Text.of(MESSAGE_PREFIX, TextColors.GREEN, NAME + " version " + VERSION);
    private Text modules = null;

    @Override
    @SuppressWarnings("unchecked")
    public CommandSpec createSpec() {
        return CommandSpec.builder().children(this.createChildCommands()).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        if (modules == null) {
            EssenceModuleService qs = Sponge.getServiceManager().provideUnchecked(EssenceModuleService.class);

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
