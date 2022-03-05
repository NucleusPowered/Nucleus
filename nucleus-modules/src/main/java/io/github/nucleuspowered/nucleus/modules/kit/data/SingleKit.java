/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.data;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.data.KitDataBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SingleKit implements Kit {

    private final String name;
    private final List<ItemStackSnapshot> stacks = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    @Nullable private Duration interval;
    private double cost = 0;
    private boolean autoRedeem = false;
    private boolean oneTime = false;
    private boolean displayOnRedeem = true;
    private boolean ignoresPermission = false;
    private boolean hidden = false;
    private boolean firstJoin = false;

    public SingleKit(final String name) {
        this.name = name;
    }

    public SingleKit(final String name, final Kit toClone) {
        this(name, toClone.getStacks(),
                toClone.getCooldown().orElse(null),
                toClone.getCost(),
                toClone.isAutoRedeem(),
                toClone.isOneTime(),
                toClone.isDisplayMessageOnRedeem(),
                toClone.ignoresPermission(),
                toClone.isHiddenFromList(),
                toClone.getCommands(),
                toClone.isFirstJoinKit());
    }

    public SingleKit(final String name,
            final List<ItemStackSnapshot> itemStackSnapshots,
            @Nullable final Duration interval, final double cost, final boolean autoRedeem, final boolean oneTime, final boolean displayOnRedeem,
            final boolean ignoresPermission, final boolean hidden, final List<String> commands, final boolean firstJoin) {
        this(name);
        this.stacks.addAll(itemStackSnapshots);
        this.interval = interval;
        this.cost = cost;
        this.autoRedeem = autoRedeem;
        this.oneTime = oneTime;
        this.displayOnRedeem = displayOnRedeem;
        this.ignoresPermission = ignoresPermission;
        this.hidden = hidden;
        this.firstJoin = firstJoin;
        this.commands.addAll(commands);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public List<ItemStackSnapshot> getStacks() {
        return new ArrayList<>(this.stacks);
    }

    @Override
    public Kit setStacks(final List<ItemStackSnapshot> stacks) {
        this.stacks.clear();
        this.stacks.addAll(stacks);
        return this;
    }

    @Override
    public Optional<Duration> getCooldown() {
        return Optional.ofNullable(this.interval);
    }

    @Override
    public Kit setCooldown(@Nullable final Duration interval) {
        this.interval = interval;
        return this;
    }

    @Override
    public double getCost() {
        return Math.max(0, this.cost);
    }

    @Override
    public Kit setCost(final double cost) {
        this.cost = Math.max(0, cost);
        return this;
    }

    @Override
    public boolean isAutoRedeem() {
        return this.autoRedeem;
    }

    @Override
    public Kit setAutoRedeem(final boolean autoRedeem) {
        this.autoRedeem = autoRedeem;
        return this;
    }

    @Override
    public boolean isOneTime() {
        return this.oneTime;
    }

    @Override
    public Kit setOneTime(final boolean oneTime) {
        this.oneTime = oneTime;
        return this;
    }

    @Override
    public List<String> getCommands() {
        return new ArrayList<>(this.commands);
    }

    @Override
    public Kit setCommands(final List<String> commands) {
        this.commands.clear();
        this.commands.addAll(commands);
        return this;
    }

    @Override
    public Kit updateKitInventory(final Inventory inventory) {
        final List<Inventory> slots = new ArrayList<>(inventory.slots());
        final List<ItemStackSnapshot> stacks = slots.stream()
                .filter(x -> !x.peek().isEmpty())
                .map(x -> x.peek().createSnapshot()).collect(Collectors.toList());

        // Add all the stacks into the kit list.
        return this.setStacks(stacks);
    }

    @Override
    public Kit updateKitInventory(final UUID player) {
        return this.updateKitInventory(Util.getStandardInventory(Sponge.server().player(player).get()));
    }

    @Override
    public void redeemKitCommands(final UUID player) {
        final SystemSubject source = Sponge.systemSubject();
        final ServerPlayer pl = Sponge.server().player(player).get();
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(pl);
            for (final String x : this.getCommands()) {
                try {
                    Sponge.server().commandManager().process(source, x.replace("{{player}}", pl.name()));
                } catch (final CommandException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isDisplayMessageOnRedeem() {
        return this.displayOnRedeem;
    }

    @Override
    public Kit setDisplayMessageOnRedeem(final boolean displayMessage) {
        this.displayOnRedeem = displayMessage;
        return this;
    }

    @Override
    public boolean ignoresPermission() {
        return this.ignoresPermission ;
    }

    @Override
    public Kit setIgnoresPermission(final boolean ignoresPermission) {
        this.ignoresPermission = ignoresPermission;
        return this;
    }

    @Override
    public boolean isHiddenFromList() {
        return this.hidden;
    }

    @Override
    public Kit setHiddenFromList(final boolean hide) {
        this.hidden = hide;
        return this;
    }

    @Override
    public boolean isFirstJoinKit() {
        return this.firstJoin;
    }

    @Override
    public Kit setFirstJoinKit(final boolean firstJoinKit) {
        this.firstJoin = firstJoinKit;
        return this;
    }

    @Override
    public int contentVersion() {
        return KitDataBuilder.CONTENT_VERSION;
    }

    @Override
    public DataContainer toContainer() {
        return KitDataBuilder.kitToContainer(this);
    }
}
