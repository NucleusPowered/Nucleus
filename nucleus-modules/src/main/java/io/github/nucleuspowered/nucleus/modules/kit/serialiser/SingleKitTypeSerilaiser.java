/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.serialiser;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.configurate.wrappers.NucleusItemStackSnapshot;
import io.github.nucleuspowered.nucleus.modules.kit.misc.SingleKit;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SingleKitTypeSerilaiser {

    public static final SingleKitTypeSerilaiser INSTANCE = new SingleKitTypeSerilaiser();

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

    private SingleKitTypeSerilaiser() {}

    @Nullable public Map<String, Kit> deserialize(@NonNull ConfigurationNode value)
            throws ObjectMappingException {
        Map<String, Kit> kits = new HashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : value.getChildrenMap().entrySet()) {
            String kitName = entry.getKey().toString().toLowerCase();

            ConfigurationNode node = entry.getValue();
            if (!node.isVirtual()) {
                List<ItemStackSnapshot> itemStackSnapshots =
                        node.getNode(STACKS)
                                .getList(TypeToken.of(NucleusItemStackSnapshot.class))
                                .stream()
                                .map(NucleusItemStackSnapshot::getSnapshot)
                                .map(ValueContainer::copy)
                                .collect(Collectors.toList());
                long interval = node.getNode(INTERVAL).getLong(0);
                double cost = node.getNode(COST).getDouble(0);
                boolean autoRedeem = node.getNode(AUTO_REDEEM).getBoolean(false);
                boolean oneTime = node.getNode(ONE_TIME).getBoolean(false);
                boolean displayMessage = node.getNode(DISPLAY_MESSAGE).getBoolean(true);
                boolean ignoresPermission = node.getNode(IGNORES_PERMISSION).getBoolean(false);
                boolean hidden = node.getNode(HIDDEN).getBoolean(false);
                List<String> commands = node.getNode(COMMANDS).getList(TypeTokens.STRING);
                boolean firstJoin = node.getNode(FIRST_JOIN).getBoolean(false);
                Kit k = new SingleKit(kitName,
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

    public void serialize(@Nullable Map<String, Kit> obj, @NonNull ConfigurationNode value)
            throws ObjectMappingException {
        if (obj != null) {
            for (Map.Entry<String, Kit> entry : obj.entrySet()) {
                Kit kit = entry.getValue();
                ConfigurationNode node = value.getNode(entry.getKey().toLowerCase());
                node.getNode(STACKS)
                        .setValue(new TypeToken<List<NucleusItemStackSnapshot>>() {},
                                kit.getStacks().stream().map(NucleusItemStackSnapshot::new).collect(Collectors.toList()));
                node.getNode(INTERVAL).setValue(kit.getCooldown().map(Duration::getSeconds).orElse(0L));
                node.getNode(COST).setValue(kit.getCost());
                node.getNode(AUTO_REDEEM).setValue(kit.isAutoRedeem());
                node.getNode(ONE_TIME).setValue(kit.isOneTime());
                node.getNode(DISPLAY_MESSAGE).setValue(kit.isDisplayMessageOnRedeem());
                node.getNode(IGNORES_PERMISSION).setValue(kit.ignoresPermission());
                node.getNode(HIDDEN).setValue(kit.isHiddenFromList());
                node.getNode(COMMANDS).setValue(new TypeToken<List<String>>() {}, kit.getCommands());
                node.getNode(FIRST_JOIN).setValue(kit.isFirstJoinKit());
            }
        }
    }

}
