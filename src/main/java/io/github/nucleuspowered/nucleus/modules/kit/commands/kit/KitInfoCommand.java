/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.kit;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.Since;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@NoModifiers
@Permissions(prefix = "kit")
@RunAsync
@RegisterCommand(value = "info", subcommandOf = KitCommand.class)
@Since(spongeApiVersion = "7.0", minecraftVersion = "1.12.1", nucleusVersion = "1.2")
public class KitInfoCommand extends AbstractCommand<CommandSource> {

    private final String kitName = "kit";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.onlyOne(new KitArgument(Text.of(kitName), false))
        };
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Kit kit = args.<Kit>getOne(kitName).get();
        MessageProvider mp = this.plugin.getMessageProvider();
        Util.getPaginationBuilder(src).title(mp.getTextMessageWithFormat("command.kit.info.title", kit.getName()))
                .contents(
                        addViewHover(mp, kit),
                        addCommandHover(mp, kit),
                        mp.getTextMessageWithFormat("command.kit.info.sep"),
                        mp.getTextMessageWithFormat("command.kit.info.firstjoin", yesno(mp, kit.isFirstJoinKit())),
                        mp.getTextMessageWithFormat("command.kit.info.cost", String.valueOf(kit.getCost())),
                        mp.getTextMessageWithFormat("command.kit.info.cooldown", kit.getCooldown().map(x ->
                            Util.getTimeStringFromSeconds(x.getSeconds())).orElse(mp.getMessageWithFormat("standard.none"))),
                        mp.getTextMessageWithFormat("command.kit.info.onetime", yesno(mp, kit.isOneTime())),
                        mp.getTextMessageWithFormat("command.kit.info.autoredeem", yesno(mp, kit.isAutoRedeem())),
                        mp.getTextMessageWithFormat("command.kit.info.hidden", yesno(mp, kit.isHiddenFromList())),
                        mp.getTextMessageWithFormat("command.kit.info.displayredeem", yesno(mp, kit.isAutoRedeem())),
                        mp.getTextMessageWithFormat("command.kit.info.ignoresperm", yesno(mp, kit.ignoresPermission()))
                ).sendTo(src);
        return CommandResult.success();
    }

    private Text addViewHover(MessageProvider mp, Kit kit) {
        return mp.getTextMessageWithFormat("command.kit.info.itemcount", String.valueOf(kit.getStacks().size())).toBuilder()
                .onHover(TextActions.showText(mp.getTextMessageWithFormat("command.kit.info.hover.itemcount", kit.getName())))
                .onClick(TextActions.runCommand("/nucleus:kit view " + kit.getName())).build();
    }

    private Text addCommandHover(MessageProvider mp, Kit kit) {
        return mp.getTextMessageWithFormat("command.kit.info.commandcount", String.valueOf(kit.getCommands().size())).toBuilder()
                .onHover(TextActions.showText(mp.getTextMessageWithFormat("command.kit.info.hover.commandcount", kit.getName())))
                .onClick(TextActions.runCommand("/nucleus:kit command " + kit.getName())).build();
    }

    private String yesno(MessageProvider mp, boolean yesno) {
        return mp.getMessageWithFormat("standard.yesno." + yesno);
    }

}
