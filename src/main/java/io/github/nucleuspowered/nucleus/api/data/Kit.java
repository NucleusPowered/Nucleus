/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.data;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a kit in NucleusPlugin.
 */
public interface Kit {
    /**
     * Gets the stacks that would be given out by this kit.
     *
     * @return The {@link List} of {@link ItemStackSnapshot}s.
     */
    List<ItemStackSnapshot> getStacks();

    /**
     * Set the stacks that would be given out by this kit.
     *
     * @param stacks The {@link List} of {@link ItemStackSnapshot}s.
     * @return This {@link Kit}, for chaining.
     */
    Kit setStacks(List<ItemStackSnapshot> stacks);

    /**
     * Gets the cooldown time for the kit.
     *
     * @return The {@link Duration}
     */
    Duration getInterval();

    /**
     * Sets the cooldown time for the kit.
     *
     * @param interval The time the user has to wait before claiming the kit again.
     * @return This {@link Kit}, for chaining.
     */
    Kit setInterval(Duration interval);

    /**
     * The cost for claiming the kit.
     *
     * @return The cost.
     */
    double getCost();

    /**
     * Set the cost for this kit.
     *
     * @param cost The cost.
     * @return This {@link Kit}, for chaining.
     */
    Kit setCost(double cost);

    /**
     * Gets whether the kit is only allowed to be used one time, ever.
     *
     * @return <code>true</code> if so
     */
    boolean isOneTime();

    /**
     * Sets whether the kit is only allowed to be used one time, ever.
     *
     * @param oneTime Set <code>true</code> if the kit should only be used once, <code>false</code> otherwise.
     * @return this {@link Kit}, for chaining.
     */
    Kit setOneTime(boolean oneTime);

    /**
     * Convenience method for updating the kit with the contents of the player's inventory.
     *
     * @param inventory The inventory to get the kit from.
     * @return This {@link Kit} for chaining.
     */
    default Kit updateKitInventory(Inventory inventory) {
        List<Inventory> slots = Lists.newArrayList(inventory.slots());
        final List<ItemStackSnapshot> stacks = slots.stream().filter(x -> x.peek().isPresent()).map(x -> x.peek().get().createSnapshot()).collect(Collectors.toList());

        // Add all the stacks into the kit list.
        setStacks(stacks);
        return this;
    }

    /**
     * Convenience method for updating the kit with the contents of the player's inventory.
     *
     * @param player The player to get the kit from.
     * @return This {@link Kit} for chaining.
     */
    default Kit updateKitInventory(Player player) {
        return updateKitInventory(Util.getStandardInventory(player));
    }
}
