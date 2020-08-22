/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.Tuples;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

@NonnullByDefault
@Command(
        aliases = {"delete", "del"},
        basePermission = WorldPermissions.BASE_WORLD_DELETE,
        commandDescriptionKey = "world.delete",
        parentCommand = WorldCommand.class
)
public class DeleteWorldCommand implements ICommandExecutor<CommandSource> {

    @Nullable private Tuples.Tri<Instant, UUID, WorldProperties> confirm = null;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.WORLD_PROPERTIES_ALL.get(serviceCollection),
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties properties = context.requireOne(NucleusParameters.Keys.WORLD, WorldProperties.class);
        if (this.confirm != null && this.confirm.getFirst().isAfter(Instant.now()) &&
                this.confirm.getSecond().equals(context.getUniqueId().orElse(Util.CONSOLE_FAKE_UUID)) &&
                this.confirm.getThird().getUniqueId().equals(properties.getUniqueId())) {
            try {
                return completeDeletion(context, properties);
            } finally {
                this.confirm = null;
            }
        }

        this.confirm = null;
        if (Sponge.getServer().getWorld(properties.getUniqueId()).isPresent()) {
            return context.errorResult("command.world.delete.loaded", properties.getWorldName());
        }

        // Scary warning.
        this.confirm = Tuples.of(Instant.now().plus(30, ChronoUnit.SECONDS), context.getUniqueId().orElse(Util.CONSOLE_FAKE_UUID), properties);
        context.sendMessage("command.world.delete.warning1", properties.getWorldName());
        context.sendMessage("command.world.delete.warning3", properties.getWorldName());
        return context.successResult();
    }

    private ICommandResult completeDeletion(ICommandContext<? extends CommandSource> context, WorldProperties properties) throws CommandException {
        Preconditions.checkNotNull(this.confirm);
        String worldName = this.confirm.getThird().getWorldName();
        if (Sponge.getServer().getWorld(properties.getUniqueId()).isPresent()) {
            return context.errorResult("command.world.delete.loaded", this.confirm.getThird());
        }

        final ConsoleSource consoleSource = Sponge.getServer().getConsole();
        context.sendMessage("command.world.delete.confirmed", worldName);
        if (!context.is(consoleSource)) {
            context.sendMessageTo(consoleSource, "command.world.delete.confirmed", worldName);
        }

        // Now request deletion
        CompletableFuture<Boolean> completableFuture = Sponge.getServer().deleteWorld(properties);
        final Supplier<Optional<? extends CommandSource>> source;
        if (context.is(Player.class)) {
            final UUID uuid = context.getIfPlayer().getUniqueId();
            source = () -> Sponge.getServer().getPlayer(uuid);
        } else {
            source = Optional::empty;
        }

        Task.builder().async()
                .execute(task -> {
                    boolean result;
                    try {
                        result = completableFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        result = false;
                        e.printStackTrace();
                    }

                    if (!result) {
                        source.get().ifPresent(x -> {
                            context.sendMessageTo(x, "command.world.delete.complete.error", worldName);
                        });

                        context.sendMessageTo(consoleSource, "command.world.delete.complete.error", worldName);
                    } else {
                        source.get().ifPresent(x -> context.sendMessageTo(x, "command.world.delete.complete.success", worldName));
                        context.sendMessageTo(consoleSource, "command.world.delete.complete.success", worldName);
                    }
                }).submit(context.getServiceCollection().pluginContainer());
        return context.successResult();
    }

}
