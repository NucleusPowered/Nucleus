/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@EssentialsEquivalent("jails")
@Command(
        aliases = { "jails" },
        basePermission = JailPermissions.BASE_JAILS_LIST,
        commandDescriptionKey = "jails",
        async = true
)
public class JailsCommand implements ICommandExecutor<CommandSource> {

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        final JailHandler handler = context.getServiceCollection().getServiceUnchecked(JailHandler.class);
        Map<String, NamedLocation> mjs = handler.getJails();
        if (mjs.isEmpty()) {
            return context.errorResult("command.jails.nojails");
        }

        List<Text> lt = mjs.entrySet().stream()
                .map(x -> createJail(context, x.getValue(), x.getKey()))
                .collect(Collectors.toList());

        CommandSource src = context.getCommandSource();
        Util.getPaginationBuilder(src)
            .title(context.getMessage("command.jails.list.header")).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt).sendTo(src);
        return context.successResult();
    }

    private Text createJail(ICommandContext<? extends CommandSource> context, @Nullable NamedLocation data, String name) {
        if (data == null || !data.getLocation().isPresent()) {
            return Text.builder(name).color(TextColors.RED)
                    .onHover(TextActions.showText(context.getMessage("command.jails.unavailable"))).build();
        }

        Location<World> world = data.getLocation().get();
        Text.Builder inner = Text.builder(name).color(TextColors.GREEN).style(TextStyles.ITALIC)
                .onClick(TextActions.runCommand("/jails tp " + name))
                .onHover(TextActions.showText(context.getMessage("command.jails.warpprompt", name)));

        return Text.builder().append(inner.build())
                .append(context.getMessage("command.warps.warploc",
                        world.getExtent().getName(), world.getBlockPosition().toString())).build();
    }
}
