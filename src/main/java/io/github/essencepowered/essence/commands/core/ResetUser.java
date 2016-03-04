/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.core;

import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.argumentparsers.UserParser;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.*;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Permissions(root = "essence")
@RunAsync
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand(value = "resetuser", subcommandOf = ResetUser.class)
public class ResetUser extends CommandBase<CommandSource> {
    private final String userKey = "user";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().arguments(new UserParser(Text.of(userKey))).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        final User user = args.<User>getOne(userKey).get();

        List<Text> messages = new ArrayList<>();
        messages.add(Text.of(TextColors.DARK_RED, Util.getMessageWithFormat("command.essence.reset.warning")));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning2", user.getName())));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning3")));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning4")));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning5")));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning6")));
        messages.add(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.warning7")));
        messages.add(
            Text.builder(Util.getMessageWithFormat("command.essence.reset.reset")).color(TextColors.GREEN)
                .style(TextStyles.UNDERLINE).onClick(TextActions.executeCallback(new Delete(plugin, user))).build()
        );

        src.sendMessages(messages);
        return CommandResult.success();
    }

    private class Delete implements Consumer<CommandSource> {

        private final User user;
        private final Essence plugin;

        public Delete(Essence plugin, User user) {
            this.user = user;
            this.plugin = plugin;
        }

        @Override
        public void accept(CommandSource source) {
            if (user.isOnline()) {
                user.getPlayer().get().kick(Text.of(Util.getMessageWithFormat("command.kick.defaultreason")));
            }

            // Ban temporarily.
            final BanService bss = Sponge.getServiceManager().provideUnchecked(BanService.class);
            final boolean isBanned = bss.getBanFor(user.getProfile()).isPresent();
            bss.addBan(
                Ban.builder().expirationDate(Instant.now().plus(30, ChronoUnit.SECONDS)).profile(user.getProfile()).type(BanTypes.PROFILE).build()
            );

            // Unload the player in a second, just to let events fire.
            Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> {
                UserConfigLoader ucl = plugin.getUserLoader();

                // Remove them from the cache immediately.
                ucl.forceUnloadPlayerWithoutSaving(user.getUniqueId());

                // Get the file to delete.
                try {
                    Path file = ucl.getUserPath(user.getUniqueId());
                    Files.delete(file);
                    source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.complete", user.getName())));
                } catch (IOException e) {
                    source.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.essence.reset.failed", user.getName())));
                } finally {
                    if (!isBanned) {
                        bss.getBanFor(user.getProfile()).ifPresent(bss::removeBan);
                    }
                }
            }, 1, TimeUnit.SECONDS);
        }
    }
}
