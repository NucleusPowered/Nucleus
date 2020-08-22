/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.lists;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.stream.Collectors;

@NonnullByDefault
public abstract class AvailableBaseCommand implements ICommandExecutor<CommandSource> {

    private final Class<? extends CatalogType> catalogType;
    private final String titleKey;

    AvailableBaseCommand(Class<? extends CatalogType> catalogType, String titleKey) {
        this.catalogType = catalogType;
        this.titleKey = titleKey;
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {

        List<Text> types = Sponge.getRegistry().getAllOf(this.catalogType).stream()
                .map(x -> context.getMessage("command.world.presets.item", x.getId(), x.getName()))
                .collect(Collectors.toList());

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
                .title(context.getMessage(this.titleKey))
                .contents(types)
                .sendTo(context.getCommandSourceUnchecked());

        return context.successResult();
    }
}
