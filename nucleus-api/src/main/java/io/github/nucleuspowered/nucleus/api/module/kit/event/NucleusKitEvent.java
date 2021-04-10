/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.kit.event;

import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.api.util.CancelMessageEvent;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Events that are related about kits.
 */
@NonnullByDefault
public interface NucleusKitEvent extends Event {

    /**
     * Fired when a kit is redeemed.
     *
     * <ul>
     *     <li>
     *         <code>Pre</code> is fired before the kit is redeemed, and the contents of the kit may be altered at this stage for this redemption
     *         only.
     *     </li>
     *     <li>
     *         <code>Post</code> is fired after a kit is redeemed successfully (defined as a kit would not be redeemable again if it was a one time
     *         kit).
     *     </li>
     * </ul>
     */
    interface Redeem extends NucleusKitEvent, TargetPlayerEvent {

        /**
         * Gets whether the redemption is due to a player joining the server for
         * the first time.
         *
         * @return true if the redemption is due to a first join
         */
        boolean isFirstJoin();

        /**
         * Gets the last time the kit was redeemed, if any.
         *
         * @return The {@link Instant} the kit was last redeemed.
         */
        Optional<Instant> getLastRedeemedTime();

        /**
         * Gets the name of the kit.
         *
         * @return The name of the kit.
         */
        String getName();

        /**
         * Gets the kit that has been redeemed.
         *
         * @return The kit that has been redeemed.
         */
        Kit getRedeemedKit();

        /**
         * Gets the {@link ItemStackSnapshot}s that the kit contains.
         *
         * <p>Note that other plugins <em>might</em> alter what is finally
         * redeemed by the kit, see {@link #getStacksToRedeem}</p>
         *
         * <p>If {@link #getStacksToRedeem()} is <code>null</code>, this
         * is what will be redeemed.</p>
         *
         * @return The {@link ItemStackSnapshot}s that would be redeemed.
         */
        Collection<ItemStackSnapshot> getOriginalStacksToRedeem();

        /**
         * Gets the {@link ItemStackSnapshot}s that will be redeemed, if a
         * plugin has altered it.
         *
         * <p>This might not represent what the kit actually contains, plugins
         * may alter this.</p>
         *
         * <p>For {@link Post}, this is what was actually redeemed.</p>
         *
         * @return The {@link ItemStackSnapshot}s that would be redeemed, if
         *         altered.
         */
        Optional<Collection<ItemStackSnapshot>> getStacksToRedeem();

        /**
         * Gets the commands that the kit will run.
         *
         * <p>Note that other plugins <em>might</em> alter what is finally
         * redeemed by the kit, see {@link #getCommandsToExecute()}</p>
         *
         * <p>If {@link #getCommandsToExecute()} ()} is <code>null</code>, this
         * is what will be executed. Note that these are run by the {@link ConsoleSource}.</p>
         *
         * <p>The token that represents the player in commands is <code>{{player}}</code></p>
         *
         * @return The commands to run.
         */
        Collection<String> getOriginalCommandsToExecute();


        /**
         * Gets the commands that the kit will run, if a plugin has set them.
         *
         * <p>This might not represent what the kit might actually contain. See
         * {@link #getOriginalCommandsToExecute()} for that.</p>
         *
         * <p>If this is {@link Optional#empty()}, then {@link #getOriginalCommandsToExecute()}
         * will be used.</p>
         *
         * <p>The token that represents the player in commands is <code>{{player}}</code></p>
         *
         * <p>Note that these are run by the {@link ConsoleSource}.</p>
         *
         * @return The commands to run.
         */
        Optional<Collection<String>> getCommandsToExecute();

        /**
         * Fired when a player is redeeming a kit.
         */
        interface Pre extends Redeem, CancelMessageEvent {

            /**
             * Override the content of the kit for this redemption only.
             *
             * @param stacksToRedeem the {@link ItemStackSnapshot}s that should be
             *                       redeemed. Set to null to return to original behaviour
             */
            void setStacksToRedeem(@Nullable Collection<ItemStackSnapshot> stacksToRedeem);

            /**
             * Override the commands run by this kit for this redemption only. Note that these
             * are run by the {@link ConsoleSource}.
             *
             * @param commandsToExecute the commands that should be redeemed, using
             *                          <code>{{player}}</code> as the player key.
             *                          Set to null to return to original behaviour.
             */
            void setCommandsToExecute(@Nullable Collection<String> commandsToExecute);
        }

        /**
         * Fired when a player's kit has been updated.
         */
        interface Post extends Redeem {}

        /**
         * Fired when a player's kit could not be updated.
         */
        interface Failed extends Redeem {

            /**
             * Gets the {@link KitRedeemResult.Status} that will be thrown by the routine.
             *
             * @return The reason.
             */
            KitRedeemResult.Status getReason();
        }
    }
}
