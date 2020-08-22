/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ignore.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.ignore.IgnorePermissions;
import io.github.nucleuspowered.nucleus.modules.ignore.services.IgnoreService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = {"ignorelist", "listignore", "ignored"},
        basePermission = IgnorePermissions.BASE_IGNORELIST,
        commandDescriptionKey = "ignorelist",
        async = true,
        associatedPermissions = IgnorePermissions.OTHERS_IGNORELIST
)
public class IgnoreListCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(false, IgnorePermissions.OTHERS_IGNORELIST)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        IgnoreService ignoreService = context.getServiceCollection().getServiceUnchecked(IgnoreService.class);
        User target = context.getPlayerFromArgs();
        boolean isSelf = context.is(target);
        final IPlayerDisplayNameService playerDisplayNameService = context.getServiceCollection().playerDisplayNameService();

        List<Text> ignoredList = ignoreService
                .getAllIgnored(target.getUniqueId())
                .stream()
                .map(playerDisplayNameService::getDisplayName)
                .collect(Collectors.toList());

        if (ignoredList.isEmpty()) {
            if (isSelf) {
                context.sendMessage("command.ignorelist.noignores.self");
            } else {
                context.sendMessage("command.ignorelist.noignores.other", target);
            }
        } else {
            Util.getPaginationBuilder(context.getCommandSource())
                    .contents(ignoredList)
                    .title(context.getMessage("command.ignorelist.header", target))
                    .sendTo(context.getCommandSource());
        }
        return context.successResult();
    }

}
