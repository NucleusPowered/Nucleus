/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.category;

import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.serializer.TextSerializers;

@Command(
        aliases = "setdescription",
        basePermission = WarpPermissions.BASE_CATEGORY_DESCRIPTION,
        commandDescriptionKey = "warp.category.setdescription",
        parentCommand = CategoryCommand.class
)
public class CategoryDescriptionCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpCategoryElement(),
                NucleusParameters.DESCRIPTION
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WarpCategory category = context.requireOne(WarpService.WARP_CATEGORY_KEY, WarpCategory.class);
        String d = context.requireOne(NucleusParameters.Keys.DESCRIPTION, String.class);
        context.getServiceCollection().getServiceUnchecked(WarpService.class)
                .setWarpCategoryDescription(category.getId(), TextSerializers.FORMATTING_CODE.deserialize(d));
        context.sendMessage("command.warp.category.description.set", category.getId(), d);
        return context.successResult();
    }
}
