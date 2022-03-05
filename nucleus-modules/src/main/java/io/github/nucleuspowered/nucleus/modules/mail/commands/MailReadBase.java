/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;
import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.modules.mail.data.NucleusMailMessage;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.pagination.PaginationList;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MailReadBase {

    public static MailReadBase INSTANCE = new MailReadBase();

    private MailReadBase() {}

    public ICommandResult executeCommand(final ICommandContext context,
            final UUID target,
            final Collection<? extends NucleusMailService.MailFilter> lmf) {
        final MailHandler handler = context.getServiceCollection().getServiceUnchecked(MailHandler.class);
        final List<MailMessage> lmd;
        if (!lmf.isEmpty()) {
            lmd = handler.getMailInternal(target, lmf.toArray(new NucleusMailService.MailFilter[0]));
        } else {
            lmd = handler.getMailInternal(target);
        }

        final String name =
                Sponge.server().gameProfileManager().cache()
                        .findById(target)
                        .flatMap(GameProfile::name)
                        .orElseGet(() -> context.getMessageString("standard.unknown"));
        final boolean isSelf = context.is(target);
        if (lmd.isEmpty()) {
            if (isSelf) {
                context.sendMessage(!lmf.isEmpty() ? "command.mail.none.filter" : "command.mail.none.normal.self");
            } else {
                context.sendMessage(!lmf.isEmpty() ? "command.mail.none.filter" : "command.mail.none.normal.other", name);
            }

            return context.successResult();
        }

        final List<Component> mails = lmd.stream().sorted(Comparator.comparing(MailMessage::getDate))
                        .map(x -> this.createMessage(context, x, target, name)).collect(Collectors.toList());

        // Paginate the mail.
        final PaginationList.Builder b = Util.getPaginationBuilder(context.audience()).padding(Component.text("-", NamedTextColor.GREEN)).title(
                this.getHeader(context, isSelf ? null : name, !lmf.isEmpty())).contents(mails);
        b.sendTo(context.audience());
        return context.successResult();
    }

    private Component getHeader(final ICommandContext context, @Nullable final String name, final boolean isFiltered) {
        if (name == null) {
            return context.getMessage(isFiltered ? "mail.title.filter.self" : "mail.title.nofilter.self");
        }

        return  context.getMessage(isFiltered ? "mail.title.filter.other" : "mail.title.nofilter.other", name);
    }

    private Component createMessage(final ICommandContext context, final MailMessage md, final UUID user, final String name) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        final UUID uuid = this.getUuid(md);
        final Component fromName = context.getServiceCollection().playerDisplayNameService().getDisplayName(uuid);
        return Component.text().append(fromName).color(NamedTextColor.GREEN).style(Style.style(TextDecoration.UNDERLINED))
                        .hoverEvent(HoverEvent.showText(context.getMessage("command.mail.hover")))
                        .clickEvent(SpongeComponents.executeCallback(src -> {
                            context.sendMessageText( LinearComponents.linear(
                                    context.getMessage("command.mail.date"),
                                    Component.space(),
                                    Component.text(dtf.format(md.getDate()), NamedTextColor.WHITE)
                            ));
                            context.sendMessageText(LinearComponents.linear(
                                    context.getMessage("command.mail.sender"),
                                    Component.space(),
                                    fromName,
                                    Component.text(" - ", NamedTextColor.YELLOW)
                            ));

                            // If the sender is not the server, allow right of reply.
                            final TextComponent.Builder builder = Component.text();
                            if (!uuid.equals(Util.CONSOLE_FAKE_UUID)) {
                                builder.append(
                                        Component.text()
                                                .append(context.getMessage("standard.reply"))
                                                .color(NamedTextColor.GREEN)
                                                .clickEvent(ClickEvent.suggestCommand("/nucleus:mail send " + name + " "))
                                                .hoverEvent(HoverEvent.showText(context.getMessage("command.mail.reply.label", name)))
                                ).append(Component.text(" - ", NamedTextColor.YELLOW));
                            }

                            builder.append(
                                    Component.text()
                                        .append(context.getMessage("standard.delete"))
                                        .hoverEvent(HoverEvent.showText(context.getMessage("command.mail.delete.label")))
                                        .clickEvent(SpongeComponents.executeCallback(s -> {
                                            if (context.getServiceCollection().getServiceUnchecked(MailHandler.class).removeMail(user, md)) {
                                                context.sendMessage("command.mail.delete.success");
                                            } else {
                                                context.sendMessage("command.mail.delete.fail");
                                            }
                                        })).build()
                            );

                            context.sendMessageText(builder.build());
                            context.sendMessage("command.mail.message");
                            context.sendMessageText(Component.text(md.getMessage()));
                        })).append(Component.text(": " + md.getMessage())).build();
    }

    private UUID getUuid(final MailMessage message) {
        if (message instanceof NucleusMailMessage) {
            return ((NucleusMailMessage) message).getSenderUUID();
        } else {
            return message.getSender().orElse(Util.CONSOLE_FAKE_UUID);
        }
    }

}
