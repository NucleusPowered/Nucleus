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
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@Command(
        aliases = "setdisplayname",
        basePermission = WarpPermissions.BASE_CATEGORY_DISPLAYNAME,
        commandDescriptionKey = "warp.category.setdisplayname",
        parentCommand = CategoryCommand.class,
        async = true
)
public class CategoryDisplayNameCommand implements ICommandExecutor<CommandSource> {

    private final String DISPLAY_NAME_KEY = "display name";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpCategoryElement(),
                GenericArguments.onlyOne(GenericArguments.string(Text.of(DISPLAY_NAME_KEY)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WarpCategory category = context.requireOne(WarpService.WARP_CATEGORY_KEY, WarpCategory.class);
        String displayName = context.requireOne(DISPLAY_NAME_KEY, String.class);
        context.getServiceCollection()
                .getServiceUnchecked(WarpService.class)
                .setWarpCategoryDisplayName(
                        category.getId(),
                        TextSerializers.FORMATTING_CODE.deserialize(displayName));
        context.sendMessage("command.warp.category.displayname.set", category.getId(), displayName);
        return context.successResult();
    }
}
