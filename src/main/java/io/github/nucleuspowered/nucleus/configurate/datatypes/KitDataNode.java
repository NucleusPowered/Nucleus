/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.datatypes;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.api.nucleusdata.KitResult;
import io.github.nucleuspowered.nucleus.api.nucleusdata.KitResultType;
import io.github.nucleuspowered.nucleus.configurate.wrappers.NucleusItemStackSnapshot;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.modules.kit.KitModule;
import io.github.nucleuspowered.nucleus.modules.kit.NucleusKitResult;
import io.github.nucleuspowered.nucleus.modules.kit.commands.kit.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.datamodules.KitUserDataModule;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ConfigSerializable
public class KitDataNode implements Kit {

    @Setting private List<NucleusItemStackSnapshot> stacks = Lists.newArrayList();

    /**
     * This is in seconds to be consistent with the rest of the plugin.
     */
    @Setting private long interval = 0;

    @Setting private double cost = 0;

    @Setting private boolean autoRedeem = false;

    @Setting private boolean oneTime = false;

    @Setting private boolean displayMessage = true;

    @Setting private boolean ignoresPermission = false;

    @Setting private boolean hidden = false;

    @Setting private List<String> commands = Lists.newArrayList();

    @Setting private boolean firstJoin = false;

    @Override
    public List<ItemStackSnapshot> getStacks() {
        return stacks.stream()
                .filter(x -> x.getSnapshot().getType() != ItemTypes.NONE)
                .map(NucleusItemStackSnapshot::getSnapshot).collect(Collectors.toList());
    }

    @Override
    public Kit setStacks(List<ItemStackSnapshot> stacks) {
        this.stacks = stacks == null ? Lists.newArrayList() : stacks.stream()
                .filter(x -> x.getType() != ItemTypes.NONE)
                .map(NucleusItemStackSnapshot::new).collect(Collectors.toList());
        return this;
    }

    @Override
    public Duration getInterval() {
        return Duration.ofSeconds(interval);
    }

    @Override
    public Kit setInterval(Duration interval) {
        this.interval = interval.getSeconds();
        return this;
    }

    @Override
    public double getCost() {
        return this.cost;
    }

    @Override
    public Kit setCost(double cost) {
        this.cost = cost;
        return this;
    }

    @Override
    public boolean isAutoRedeem() {
        return this.autoRedeem;
    }

    @Override
    public Kit setAutoRedeem(boolean autoRedeem) {
        this.autoRedeem = autoRedeem;
        return this;
    }

    @Override
    public boolean isOneTime() {
        return this.oneTime;
    }

    @Override
    public Kit setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
        return this;
    }

    @Override public List<String> getCommands() {
        return new ArrayList<>(this.commands);
    }

    @Override public Kit setCommands(List<String> commands) {
        this.commands = Preconditions.checkNotNull(commands);
        return this;
    }

    @Override public Kit updateKitInventory(Inventory inventory) {
        List<Inventory> slots = Lists.newArrayList(inventory.slots());
        final List<ItemStackSnapshot> stacks = slots.stream()
                .filter(x -> x.peek().isPresent() && x.peek().get().getItem() != ItemTypes.NONE)
                .map(x -> x.peek().get().createSnapshot()).collect(Collectors.toList());

        // Add all the stacks into the kit list.
        setStacks(stacks);
        return this;
    }

    @Override public Kit updateKitInventory(Player player) {
        return updateKitInventory(Util.getStandardInventory(player));
    }

    @Override public KitResult redeemKitItems(Player player, boolean performChecks) {
        KitResult result = new NucleusKitResult(KitResultType.SUCCESS);
        if (performChecks){
            result = performChecks(player);
            if (!result.successful()) return result;
        }
        //TODO: Move KitHandler#redeemKit to KitDataNode
        return result;
    }

    @Override public KitResult redeemKitCommands(Player player, boolean performChecks) {
        KitResult result = new NucleusKitResult(KitResultType.SUCCESS);
        if (performChecks){
            result = performChecks(player);
            if (!result.successful()) return result;
        }
        ConsoleSource source = Sponge.getServer().getConsole();
        String playerName = player.getName();
        getCommands().forEach(x -> Sponge.getCommandManager().process(source, x.replace("{{player}}", playerName)));
        return result;
    }

    @Override public KitResult performChecks(Player player) {
        CommandPermissionHandler cph = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(KitCommand.class);
        KitUserDataModule user = Nucleus.getNucleus().getUserDataManager().get(player.getUniqueId()).get().get(KitUserDataModule.class);
        KitConfigAdapter config = Nucleus.getNucleus().getConfigAdapter(KitModule.ID, KitConfigAdapter.class).get();

        String kitName = ""; // TODO: Need a way to retrieve the kit name.
        Optional<Instant> oi = Util.getValueIgnoreCase(user.getKitLastUsedTime(), kitName);
        Instant now = Instant.now();

        if (config.getNodeOrDefault().isSeparatePermissions()
            && !ignoresPermission()
            || !player.hasPermission(PermissionRegistry.PERMISSIONS_PREFIX + "kits." + kitName))
            return new NucleusKitResult(KitResultType.NO_PERMISSION);

        // If the kit was used before...
        if (oi.isPresent()) {

            // if it's one time only and the user does not have an exemption...
            if (isOneTime() && !player.hasPermission(cph.getPermissionWithSuffix("exempt.onetime"))) {
                // tell the user.
                return new NucleusKitResult(KitResultType.ALREADY_REDEEMED);
            }

            // If we have a cooldown for the kit, and we don't have permission to
            // bypass it...
            if (!cph.testCooldownExempt(player) && getInterval().getSeconds() > 0) {

                // ...and we haven't reached the cooldown point yet...
                Instant timeForNextUse = oi.get().plus(getInterval());
                if (timeForNextUse.isAfter(now)) {
                    Duration d = Duration.between(now, timeForNextUse);

                    // tell the user.
                    return new NucleusKitResult(KitResultType.ON_COOLDOWN, d);
                }
            }
        }

        if (player.getInventory().capacity() - player.getInventory().size() < getStacks().size())
            return new NucleusKitResult(KitResultType.INVENTORY_FULL);

        return new NucleusKitResult(KitResultType.SUCCESS);
    }

    @Override public boolean isDisplayMessageOnRedeem() {
        return this.displayMessage;
    }

    @Override public Kit setDisplayMessageOnRedeem(boolean displayMessage) {
        this.displayMessage = displayMessage;
        return this;
    }

    @Override public boolean ignoresPermission() {
        return this.ignoresPermission;
    }

    @Override public Kit setIgnoresPermission(boolean ignoresPermission) {
        this.ignoresPermission = ignoresPermission;
        return this;
    }

    @Override public boolean isHiddenFromList() {
        return this.hidden;
    }

    @Override public Kit setHiddenFromList(boolean hide) {
        this.hidden = hide;
        return this;
    }

    @Override public boolean isFirstJoinKit() {
        return this.firstJoin;
    }

    @Override public Kit setFirstJoinKit(boolean firstJoin) {
        this.firstJoin = firstJoin;
        if (this.firstJoin) {
            this.hidden = true;
        }

        return this;
    }
}
