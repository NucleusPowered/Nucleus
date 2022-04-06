/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.serialiser;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.data.SingleKit;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SingleKitTypeSerialiser {

    public static final SingleKitTypeSerialiser INSTANCE = new SingleKitTypeSerialiser();

    private static final String STACKS = "stacks";
    private static final String INTERVAL = "interval";
    private static final String COST = "cost";
    private static final String AUTO_REDEEM = "autoRedeem";
    private static final String ONE_TIME = "oneTime";
    private static final String DISPLAY_MESSAGE = "displayMessage";
    private static final String IGNORES_PERMISSION = "ignoresPermission";
    private static final String HIDDEN = "hidden";
    private static final String COMMANDS = "commands";
    private static final String FIRST_JOIN = "firstJoin";

    private SingleKitTypeSerialiser() {}

    public Map<String, Kit> deserialize(@NonNull final ConfigurationNode value) throws ConfigurateException {
        final Map<String, Kit> kits = new HashMap<>();
        for (final Map.Entry<Object, ? extends ConfigurationNode> entry : value.childrenMap().entrySet()) {
            final String kitName = entry.getKey().toString().toLowerCase();

            final ConfigurationNode node = entry.getValue();
            if (!node.virtual()) {
                final List<ItemStackSnapshot> itemStackSnapshots =
                        node.node(STACKS).getList(ItemStackSnapshot.class);
                final long interval = node.node(INTERVAL).getLong(0);
                final double cost = node.node(COST).getDouble(0);
                final boolean autoRedeem = node.node(AUTO_REDEEM).getBoolean(false);
                final boolean oneTime = node.node(ONE_TIME).getBoolean(false);
                final boolean displayMessage = node.node(DISPLAY_MESSAGE).getBoolean(true);
                final boolean ignoresPermission = node.node(IGNORES_PERMISSION).getBoolean(false);
                final boolean hidden = node.node(HIDDEN).getBoolean(false);
                final List<String> commands = node.node(COMMANDS).getList(String.class);
                final boolean firstJoin = node.node(FIRST_JOIN).getBoolean(false);
                final Kit k = new SingleKit(kitName,
                        itemStackSnapshots,
                        Duration.ofSeconds(interval),
                        cost,
                        autoRedeem,
                        oneTime,
                        displayMessage,
                        ignoresPermission,
                        hidden,
                        commands,
                        firstJoin
                );
                kits.put(kitName, k);
            }
        }
        return kits;
    }

    public void serialize(@Nullable final Map<String, Kit> obj, @NonNull final ConfigurationNode value) throws ConfigurateException {
        if (obj != null) {
            for (final Map.Entry<String, Kit> entry : obj.entrySet()) {
                final Kit kit = entry.getValue();
                final ConfigurationNode node = value.node(entry.getKey().toLowerCase());
                node.node(STACKS).set(new TypeToken<List<ItemStackSnapshot>>() {}, kit.getStacks());
                node.node(INTERVAL).set(kit.getCooldown().map(Duration::getSeconds).orElse(0L));
                node.node(COST).set(kit.getCost());
                node.node(AUTO_REDEEM).set(kit.isAutoRedeem());
                node.node(ONE_TIME).set(kit.isOneTime());
                node.node(DISPLAY_MESSAGE).set(kit.isDisplayMessageOnRedeem());
                node.node(IGNORES_PERMISSION).set(kit.ignoresPermission());
                node.node(HIDDEN).set(kit.isHiddenFromList());
                node.node(COMMANDS).set(new TypeToken<List<String>>() {}, kit.getCommands());
                node.node(FIRST_JOIN).set(kit.isFirstJoinKit());
            }
        }
    }

}
