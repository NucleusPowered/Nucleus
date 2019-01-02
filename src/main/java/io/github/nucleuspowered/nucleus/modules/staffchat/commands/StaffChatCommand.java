/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.text.TextParsingUtils;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatMessageChannel;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatUserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.staffchat.services.StaffChatService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@Permissions(suggestedLevel = SuggestedLevel.MOD)
@NoModifiers
@RegisterCommand({"staffchat", "sc", "a"})
@NonnullByDefault
public class StaffChatCommand extends AbstractCommand<CommandSource> {

    private final UserPreferenceService userPreferenceService = getServiceUnchecked(UserPreferenceService.class);
    private final StaffChatService staffChatService = getServiceUnchecked(StaffChatService.class);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_MESSAGE
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        Optional<String> toSend = args.getOne(NucleusParameters.Keys.MESSAGE);
        if (toSend.isPresent()) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.addContext(EventContexts.SHOULD_FORMAT_CHANNEL, StaffChatMessageChannel.getInstance().formatMessages());
                if (src instanceof Player) {
                    Player pl = (Player) src;
                    frame.pushCause(pl);
                    frame.addContext(EventContextKeys.PLAYER_SIMULATED, pl.getProfile());

                    MessageChannel mc = pl.getMessageChannel();
                    pl.setMessageChannel(StaffChatMessageChannel.getInstance());
                    pl.simulateChat(TextParsingUtils.addUrls(toSend.get()), Sponge.getCauseStackManager().getCurrentCause());
                    pl.setMessageChannel(mc);

                    // If you send a message, you're viewing it again.
                    this.userPreferenceService.setPreferenceFor(pl, StaffChatUserPrefKeys.VIEW_STAFF_CHAT, true);
                } else {
                    StaffChatMessageChannel.getInstance()
                            .send(src, TextParsingUtils.addUrls(toSend.get()), ChatTypes.CHAT);
                }

                return CommandResult.success();
            }
        }

        if (!(src instanceof Player)) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.staffchat.consoletoggle"));
        }

        Player player = (Player)src;

        boolean result = this.staffChatService.isToggledChat(player);
        this.staffChatService.toggle(player, !result);

        sendMessageTo(src, "command.staffchat." + (result ? "on" : "off"));

        // If you send a message, you're viewing it again.
        return CommandResult.success();
    }

}
