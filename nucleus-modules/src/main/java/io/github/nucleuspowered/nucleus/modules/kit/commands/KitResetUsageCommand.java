/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.parameters.KitParameter;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.util.Map;

@NonnullByDefault
@Command(
        aliases = { "resetusage", "reset" },
        async = true,
        basePermission = KitPermissions.BASE_KIT_RESETUSAGE,
        commandDescriptionKey = "kit.resetusage",
        parentCommand = KitCommand.class
)
public class KitResetUsageCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.ONE_USER.get(serviceCollection),
                serviceCollection.getServiceUnchecked(KitService.class).createKitElement(false)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Kit kitInfo = context.requireOne(KitParameter.KIT_PARAMETER_KEY, Kit.class);
        User u = context.requireOne(NucleusParameters.Keys.USER, User.class);
        IStorageManager storageManager = context.getServiceCollection().storageManager();
        IUserDataObject userDataObject = storageManager.getUserService().getOrNewOnThread(u.getUniqueId());
        Map<String, Instant> data = userDataObject.getNullable(KitKeys.REDEEMED_KITS);
        if (data != null && data.containsKey(kitInfo.getName().toLowerCase())) {
            // Remove the key.
            data.remove(kitInfo.getName().toLowerCase());
            userDataObject.set(KitKeys.REDEEMED_KITS, data);
            storageManager.getUserService().save(u.getUniqueId(), userDataObject);
            context.sendMessage("command.kit.resetuser.success", u.getName(), kitInfo.getName());
            return context.successResult();
        }

        return context.errorResult("command.kit.resetuser.empty", u.getName(), kitInfo.getName());
    }
}
