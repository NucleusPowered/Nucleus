/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.Map;

@Command(
        aliases = { "resetusage", "reset" },
        basePermission = KitPermissions.BASE_KIT_RESETUSAGE,
        commandDescriptionKey = "kit.resetusage",
        parentCommand = KitCommand.class
)
public class KitResetUsageCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.ONE_USER,
                serviceCollection.getServiceUnchecked(KitService.class).kitParameterWithoutPermission()
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Kit kitInfo = context.requireOne(KitService.KIT_KEY);
        final User u = context.getUserFromArgs(NucleusParameters.ONE_USER);
        final IStorageManager storageManager = context.getServiceCollection().storageManager();
        final IUserDataObject userDataObject = storageManager.getUserService().getOrNewOnThread(u.uniqueId());
        final Map<String, Instant> data = userDataObject.getNullable(KitKeys.REDEEMED_KITS);
        if (data != null && data.containsKey(kitInfo.getName().toLowerCase())) {
            // Remove the key.
            data.remove(kitInfo.getName().toLowerCase());
            userDataObject.set(KitKeys.REDEEMED_KITS, data);
            storageManager.getUserService().save(u.uniqueId(), userDataObject);
            context.sendMessage("command.kit.resetuser.success", u.name(), kitInfo.getName());
            return context.successResult();
        }

        return context.errorResult("command.kit.resetuser.empty", u.name(), kitInfo.getName());
    }
}
