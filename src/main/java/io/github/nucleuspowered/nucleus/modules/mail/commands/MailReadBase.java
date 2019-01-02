/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.MailMessage;
import io.github.nucleuspowered.nucleus.api.service.NucleusMailService;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.mail.data.MailData;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Identifiable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MailReadBase implements InternalServiceManagerTrait {

    public static MailReadBase INSTANCE = new MailReadBase();

    private MailReadBase() {}

    private final MailHandler handler = getServiceUnchecked(MailHandler.class);
    static final String filters = "filters";

    public CommandResult executeCommand(CommandSource src, final User target, Collection<NucleusMailService.MailFilter> lmf) {
        List<MailMessage> lmd;
        if (!lmf.isEmpty()) {
            lmd = this.handler.getMailInternal(target, lmf.toArray(new NucleusMailService.MailFilter[0]));
        } else {
            lmd = this.handler.getMailInternal(target);
        }

        if (lmd.isEmpty()) {
            if (src instanceof Player && target.getUniqueId().equals(((Player) src).getUniqueId())) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(!lmf.isEmpty() ? "command.mail.none.filter" : "command.mail.none.normal.self"));
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(!lmf.isEmpty() ? "command.mail.none.filter" : "command.mail.none.normal.other", target.getName()));
            }

            return CommandResult.success();
        }

        List<Text> mails = lmd.stream().sorted(Comparator.comparing(MailMessage::getDate)).map(x -> createMessage(x, target)).collect(Collectors.toList());

        // Paginate the mail.
        PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        PaginationList.Builder b = ps.builder().padding(Text.of(TextColors.GREEN, "-")).title(getHeader(src, target, !lmf.isEmpty())).contents(mails);
        if (!(src instanceof Player)) {
            b.linesPerPage(-1);
        } else {
            b.header(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("mail.header"));
        }

        b.sendTo(src);
        return CommandResult.success();
    }

    private Text getHeader(CommandSource src, User user, boolean isFiltered) {
        if (src instanceof Player && user.getUniqueId().equals(((Player) src).getUniqueId())) {
            return Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(isFiltered ? "mail.title.filter.self" : "mail.title.nofilter.self");
        }

        return Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(isFiltered ? "mail.title.filter.other" : "mail.title.nofilter.other", user.getName());
    }

    private Text createMessage(final MailMessage md, final User user) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault());
        UUID uuid = getUuid(md);
        String name = Nucleus.getNucleus().getNameUtil().getNameFromUUID(uuid);
        return Text.builder()
                .append(Text.builder(name).color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                        .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.hover")))
                        .onClick(TextActions.executeCallback(src -> {
                            src.sendMessage(Text.builder().append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.date"))
                                    .append(Text.of(" ", TextColors.WHITE, dtf.format(md.getDate()))).build());
                            Text.Builder tb = Text.builder().append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.sender"))
                                    .append(Text.of(" ", TextColors.WHITE, Nucleus.getNucleus().getNameUtil().getNameFromUUID(uuid)))
                                    .append(Text.of(TextColors.YELLOW, " - "));

                            // If the sender is not the server, allow right of reply.
                            if (!uuid.equals(Util.consoleFakeUUID)) {
                                tb.append(Text.builder(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.reply")).color(TextColors.GREEN)
                                        .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.reply.label", name)))
                                        .onClick(TextActions.suggestCommand("/mail send " + name + " ")).build())
                                        .append(Text.of(TextColors.YELLOW, " - "));
                            }

                            src.sendMessage(tb.append(Text.builder(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.delete")).color(TextColors.RED)
                                .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.delete.label")))
                                .onClick(TextActions.executeCallback(s -> {
                                    if (this.handler.removeMail(user, md)) {
                                        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.delete.success"));
                                    } else {
                                        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.delete.fail"));
                                    }
                                })).build()).build());
                            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.mail.message"));
                            src.sendMessage(Text.of(TextColors.WHITE, md.getMessage()));
                        })).build())
                .append(Text.of(": " + md.getMessage())).build();
    }

    private UUID getUuid(MailMessage message) {
        if (message instanceof MailData) {
            return ((MailData) message).getUuid();
        } else {
            return message.getSender().map(Identifiable::getUniqueId).orElse(Util.consoleFakeUUID);
        }
    }

}
