/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.parameter;

import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.util.Identifiable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailFilterParameter implements ValueParameter<NucleusMailService.MailFilter> {

    private static final Pattern late = Pattern.compile("b:(\\d+)");
    private static final Pattern early = Pattern.compile("a:(\\d+)");
    private final MailHandler handler;
    private final IMessageProviderService messageProviderService;

    public MailFilterParameter(final MailHandler handler, final IMessageProviderService messageProviderService) {
        this.handler = handler;
        this.messageProviderService = messageProviderService;
    }

    private Optional<UUID> player(final String text) {
        if (text.equalsIgnoreCase("server") || (text.equalsIgnoreCase("console")) || text.equalsIgnoreCase("-")) {
            return Optional.of(Util.CONSOLE_FAKE_UUID);
        }

        return Sponge.server().gameProfileManager().cache().findByName(text).map(Identifiable::uniqueId);
    }

    @Override
    public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
        return Collections.emptyList();
    }

    @Override
    public Optional<? extends NucleusMailService.MailFilter> parseValue(final Parameter.Key<? super NucleusMailService.MailFilter> parameterKey,
            final ArgumentReader.Mutable reader, final CommandContext.Builder context) throws ArgumentParseException {
        final String toParse = reader.parseString();
        try {
            final String s = toParse.substring(0, 2);

            switch (s) {
                case "p:":
                    final String playerName = toParse.split(":", 2)[1];
                    final UUID uuid = this.player(playerName).orElseThrow(() -> reader.createException(
                            this.messageProviderService.getMessageFor(context.cause().audience(), "command.mail.param.noplayer", playerName)
                    ));
                    return Optional.of(this.handler.createSenderFilter(false, Collections.singleton(uuid)));
                case "m:":
                    return Optional.of(this.handler.createMessageFilter(false, toParse.split(":", 2)[1]));
                case "c:":
                    return Optional.of(this.handler.createSenderFilter(true, Collections.emptyList()));
                case "b:":
                    final Matcher b = late.matcher(toParse);
                    if (b.find()) {
                        // Days before
                        final Instant l = Instant.now().minus(Integer.parseInt(b.group(1)), ChronoUnit.DAYS);
                        return Optional.of(this.handler.createDateFilter(null, l));
                    }

                    throw reader.createException(
                            this.messageProviderService.getMessageFor(context.cause().audience(), "command.mail.param.invalidtime")
                    );
                case "a:":
                    final Matcher a = early.matcher(toParse);
                    if (a.find()) {
                        // Days before
                        final Instant l = Instant.now().minus(Integer.parseInt(a.group(1)), ChronoUnit.DAYS);
                        return Optional.of(this.handler.createDateFilter(l, null));
                    }

                    throw reader.createException(
                            this.messageProviderService.getMessageFor(context.cause().audience(), "command.mail.param.invalidtime")
                    );
            }
        } catch (final Exception e) {
            // ignored
        }

        throw reader.createException(this.messageProviderService.getMessageFor(context.cause().audience(), "command.mail.param.invalidfilter"));
    }
}
