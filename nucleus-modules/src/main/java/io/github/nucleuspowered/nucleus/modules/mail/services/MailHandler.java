/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;
import io.github.nucleuspowered.nucleus.api.module.mail.data.MailMessage;
import io.github.nucleuspowered.nucleus.modules.mail.MailKeys;
import io.github.nucleuspowered.nucleus.modules.mail.data.NucleusMailMessage;
import io.github.nucleuspowered.nucleus.modules.mail.events.InternalNucleusSendMailEvent;
import io.github.nucleuspowered.nucleus.modules.mail.parameter.MailFilterParameter;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.parameter.Parameter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@APIService(NucleusMailService.class)
public class MailHandler implements NucleusMailService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;
    private final Parameter.Value<MailFilter> mailFilterParameter;

    @Inject
    public MailHandler(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.mailFilterParameter = Parameter.builder(MailFilter.class)
                .consumeAllRemaining()
                .optional()
                .key("mail filters")
                .addParser(new MailFilterParameter(this, serviceCollection.messageProvider()))
                .build();
    }

    public Parameter.Value<MailFilter> getMailFilterParameter() {
        return this.mailFilterParameter;
    }

    @Override
    public final List<MailMessage> getMail(final UUID player, final MailFilter... filters) {
        return new ArrayList<>(this.getMailInternal(player, filters));
    }

    public final List<MailMessage> getMailInternal(final UUID player, final MailFilter... filters) {
        final List<MailMessage> data = this.serviceCollection.storageManager().getUserService()
                .getOrNewOnThread(player)
                .getNullable(MailKeys.MAIL_DATA);
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }

        if (filters.length == 0) {
            return Collections.unmodifiableList(data);
        }

        final Predicate<MailMessage> lmf = Arrays.stream(filters).map(x -> (Predicate<MailMessage>)x).reduce(Predicate::and).orElse(x -> true);
        return data.stream().filter(lmf).collect(Collectors.toList());
    }

    @Override
    public boolean removeMail(final UUID player, final MailMessage mailData) {
        final Optional<IUserDataObject> o = this.serviceCollection.storageManager().getUserService().getOnThread(player);
        if (!o.isPresent()) {
            return false;
        }
        final IUserDataObject dataObject = o.get();
        final List<MailMessage> data = dataObject.get(MailKeys.MAIL_DATA).orElseGet(ArrayList::new);
        final boolean result = data.removeIf(x ->
                mailData.getDate().equals(x.getDate()) &&
                mailData.getMessage().equalsIgnoreCase(x.getMessage()) &&
                Objects.equals(mailData.getSender().orElse(null), x.getSender().orElse(null)));

        if (result) {
            dataObject.set(MailKeys.MAIL_DATA, data);
            this.serviceCollection.storageManager().getUserService().save(player, dataObject);
        }

        return result;
    }

    @Override
    public void sendMail(@Nullable final UUID playerFrom, final UUID playerTo, final String message) {
        final IUserDataObject dataObject = this.serviceCollection.storageManager().getUserService().getOrNewOnThread(playerTo);

        // Message is about to be sent. Send the event out. If canceled, then
        // that's that.
        final IMessageProviderService messageProvider = this.serviceCollection.messageProvider();
        if (Sponge.eventManager().post(new InternalNucleusSendMailEvent(playerFrom, playerTo, message))) {
            if (playerFrom == null) {
                messageProvider.sendMessageTo(
                        Sponge.systemSubject(),
                        "message.cancel");
            } else {
                Sponge.server().player(playerFrom)
                        .ifPresent(x -> messageProvider.sendMessageTo(x, "message.cancel"));
            }
            return;
        }

        final List<MailMessage> messages = dataObject.get(MailKeys.MAIL_DATA).orElseGet(ArrayList::new);
        final NucleusMailMessage md = new NucleusMailMessage(playerFrom == null ? Util.CONSOLE_FAKE_UUID : playerFrom, Instant.now(), message);
        messages.add(md);
        dataObject.set(MailKeys.MAIL_DATA, messages);
        this.serviceCollection.storageManager().getUserService().save(playerTo, dataObject);

        final Component from = this.serviceCollection.playerDisplayNameService().getDisplayName(md.getSenderUUID());
        Sponge.server().player(playerTo).ifPresent(x ->
                x.sendMessage(LinearComponents.linear(messageProvider.getMessageFor(x, "mail.youvegotmail"), Component.space(), from)));
    }

    @Override
    public void sendMailFromConsole(final UUID playerTo, final String message) {
        this.sendMail(null, playerTo, message);
    }

    @Override
    public boolean clearUserMail(final UUID player) {
        final IStorageManager storageManager = this.serviceCollection.storageManager();
        final IUserDataObject dataObject = storageManager.getUserService().getOnThread(player).orElse(null);
        if (dataObject != null && dataObject.getNullable(MailKeys.MAIL_DATA) != null) {
            dataObject.remove(MailKeys.MAIL_DATA);
            storageManager.getUserService().save(player, dataObject);
            return true;
        }

        return false;
    }

}
