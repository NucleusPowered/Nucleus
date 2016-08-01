/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.modules.warn.handlers.WarnHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WarningArgument extends CommandElement {

    private Nucleus plugin;
    private WarnHandler handler;

    public WarningArgument(@Nullable Text key, Nucleus plugin, WarnHandler handler) {
        super(key);
        this.plugin = plugin;
        this.handler = handler;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        Optional<String> optPlayer = args.nextIfPresent();
        if (!optPlayer.isPresent()) {
            throw args.createError(Util.getTextMessageWithFormat("args.warning.nouserarg"));
        }
        String player = optPlayer.get();

        Optional<User> optUser = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(player);
        if (!optUser.isPresent()) {
            throw args.createError(Util.getTextMessageWithFormat("args.warning.nouser", player));
        }
        User user = optUser.get();

        Optional<String> optIndex = args.nextIfPresent();
        if (!optIndex.isPresent()) {
            throw args.createError(Util.getTextMessageWithFormat("args.warning.noindex", user.getName()));
        }

        List<WarnData> warnData = handler.getWarnings(user);
        int index;
        try {
            index = Integer.parseInt(optIndex.get()) - 1;
            if (index >= warnData.size() || index < 0) {
                throw args.createError(Util.getTextMessageWithFormat("args.warning.nowarndata", optIndex.get(), user.getName()));
            }
        } catch (NumberFormatException ex) {
            throw args.createError(Util.getTextMessageWithFormat("args.warning.indexnotnumber"));
        }

        if (!warnData.isEmpty()) {
            return new Result(user, warnData.get(index));
        }

        throw args.createError(Util.getTextMessageWithFormat("args.warning.nouserwarnings",user.getName()));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Collections.emptyList();
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of("<user> <ID>");
    }

    public static class Result {
        public final User user;
        public final WarnData warnData;

        public Result(User user, WarnData warnData) {
            this.user = user;
            this.warnData = warnData;
        }
    }
}
