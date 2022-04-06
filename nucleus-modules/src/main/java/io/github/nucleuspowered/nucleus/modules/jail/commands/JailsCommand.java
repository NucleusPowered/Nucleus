/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EssentialsEquivalent("jails")
@Command(
        aliases = { "jails" },
        basePermission = JailPermissions.BASE_JAILS_LIST,
        commandDescriptionKey = "jails")
public class JailsCommand implements ICommandExecutor {

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final JailService handler = context.getServiceCollection().getServiceUnchecked(JailService.class);
        final Map<String, Jail> jails = handler.getJails();
        if (jails.isEmpty()) {
            return context.errorResult("command.jails.nojails");
        }

        final List<Component> lt = jails.entrySet().stream()
                .map(x -> this.createJail(context, x.getValue(), x.getKey()))
                .collect(Collectors.toList());

        Util.getPaginationBuilder(context.audience())
            .title(context.getMessage("command.jails.list.header")).padding(Component.text("-", NamedTextColor.GREEN))
            .contents(lt).sendTo(context.audience());
        return context.successResult();
    }

    private Component createJail(final ICommandContext context, @Nullable final Jail data, final String name) {
        if (data == null || !data.getLocation().getLocation().isPresent()) {
            return Component.text().content(name).color(NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(context.getMessage("command.jails.unavailable"))).build();
        }

        final ServerLocation world = data.getLocation().getLocation().get();
        final TextComponent.Builder inner = Component.text().content(name).color(NamedTextColor.GREEN).style(Style.style(TextDecoration.ITALIC))
                .clickEvent(ClickEvent.runCommand("/jails tp " + name))
                .hoverEvent(HoverEvent.showText(context.getMessage("command.jails.warpprompt", name)));

        return LinearComponents.linear(
                inner.build(),
                context.getMessage("command.warps.warploc", world.worldKey(), world.blockPosition().toString()));
    }
}
