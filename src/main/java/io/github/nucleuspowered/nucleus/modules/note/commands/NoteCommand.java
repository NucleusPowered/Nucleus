/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Permissions(suggestedLevel = SuggestedLevel.MOD)
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand({"note", "addnote"})
public class NoteCommand extends CommandBase<CommandSource> {
    public static final String notifyPermission = PermissionRegistry.PERMISSIONS_PREFIX + "note.notify";

    private final String playerKey = "player";
    private final String noteKey = "note";

    @Inject private NoteHandler noteHandler;
    @Inject private NoteConfigAdapter nca;

    @Override
    public Map<String, PermissionInformation> permissionsToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(notifyPermission, new PermissionInformation(Util.getMessageWithFormat("permission.note.notify"), SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.onlyOne(GenericArguments.user(Text.of(playerKey))),
                GenericArguments.onlyOne(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(noteKey))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = args.<User>getOne(playerKey).get();
        String note = args.<String>getOne(noteKey).get();

        UUID noter = Util.consoleFakeUUID;
        if (src instanceof Player) {
            noter = ((Player) src).getUniqueId();
        }

        NoteData noteData = new NoteData(Instant.now(), noter, note);

        if (noteHandler.addNote(user, noteData)) {
            MutableMessageChannel messageChannel = MessageChannel.permission(notifyPermission).asMutable();
            messageChannel.addMember(src);

            messageChannel.send(Util.getTextMessageWithFormat("command.note.success", src.getName(), noteData.getNote(), user.getName()));

            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.warn.fail", user.getName()));
        return CommandResult.empty();
    }
}
