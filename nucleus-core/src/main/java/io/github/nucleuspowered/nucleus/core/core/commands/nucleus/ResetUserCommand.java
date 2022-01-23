/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.ban.BanTypes;
import org.spongepowered.api.util.Ticks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Command(
        aliases = "resetuser",
        basePermission = CorePermissions.BASE_NUCLEUS_RESETUSER,
        commandDescriptionKey = "nucleus.resetuser",
        parentCommand = NucleusCommand.class
)
public class ResetUserCommand implements ICommandExecutor {

    private static final Parameter.Value<UUID> UUID_PARAMETER = Parameter.uuid().key("user").build();

    private final Map<UUID, Delete> callbacks = new HashMap<>();

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("a", "all")
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                Parameter.firstOf(
                        NucleusParameters.ONE_USER,
                        ResetUserCommand.UUID_PARAMETER
                )
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User user;
        final Optional<UUID> o = context.getOne(NucleusParameters.ONE_USER);
        if (o.isPresent()) {
            user = context.getUserFromArgs();
        } else {
            final UUID uuid = context.requireOne(ResetUserCommand.UUID_PARAMETER);
            user = Sponge.server().userManager().load(uuid).join().orElseThrow(() -> context.createException("args.uuid.notvalid.nomatch"));
        }
        final boolean deleteall = context.hasFlag("a");
        final UUID responsible = context.uniqueId().orElse(Util.CONSOLE_FAKE_UUID);

        final Audience targetAudience = context.audience();
        if (this.callbacks.containsKey(responsible)) {
            final Delete delete = this.callbacks.get(responsible);
            this.callbacks.remove(responsible);
            if (Instant.now().isBefore(delete.until) && delete.all == deleteall && delete.user.equals(user.uniqueId())) {
                // execute that callback
                delete.accept(targetAudience);
                return context.successResult();
            }
        }

        this.callbacks.values().removeIf(x -> Instant.now().isAfter(x.until));
        this.callbacks.remove(responsible);
        final List<Component> messages = new ArrayList<>();

        final IMessageProviderService messageProvider = context.getServiceCollection().messageProvider();

        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning"));
        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning2", user.name()));
        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning3"));
        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning4"));
        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning5"));
        messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning6"));
        if (deleteall) {
            messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning8"));
        } else {
            messages.add(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.warning7"));
        }

        this.callbacks.put(responsible,
                new Delete(Instant.now().plus(30, ChronoUnit.SECONDS),
                    user.uniqueId(),
                    deleteall,
                    context.getServiceCollection()));
        messages.add(Component.text().append(messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.reset"))
                .style(Style.style(TextDecoration.UNDERLINED))
                .clickEvent(SpongeComponents.executeCallback(cs -> {
                    this.callbacks.values().removeIf(x -> Instant.now().isAfter(x.until));
                    if (this.callbacks.containsKey(responsible)) {
                        final Delete delete = this.callbacks.get(responsible);
                        this.callbacks.remove(responsible);
                        if (Instant.now().isBefore(delete.until) && delete.all == deleteall && delete.user.equals(user.uniqueId())) {
                            // execute that callback
                            delete.accept(targetAudience);
                        }
                    } else {
                        messageProvider.getMessageFor(targetAudience, "command.nucleus.reset.resetfailed");
                    }

                    this.callbacks.remove(responsible);
                })).build());

        targetAudience.sendMessage(Component.join(Component.newline(), messages));
        return context.successResult();
    }

    private static class Delete implements Consumer<Audience> {

        private final UUID user;
        private final boolean all;
        private final INucleusServiceCollection serviceCollection;
        private final Instant until;

        Delete(final Instant until, final UUID user, final boolean all, final INucleusServiceCollection serviceCollection) {
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
        public void accept(final Audience source) {
            final IMessageProviderService messageProvider = this.serviceCollection.messageProvider();
            final User user = Sponge.server().userManager().load(this.user).join().get();
            if (user.isOnline()) {
                final ServerPlayer player = user.player().get();
                final Component kickReason = messageProvider.getMessageFor(player, "command.kick.defaultreason");
                player.kick(kickReason);

                // Let Sponge do what it needs to close the user off.
                final Task task =
                        Task.builder().execute(() -> this.accept(source)).delay(Ticks.of(1)).plugin(this.serviceCollection.pluginContainer()).build();
                Sponge.server().scheduler().submit(task);
                return;
            }

            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.starting", user.name()));

            // Ban temporarily.
            final BanService bss = Sponge.server().serviceProvider().banService();
            final CompletableFuture<Optional<? extends Ban>> future =
                    bss.add(Ban.builder().type(BanTypes.PROFILE)
                            .expirationDate(Instant.now().plus(30, ChronoUnit.SECONDS)).profile(user.profile())
                            .build());

            // Unload the player in a second, just to let events fire.
            Sponge.asyncScheduler().executor(this.serviceCollection.pluginContainer()).schedule(() -> {
                // TODO: Do this properly
                final Optional<? extends Ban> ban = future.join();
                final IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> userStorageService =
                        this.serviceCollection.storageManager().getUserService();

                // Get the file to delete.
                try {
                    // Remove them from the cache immediately.
                    userStorageService.clearCache();
                    final UUID uuid = user.uniqueId();
                    userStorageService.delete(uuid);
                    if (this.all) {
                        if (Sponge.server().userManager().delete(uuid).join()) {
                            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.completeall", user.name()));
                        } else {
                            source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.completenonm", user.name()));
                        }
                    } else {
                        source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.complete", user.name()));
                    }

                    source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.restartadvised", user.name()));
                } catch (final Exception e) {
                    source.sendMessage(messageProvider.getMessageFor(source, "command.nucleus.reset.failed", user.name()));
                } finally {
                    ban.ifPresent(bss::remove);
                }
            } , 1, TimeUnit.SECONDS);
        }
    }
}
