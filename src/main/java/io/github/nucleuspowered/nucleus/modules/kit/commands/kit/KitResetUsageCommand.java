/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.kit;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitFallbackBase;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.util.Map;

@Permissions(prefix = "kit", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"resetusage", "reset"}, subcommandOf = KitCommand.class)
@RunAsync
@NonnullByDefault
@NoModifiers
public class KitResetUsageCommand extends KitFallbackBase<CommandSource> {

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                NucleusParameters.ONE_USER,
                KitFallbackBase.KIT_PARAMETER_NO_PERM_CHECK
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource player, CommandContext args, Cause cause) throws Exception {
        Kit kitInfo = args.<Kit>getOne(KIT_PARAMETER_KEY).get();
        User u = args.<User>getOne(NucleusParameters.Keys.USER).get();
        IUserDataObject userDataObject = Nucleus.getNucleus().getStorageManager().getUserService().getOrNewOnThread(u.getUniqueId());
        Map<String, Instant> data = userDataObject.getNullable(KitKeys.REDEEMED_KITS);
        if (data != null && data.containsKey(kitInfo.getName().toLowerCase())) {
            // Remove the key.
            data.remove(kitInfo.getName().toLowerCase());
            userDataObject.set(KitKeys.REDEEMED_KITS, data);
            Nucleus.getNucleus().getStorageManager().getUserService().save(u.getUniqueId(), userDataObject);
            player.sendMessage(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.resetuser.success", u.getName(), kitInfo.getName()));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.kit.resetuser.empty", u.getName(), kitInfo.getName());
    }
}
