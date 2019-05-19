/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.argumentparsers.UUIDArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.storage.services.IStorageService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Permissions(prefix = "nucleus", suggestedLevel = SuggestedLevel.OWNER)
@RunAsync
@NoModifiers
@NonnullByDefault
@RegisterCommand(value = "resetuser", subcommandOf = NucleusCommand.class)
public class ResetUserCommand extends AbstractCommand<CommandSource> {

    private final String userKey = "user";
    private final String uuidKey = "UUID";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("a", "-all").buildWith(
                GenericArguments.firstParsing(
                    GenericArguments.user(Text.of(this.userKey)),
                        new UUIDArgument<>(Text.of(this.uuidKey), u -> Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(u))
                ))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        final User user = args.<User>getOne(this.userKey).orElseGet(() -> args.<User>getOne(this.uuidKey).get());
        final boolean deleteall = args.hasAny("a");

        List<Text> messages = new ArrayList<>();

        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning"));
        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning2", user.getName()));
        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning3"));
        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning4"));
        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning5"));
        messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning6"));
        if (deleteall) {
            messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning8"));
        } else {
            messages.add(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.warning7"));
        }

        messages.add(Text.builder().append(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.reset")).style(TextStyles.UNDERLINE)
                .onClick(TextActions.executeCallback(new Delete(user, deleteall))).build());

        src.sendMessages(messages);
        return CommandResult.success();
    }

    private class Delete implements Consumer<CommandSource> {

        private final User user;
        private final boolean all;

        public Delete(User user, boolean all) {
            this.user = user;
            this.all = all;
        }

        @Override
        public void accept(CommandSource source) {
            if (this.user.isOnline()) {
                this.user.getPlayer().get().kick(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kick.defaultreason"));

                // Let Sponge do what it needs to close the user off.
                Task.builder().execute(() -> this.accept(source)).delayTicks(1).submit(Nucleus.getNucleus());
                return;
            }

            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.reset.starting", this.user.getName()));

            // Ban temporarily.
            final BanService bss = Sponge.getServiceManager().provideUnchecked(BanService.class);
            final boolean isBanned = bss.getBanFor(this.user.getProfile()).isPresent();
            bss.addBan(Ban.builder().type(BanTypes.PROFILE).expirationDate(Instant.now().plus(30, ChronoUnit.SECONDS)).profile(this.user.getProfile())
                    .build());

            final MessageProvider messageProvider = NucleusPlugin.getNucleus().getMessageProvider();

            // Unload the player in a second, just to let events fire.
            Sponge.getScheduler().createAsyncExecutor(Nucleus.getNucleus()).schedule(() -> {
                IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> userStorageService =
                        Nucleus.getNucleus().getStorageManager().getUserService();

                // Get the file to delete.
                try {
                    // Remove them from the cache immediately.
                    userStorageService.clearCache();
                    userStorageService.delete(this.user.getUniqueId());
                    if (this.all) {
                        String uuid = this.user.getUniqueId() + ".dat";
                        if (Sponge.getServiceManager().provideUnchecked(UserStorageService.class).delete(this.user)) {
                            // Sponge Data
                            Files.deleteIfExists(Sponge.getGame().getSavesDirectory().resolve("data/sponge").resolve(uuid));
                            source.sendMessage(messageProvider
                                    .getTextMessageWithFormat("command.nucleus.reset.completeall", this.user.getName()));
                        } else {
                            source.sendMessage(messageProvider.getTextMessageWithFormat("command.nucleus.reset.completenonm", this.user.getName()));
                        }
                    } else {
                        source.sendMessage(messageProvider
                                .getTextMessageWithFormat("command.nucleus.reset.complete", this.user.getName()));
                    }

                    source.sendMessage(messageProvider
                            .getTextMessageWithFormat("command.nucleus.reset.restartadvised", this.user.getName()));
                } catch (Exception e) {
                    source.sendMessage(messageProvider .getTextMessageWithFormat("command.nucleus.reset.failed", this.user.getName()));
                } finally {
                    if (!isBanned) {
                        bss.getBanFor(this.user.getProfile()).ifPresent(bss::removeBan);
                    }
                }
            } , 1, TimeUnit.SECONDS);
        }
    }
}
