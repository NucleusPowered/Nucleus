/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.MailMessage;
import io.github.nucleuspowered.nucleus.api.service.NucleusMailService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.modules.mail.MailKeys;
import io.github.nucleuspowered.nucleus.modules.mail.data.MailData;
import io.github.nucleuspowered.nucleus.modules.mail.events.InternalNucleusMailEvent;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nullable;

@APIService(NucleusMailService.class)
public class MailHandler implements NucleusMailService, ServiceBase {

    @Override
    public final List<MailMessage> getMail(User player, MailFilter... filters) {
        return Lists.newArrayList(getMailInternal(player, filters));
    }

    public final List<MailMessage> getMailInternal(User player, MailFilter... filters) {
        List<MailMessage> data = Nucleus.getNucleus().getStorageManager().getUserService()
                .getOrNewOnThread(player.getUniqueId())
                .getNullable(MailKeys.MAIL_DATA);
        if (data == null || data.isEmpty()) {
            return ImmutableList.of();
        }

        if (filters.length == 0) {
            return ImmutableList.copyOf(data);
        }

        Predicate<MailMessage> lmf = Arrays.stream(filters).map(x -> (Predicate<MailMessage>)x).reduce(Predicate::and).orElse(x -> true);
        return data.stream().filter(lmf).collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean removeMail(User player, MailMessage mailData) {
        IUserDataObject dataObject = Nucleus.getNucleus().getStorageManager().getUserService().getOrNewOnThread(player.getUniqueId());
        List<MailMessage> data = dataObject.get(MailKeys.MAIL_DATA).orElseGet(ArrayList::new);
        boolean result = data.removeIf(x ->
                mailData.getDate().equals(x.getDate()) &&
                mailData.getMessage().equalsIgnoreCase(x.getMessage()) &&
                Objects.equals(mailData.getSender().orElse(null), x.getSender().orElse(null)));

        if (result) {
            dataObject.set(MailKeys.MAIL_DATA, data);
            Nucleus.getNucleus().getStorageManager().getUserService().save(player.getUniqueId(), dataObject);
        }

        return result;
    }

    @Override
    public void sendMail(@Nullable User playerFrom, User playerTo, String message) {
        IUserDataObject dataObject = Nucleus.getNucleus().getStorageManager().getUserService().getOrNewOnThread(playerTo.getUniqueId());

        MessageProvider provider = Nucleus.getNucleus().getMessageProvider();
        // Message is about to be sent. Send the event out. If canceled, then
        // that's that.
        if (Sponge.getEventManager().post(new InternalNucleusMailEvent(playerFrom, playerTo, message))) {
            if (playerFrom == null) {
                Sponge.getServer().getConsole().sendMessage(provider.getTextMessageWithFormat("message.cancel"));
            } else {
                playerFrom.getPlayer()
                        .ifPresent(x -> x.sendMessage(provider.getTextMessageWithFormat("message.cancel")));
            }
            return;
        }

        List<MailMessage> messages = dataObject.get(MailKeys.MAIL_DATA).orElseGet(ArrayList::new);
        MailData md = new MailData(playerFrom == null ? Util.consoleFakeUUID : playerFrom.getUniqueId(), Instant.now(), message);
        messages.add(md);
        dataObject.set(MailKeys.MAIL_DATA, messages);
        Nucleus.getNucleus().getStorageManager().getUserService().save(playerTo.getUniqueId(), dataObject);

        Text from = playerFrom == null ? Text.of(Sponge.getServer().getConsole().getName()) : Nucleus.getNucleus().getNameUtil().getName(playerFrom);
        playerTo.getPlayer().ifPresent(x ->
                x.sendMessage(Text.builder().append(provider.getTextMessageWithFormat("mail.youvegotmail")).append(Text.of(" ", from)).build()));
    }

    @Override
    public void sendMailFromConsole(User playerTo, String message) {
        sendMail(null, playerTo, message);
    }

    @Override
    public boolean clearUserMail(User player) {
        IUserDataObject dataObject = Nucleus.getNucleus().getStorageManager().getUserService().getOrNewOnThread(player.getUniqueId());
        if (dataObject.getNullable(MailKeys.MAIL_DATA) != null) {
            dataObject.remove(MailKeys.MAIL_DATA);
            Nucleus.getNucleus().getStorageManager().getUserService().save(player.getUniqueId(), dataObject);
            return true;
        }

        return false;
    }
}
