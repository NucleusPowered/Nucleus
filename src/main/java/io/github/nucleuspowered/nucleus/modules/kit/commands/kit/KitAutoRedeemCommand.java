/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.kit;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.commands.kit.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

/**
 * Sets kit to be automatically redeemed on login.
 *
 * Command Usage: /kit set Permission: plugin.kit.onetime.base
 */
@Permissions(prefix = "kit", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"autoredeem"}, subcommandOf = KitCommand.class)
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
public class KitAutoRedeemCommand extends AbstractCommand<CommandSource> {

    @Inject private KitHandler kitConfig;
    @Inject private KitConfigAdapter kca;

    private final String kit = "kit";
    private final String toggle = "autoRedeemToggle";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.seq(GenericArguments.onlyOne(new KitArgument(Text.of(kit), kca, kitConfig, true)),
            GenericArguments.onlyOne(GenericArguments.bool(Text.of(toggle))))
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource player, CommandContext args) throws Exception {
        KitArgument.KitInfo kitInfo = args.<KitArgument.KitInfo>getOne(kit).get();
        boolean b = args.<Boolean>getOne(toggle).get();

        // This Kit is a reference back to the version in list, so we don't need
        // to update it explicitly
        kitInfo.kit.setAutoRedeem(b);
        kitConfig.saveKit(kitInfo.name, kitInfo.kit);
        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(b ? "command.kit.autoredeem.on" : "command.kit.autoredeem.off", kitInfo.name));

        return CommandResult.success();
    }
}
