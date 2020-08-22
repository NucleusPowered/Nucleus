/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mail.parameter;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.mail.NucleusMailService;
import io.github.nucleuspowered.nucleus.modules.mail.services.MailHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class MailFilterArgument extends CommandElement {

    private static final Pattern late = Pattern.compile("b:(\\d+)");
    private static final Pattern early = Pattern.compile("a:(\\d+)");
    private final MailHandler handler;

    public MailFilterArgument(@Nullable Text key, MailHandler handler) {
        super(key);
        this.handler = handler;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        // Get all the arguments in list.
        List<UUID> players = Lists.newArrayList();
        boolean console = false;
        Instant ea = null;
        Instant l = null;
        List<String> message = Lists.newArrayList();
        while (args.hasNext()) {
            String toParse = args.next();
            try {
                String s = toParse.substring(0, 2);

                switch (s) {
                    case "p:":
                        player(toParse.split(":", 2)[1]).ifPresent(players::add);
                        break;
                    case "m:":
                        message.add(toParse.split(":", 2)[1]);
                        break;
                    case "c:":
                        console = true;
                        break;
                    case "b:":
                        Matcher b = late.matcher(toParse);
                        if (b.find()) {
                            // Days before
                            l = Instant.now().minus(Integer.parseInt(b.group(1)), ChronoUnit.DAYS);
                        }

                        break;
                    case "a:":
                        Matcher a = early.matcher(toParse);
                        if (a.find()) {
                            ea = Instant.now().minus(Integer.parseInt(a.group(1)), ChronoUnit.DAYS);
                        }

                        break;
                }
            } catch (Exception e) {
                // ignored
            }
        }

        List<NucleusMailService.MailFilter> lmf = Lists.newArrayList();
        if (console || !players.isEmpty()) {
            lmf.add(this.handler.createSenderFilter(console, players));
        }

        if (ea != null || l != null) {
            lmf.add(this.handler.createDateFilter(ea, l));
        }

        if (!message.isEmpty()) {
            lmf.add(this.handler.createMessageFilter(false, message));
        }

        return lmf.isEmpty() ? null : lmf;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Lists.newArrayList();
    }

    private Optional<UUID> player(String text) {
        if (text.equalsIgnoreCase("server") || (text.equalsIgnoreCase("console"))) {
            return Optional.of(Util.CONSOLE_FAKE_UUID);
        }

        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        Optional<User> ou = uss.get(text);
        return ou.map(Identifiable::getUniqueId);
    }

}
