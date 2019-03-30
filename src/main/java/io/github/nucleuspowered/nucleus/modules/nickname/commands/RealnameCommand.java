/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
@RegisterCommand({"realname"})
@Permissions(suggestedLevel = SuggestedLevel.USER)
@EssentialsEquivalent("realname")
public class RealnameCommand extends AbstractCommand<CommandSource> {

    private final String playerKey = "name";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.string(Text.of(playerKey))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        String argname = args.<String>getOne(playerKey).get();
        String name = argname.toLowerCase();

        NicknameService service = getServiceUnchecked(NicknameService.class);
        Map<Player, Text> names = service.getFromSubstring(argname.toLowerCase());
        names.forEach((player, text) -> {

        });

        if (names.isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.realname.nonames", argname));
        } else {
            List<Text> realNames = new ArrayList<>();
            for (Map.Entry<Player, Text> entry : names.entrySet()) {
                realNames.add(Text.of(entry.getKey().getName(), TextColors.GRAY, " -> ", TextColors.WHITE, entry.getValue()));
            }

            PaginationList.Builder plb = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder()
                    .contents(realNames)
                    .padding(Text.of(TextColors.GREEN, "-"))
                    .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.realname.title", argname));

            if (!(src instanceof Player)) {
                plb.linesPerPage(-1);
            }

            plb.sendTo(src);
        }

        return CommandResult.success();
    }

    private class NameTuple {
        private final String nickname;
        private final Player player;

        private NameTuple(String nickname, Player player) {
            this.nickname = nickname;
            this.player = player;
        }
    }
}
