/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.argumentparsers;

import com.google.common.base.Preconditions;
import io.github.essencepowered.essence.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;

public class TwoPlayersArgument extends CommandElement {
    private final Text key;
    private final Text key2;
    private final String permission;

    // Workaround for https://github.com/SpongePowered/SpongeAPI/issues/1123
    public TwoPlayersArgument(@Nullable Text key, Text key2, @Nullable String requiredPermission) {
        super(key);
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(key2);
        this.key = key;
        this.key2 = key2;
        this.permission = requiredPermission;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        if (permission != null && !source.hasPermission(permission)) {
            // Temporary until we can use the GenericArguments.requiringPermission method again.
            throw args.createError(t("You do not have permission to use the %s argument", getKey()));
        }

        String sp1 = args.next();
        Optional<String> osp2 = args.nextIfPresent();

        if (!osp2.isPresent()) {
            throw args.createError(Text.of(TextColors.RED, Util.getMessageWithFormat("args.twoplayer.notenough")));
        }

        String sp2 = osp2.get();

        context.putArg(key.toPlain(), Sponge.getServer().getPlayer(sp1).orElseThrow(() -> args.createError(Text.of(TextColors.RED, Util.getMessageWithFormat("args.twoplayer.noexist", sp1)))));
        context.putArg(key2.toPlain(), Sponge.getServer().getPlayer(sp2).orElseThrow(() -> args.createError(Text.of(TextColors.RED, Util.getMessageWithFormat("args.twoplayer.noexist", sp2)))));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        try {
            String s = args.peek().toLowerCase();
            return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().startsWith(s.toLowerCase())).map(User::getName).collect(Collectors.toList());
        } catch (ArgumentParseException e) {
            return Sponge.getServer().getOnlinePlayers().stream().map(User::getName).collect(Collectors.toList());
        }
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of("(<player to teleport> <target>)");
    }
}
