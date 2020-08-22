/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.UUIDArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.storage.services.IStorageService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@NonnullByDefault
@Command(
        aliases = "resetuser",
        basePermission = CorePermissions.BASE_NUCLEUS_RESETUSER,
        commandDescriptionKey = "nucleus.resetuser",
        parentCommand = NucleusCommand.class,
        async = true
)
public class ResetUserCommand implements ICommandExecutor<CommandSource> {

    private final String userKey = "user";
    private final String uuidKey = "UUID";

    private final Map<UUID, Delete> callbacks = new HashMap<>();

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("a", "-all").buildWith(
                GenericArguments.firstParsing(
                    GenericArguments.user(Text.of(this.userKey)),
                        new UUIDArgument<>(Text.of(this.uuidKey),
                                u -> Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(u),
                                serviceCollection)
                ))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        final User user = context.getOne(this.userKey, User.class).orElseGet(() -> context.requireOne(this.uuidKey, User.class));
        final boolean deleteall = context.hasAny("a");
        final UUID responsible = context.getUniqueId().orElse(Util.CONSOLE_FAKE_UUID);

        if (this.callbacks.containsKey(responsible)) {
            Delete delete = this.callbacks.get(responsible);
            this.callbacks.remove(responsible);
            if (Instant.now().isBefore(delete.until) && delete.all == deleteall && delete.user.equals(user.getUniqueId())) {
                // execute that callback
                delete.accept(context.getCommandSource());
                return context.successResult();
            }
        }

        this.callbacks.values().removeIf(x -> Instant.now().isAfter(x.until));
        this.callbacks.remove(responsible);
        List<Text> messages = new ArrayList<>();

        IMessageProviderService messageProvider = context.getServiceCollection().messageProvider();
        CommandSource source = context.getCommandSource();

        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning"));
        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning2", user.getName()));
        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning3"));
        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning4"));
        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning5"));
        messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning6"));
        if (deleteall) {
            messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning8"));
        } else {
            messages.add(messageProvider.getMessageFor(source, "command.nucleus.reset.warning7"));
        }

        this.callbacks.put(responsible,
                new Delete(Instant.now().plus(30, ChronoUnit.SECONDS),
                    user.getUniqueId(),
                    deleteall,
                    context.getServiceCollection()));
        messages.add(Text.builder().append(messageProvider.getMessageFor(source, "command.nucleus.reset.reset")).style(TextStyles.UNDERLINE)
                .onClick(TextActions.executeCallback(cs -> {
                    this.callbacks.values().removeIf(x -> Instant.now().isAfter(x.until));
                    if (this.callbacks.containsKey(responsible)) {
                        Delete delete = this.callbacks.get(responsible);
                        this.callbacks.remove(responsible);
                        if (Instant.now().isBefore(delete.until) && delete.all == deleteall && delete.user.equals(user.getUniqueId())) {
                            // execute that callback
                            delete.accept(cs);
                        }
                    } else {
                        messageProvider.getMessageFor(source, "command.nucleus.reset.resetfailed");
                    }

                    this.callbacks.remove(responsible);
                })).build());

        source.sendMessages(messages);
        return context.successResult();
    }

    private static class Delete implements Consumer<CommandSource> {

        private final UUID user;
        private final boolean all;
        private final INucleusServiceCollection serviceCollection;
        private final Instant until;

        Delete(Instant until, UUID user, boolean all, INucleusServiceCollection serviceCollection) {
            this.user = user;
            this.all = all;
            this.serviceCollection = serviceCollection;
            this.until = until;
        }

        public UUID getUser() {
            return this.user;
        }

        public boolean isAll() {
            return this.all;
        }

        public Instant getUntil() {
            return this.until;
        }

        @Override
        public void accept(final CommandSource source) {
            IMessageProviderService messageProvider = this.serviceCollection.messageProvider();
            final User user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(this.user).get();
            if (user.isOnline()) {
                Player player = user.getPlayer().get();
                Text kickReason = messageProvider.getMessageFor(player, "command.kick.defaultreason");
                player.kick(kickReason);

                // Let Sponge do what it needs to close the user off.
                Task.builder().execute(() -> this.accept(source)).delayTicks(1).submit(this.serviceCollection.pluginContainer());
                return;
            }

            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.starting", user.getName()));

            // Ban temporarily.
            final BanService bss = Sponge.getServiceManager().provideUnchecked(BanService.class);
            final boolean isBanned = bss.getBanFor(user.getProfile()).isPresent();
            bss.addBan(Ban.builder().type(BanTypes.PROFILE).expirationDate(Instant.now().plus(30, ChronoUnit.SECONDS)).profile(user.getProfile())
                    .build());

            // Unload the player in a second, just to let events fire.
            Sponge.getScheduler().createAsyncExecutor(this.serviceCollection.pluginContainer()).schedule(() -> {
                IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> userStorageService =
                        this.serviceCollection.storageManager().getUserService();

                // Get the file to delete.
                try {
                    // Remove them from the cache immediately.
                    userStorageService.clearCache();
                    userStorageService.delete(user.getUniqueId());
                    if (this.all) {
                        String uuid = user.getUniqueId() + ".dat";
                        if (Sponge.getServiceManager().provideUnchecked(UserStorageService.class).delete(user)) {
                            // Sponge Data
                            Files.deleteIfExists(Sponge.getGame().getSavesDirectory().resolve("data/sponge").resolve(uuid));
                            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.completeall", user.getName()));
                        } else {
                            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.completenonm", user.getName()));
                        }
                    } else {
                        source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.complete", user.getName()));
                    }

                    source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.restartadvised", user.getName()));
                } catch (Exception e) {
                    source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.failed", user.getName()));
                } finally {
                    if (!isBanned) {
                        bss.getBanFor(user.getProfile()).ifPresent(bss::removeBan);
                    }
                }
            } , 1, TimeUnit.SECONDS);
        }
    }
}
