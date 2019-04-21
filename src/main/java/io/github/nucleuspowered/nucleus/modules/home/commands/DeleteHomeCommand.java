/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.argumentparsers.HomeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Permissions(mainOverride = "home", suggestedLevel = SuggestedLevel.USER)
@NoModifiers
@RegisterCommand(value = {"delete", "del"}, subcommandOf = HomeCommand.class, rootAliasRegister = {"deletehome", "delhome"})
@EssentialsEquivalent({"delhome", "remhome", "rmhome"})
@NonnullByDefault
public class DeleteHomeCommand extends AbstractCommand<Player> {

    private final String homeKey = "home";

    private final HomeService homeService = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(HomeService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(new HomeArgument(Text.of(this.homeKey), Nucleus.getNucleus()))};
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        Home wl = args.<Home>getOne(this.homeKey).get();

        CauseStackHelper.createFrameWithCausesWithConsumer(c -> this.homeService.removeHomeInternal(c, wl), src);
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.home.delete.success", wl.getName()));
        return CommandResult.success();
    }
}
