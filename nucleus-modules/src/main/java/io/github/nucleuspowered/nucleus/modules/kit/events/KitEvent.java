/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.events;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.api.module.kit.event.NucleusKitEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
public abstract class KitEvent extends AbstractEvent implements NucleusKitEvent {

    public static abstract class Redeem extends KitEvent implements NucleusKitEvent.Redeem {

        private final Cause cause;
        private final Kit kit;
        @Nullable private final Instant lastTime;
        private final Player targetPlayer;
        private final Collection<ItemStackSnapshot> original;
        private final Collection<String> commands;

        public Redeem(Cause cause, @Nullable Instant lastTime, Kit kit, Player targetPlayer, Collection<ItemStackSnapshot> original,
                Collection<String> commands) {
            this.cause = cause;
            this.kit = kit;
            this.targetPlayer = targetPlayer;
            this.lastTime = lastTime;
            this.original = original;
            this.commands = commands;
        }

        @Override public Optional<Instant> getLastRedeemedTime() {
            return Optional.ofNullable(this.lastTime);
        }

        @Override public String getName() {
            return this.kit.getName();
        }

        @Override public Kit getRedeemedKit() {
            return this.kit;
        }

        @Override public Collection<ItemStackSnapshot> getOriginalStacksToRedeem() {
            return this.original;
        }

        @Override public Player getTargetEntity() {
            return this.targetPlayer;
        }

        @Override public Cause getCause() {
            return this.cause;
        }

        @Override public Collection<String> getOriginalCommandsToExecute() {
            return this.commands;
        }
    }

    public static class PreRedeem extends Redeem implements NucleusKitEvent.Redeem.Pre {

        @Nullable private Text cancelMessage = null;
        private boolean isCancelled;
        @Nullable private Collection<ItemStackSnapshot> toRedeem = null;
        @Nullable private Collection<String> commandRedeem = null;

        public PreRedeem(Cause cause, @Nullable Instant lastTime, Kit kit, Player targetPlayer, Collection<ItemStackSnapshot> original,
                Collection<String> originalCommands) {
            super(cause, lastTime, kit, targetPlayer, original, originalCommands);
        }

        @Override public boolean isCancelled() {
            return this.isCancelled;
        }

        @Override public void setCancelled(boolean cancel) {
            this.isCancelled = cancel;
        }

        @Override public Optional<Text> getCancelMessage() {
            return Optional.ofNullable(this.cancelMessage);
        }

        @Override public void setCancelMessage(@Nullable Text message) {
            this.cancelMessage = message;
        }

        @Override public Optional<Collection<ItemStackSnapshot>> getStacksToRedeem() {
            return Optional.ofNullable(this.toRedeem);
        }

        @Override public void setStacksToRedeem(@Nullable Collection<ItemStackSnapshot> stacksToRedeem) {
            if (stacksToRedeem == null) {
                this.toRedeem = null;
            } else {
                this.toRedeem = ImmutableList.copyOf(stacksToRedeem);
            }
        }

        @Override public Optional<Collection<String>> getCommandsToExecute() {
            return Optional.ofNullable(this.commandRedeem);
        }

        @Override public void setCommandsToExecute(@Nullable Collection<String> commandsToExecute) {
            if (commandsToExecute == null) {
                this.commandRedeem = null;
            } else {
                this.commandRedeem = ImmutableList.copyOf(commandsToExecute);
            }
        }
    }

    public static class PostRedeem extends Redeem implements NucleusKitEvent.Redeem.Post {

        @Nullable private final Collection<ItemStackSnapshot> redeemed;
        @Nullable private final Collection<String> commands;

        public PostRedeem(Cause cause, @Nullable Instant lastTime, Kit kit, Player targetPlayer, Collection<ItemStackSnapshot> original,
                @Nullable Collection<ItemStackSnapshot> redeemed, Collection<String> originalCommands, @Nullable Collection<String> commands) {
            super(cause, lastTime, kit, targetPlayer, original, originalCommands);
            this.redeemed = redeemed;
            this.commands = commands;
        }

        @Override public Optional<Collection<ItemStackSnapshot>> getStacksToRedeem() {
            return Optional.ofNullable(this.redeemed);
        }

        @Override public Optional<Collection<String>> getCommandsToExecute() {
            return Optional.ofNullable(this.commands);
        }
    }

    public static class FailedRedeem extends Redeem implements NucleusKitEvent.Redeem.Failed {

        @Nullable private final Collection<ItemStackSnapshot> redeemed;
        @Nullable private final Collection<String> commands;
        private final KitRedeemResult.Status ex;

        public FailedRedeem(Cause cause, @Nullable Instant lastTime, Kit kit, Player targetPlayer, Collection<ItemStackSnapshot> original,
                @Nullable Collection<ItemStackSnapshot> redeemed, Collection<String> originalCommands, @Nullable Collection<String> commands,
                KitRedeemResult.Status ex) {
            super(cause, lastTime, kit, targetPlayer, original, originalCommands);
            this.redeemed = redeemed;
            this.commands = commands;
            this.ex = ex;
        }

        @Override public Optional<Collection<ItemStackSnapshot>> getStacksToRedeem() {
            return Optional.ofNullable(this.redeemed);
        }

        @Override public Optional<Collection<String>> getCommandsToExecute() {
            return Optional.ofNullable(this.commands);
        }

        @Override public KitRedeemResult.Status getReason() {
            return this.ex;
        }
    }
}
