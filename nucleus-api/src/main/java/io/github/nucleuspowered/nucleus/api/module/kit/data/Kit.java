/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.kit.data;

import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.NucleusKitService;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a kit in Nucleus.
 *
 * <p>Note that this kit requires explicit saving, see {@link #save()}</p>
 */
public interface Kit extends DataSerializable {

    /**
     * Creates a {@link Kit}.
     *
     * @see NucleusKitService#createKit(String)
     *
     * @param name The name of the kit to create
     * @return The {@link Kit}
     */
    static Kit create(final String name) {
        return NucleusAPI.getKitService().orElseThrow(() -> new IllegalStateException("The kit module is not loaded.")).createKit(name);
    }

    /**
     * Gets the name of the kit.
     *
     * @return The name
     */
    String getName();

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
    Optional<Duration> getCooldown();

    /**
     * Sets the cooldown time for the kit.
     *
     * @param interval The time the user has to wait before claiming the kit again.
     * @return This {@link Kit}, for chaining.
     * @deprecated Use {@link #setCooldown(Duration)} instead.
     */
    @Deprecated
    default Kit setInterval(final Duration interval) {
        return this.setCooldown(interval);
    }

    /**
     * Sets the cooldown time for the kit.
     *
     * @param cooldown The time the user has to wait before claiming the kit again.
     * @return This {@link Kit}, for chaining.
     */
    Kit setCooldown(Duration cooldown);

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
     * Gets whether the kit is automatically redeemed on login.
     *
     * @return <code>true</code> if so
     */
    boolean isAutoRedeem();

    /**
     * Sets whether the kit is automatically redeemed on login.
     *
     * @param autoRedeem Set <code>true</code> if the kit should automatically redeemed, <code>false</code> otherwise.
     * @return this {@link Kit}, for chaining.
     */
    Kit setAutoRedeem(boolean autoRedeem);

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
     * Gets the commands associated with this kit.
     *
     * @return The list of commands.
     */
    List<String> getCommands();

    /**
     * Sets the commands associated with this kit.
     *
     * @param commands The list of commands, with <code>{{player}}</code> as the token for the name of the player.
     * @return This {@link Kit} for chaining.
     */
    Kit setCommands(List<String> commands);

    /**
     * Adds a command to the {@link Kit}
     *
     * @param command The command to add.
     * @return This {@link Kit} for chaining.
     */
    default Kit addCommand(final String command) {
        final List<String> commands = this.getCommands();
        commands.add(command);
        return this.setCommands(commands);
    }

    /**
     * Convenience method for updating the kit with the contents of the player's inventory.
     *
     * @param inventory The inventory to get the kit from.
     * @return This {@link Kit} for chaining.
     */
    Kit updateKitInventory(Inventory inventory);

    /**
     * Convenience method for updating the kit with the contents of the player's inventory.
     *
     * @param player The player to get the kit from.
     * @return This {@link Kit} for chaining.
     */
    Kit updateKitInventory(UUID player);

    /**
     * Obtains a collection of items that a player would obtain when redeeming the kit.
     *
     * @see NucleusKitService#getItemsForPlayer(Kit, UUID)
     *
     * @param uuid The {@link UUID} of the player
     * @return The items
     */
    default Collection<ItemStack> getItemsForPlayer(final UUID uuid) {
        return NucleusAPI.getKitService().orElseThrow(() -> new IllegalStateException("No Kit module")).getItemsForPlayer(this, uuid);
    }

    /**
     * Attempts to redeem this kit, saving it beforehand.
     *
     * @param uuid The {@link UUID} of the player to redeem the kit for
     * @return The result
     */
    default KitRedeemResult redeem(final UUID uuid) {
        this.save();
        final NucleusKitService kitService = NucleusAPI.getKitService().orElseThrow(() -> new IllegalStateException("No Kit module"));
        return kitService.redeemKit(this, uuid, true);
    }

    /**
     * Redeems the commands in the kit, without redeeming the items.
     *
     * @param player The {@link UUID} that should redeem the commands.
     */
    void redeemKitCommands(UUID player);

    /**
     * Gets whether a message is displayed to the player when a kit is redeemed.
     *
     * @return <code>true</code> if the player will be notified if a player will be notified.
     */
    boolean isDisplayMessageOnRedeem();

    /**
     * Sets whether a message is displayed to the player when a kit is redeemed.
     *
     * @param displayMessage <code>true</code> to display a message to the player when they redeem a kit.
     * @return This {@link Kit} for chaining.
     */
    Kit setDisplayMessageOnRedeem(boolean displayMessage);

    /**
     * Gets whether redeeming this kit will ignore the permission checks.
     *
     * <p>
     *     Specifically, this is for when separate kit permissions are turned on in the Nucleus config, if this is
     *     <code>true</code>, this kit will never require a separate permission to redeem.
     * </p>
     *
     * @return <code>true</code> if this kit does not require a permission to redeem.
     */
    boolean ignoresPermission();

    /**
     * Sets whether redeeming this kit ignores permission checks.
     *
     * @param ignoresPermission Whether to ignore separate permission checks
     * @return This {@link Kit}, for chaining.
     */
    Kit setIgnoresPermission(boolean ignoresPermission);

    /**
     * Gets whether this kit is hidden from the <code>/kits</code> list (except for those with permission to view it).
     *
     * <p>
     *     If hidden, a player won't be able to tab complete to this either, but can still redeem the kit.
     * </p>
     *
     * @return Whether the kit is hidden from the <code>/kits</code> list and tab complete.
     */
    boolean isHiddenFromList();

    /**
     * Sets whether this kit is hidden from <code>/kits</code>
     *
     * @param hide <code>true</code> if so.
     * @return This {@link Kit}, for chaining.
     */
    Kit setHiddenFromList(boolean hide);

    /**
     * Returns whether this kit acts like a First Join Kit.
     *
     * @return <code>true</code> if so.
     */
    boolean isFirstJoinKit();

    /**
     * Sets whether this kit will be redeemed on first join.
     *
     * @param firstJoinKit <code>true</code> if so.
     * @return This {@link Kit}, for chaining.
     */
    Kit setFirstJoinKit(boolean firstJoinKit);

    /**
     * Saves this current kit's state.
     *
     * @throws IllegalStateException if the kit module isn't active.
     */
    default void save() {
        NucleusAPI.getKitService().orElseThrow(() -> new IllegalStateException("No Kit module")).saveKit(this);
    }

}
