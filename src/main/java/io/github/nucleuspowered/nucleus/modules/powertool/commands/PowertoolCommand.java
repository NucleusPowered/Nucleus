/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Permissions
@RunAsync
@NoModifiers
@RegisterCommand({"powertool", "pt"})
@EssentialsEquivalent({"powertool", "pt"})
@NonnullByDefault
public class PowertoolCommand extends AbstractCommand<Player> {

    private final PowertoolService service = getServiceUnchecked(PowertoolService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_COMMAND
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        ItemStack itemStack = src.getItemInHand(HandTypes.MAIN_HAND)
                .orElseThrow(() -> ReturnMessageException.fromKey("command.powertool.noitem"));

        Optional<String> command = args.getOne(NucleusParameters.Keys.COMMAND);
        return command.map(s -> setPowertool(src, itemStack.getType(), s))
                .orElseGet(() -> viewPowertool(src, itemStack));
    }

    private CommandResult viewPowertool(Player src, ItemStack item) {
        Optional<List<String>> cmds = this.service.getPowertoolForItem(src.getUniqueId(), item.getType());
        MessageProvider mp = Nucleus.getNucleus().getMessageProvider();
        if (cmds.isPresent() && !cmds.get().isEmpty()) {
            Util.getPaginationBuilder(src)
                    .contents(cmds.get().stream().map(f -> Text.of(TextColors.YELLOW, f)).collect(Collectors.toList()))
                    .title(mp.getTextMessageWithTextFormat("command.powertool.viewcmdstitle", Text.of(item), Text.of(item.getType().getId())))
                    .sendTo(src);
        } else {
            src.sendMessage(mp.getTextMessageWithTextFormat("command.powertool.nocmds", Text.of(item)));
        }

        return CommandResult.success();
    }

    private CommandResult setPowertool(Player src, ItemType item, String command) {
        // For consistency, if a command starts with "/", remove it, but just
        // once. WorldEdit commands can be input using "//"
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        this.service.setPowertool(src.getUniqueId(), item, Lists.newArrayList(command));
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.powertool.set", item.getId(), command));
        return CommandResult.success();
    }
}
