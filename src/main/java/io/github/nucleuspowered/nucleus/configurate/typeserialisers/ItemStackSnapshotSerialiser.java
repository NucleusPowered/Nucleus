/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.typeserialisers;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.Util;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.List;
import java.util.Optional;

public class ItemStackSnapshotSerialiser implements TypeSerializer<ItemStackSnapshot> {

    private final Logger logger;

    public ItemStackSnapshotSerialiser(Logger logger) {
        this.logger = logger;
    }

    @Override
    public ItemStackSnapshot deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        // Process enchantments, temporary fix before Sponge gets a more general
        // fix in.
        ConfigurationNode ench = value.getNode("UnsafeData", "ench");
        if (!ench.isVirtual()) {
            List<? extends ConfigurationNode> enchantments = ench.getChildrenList();
            enchantments.forEach(x -> {
                try {
                    int id = Integer.parseInt(x.getNode("id").getString());
                    int lvl = Integer.parseInt(x.getNode("lvl").getString());

                    x.getNode("id").setValue(id);
                    x.getNode("lvl").setValue(lvl);
                } catch (NumberFormatException e) {
                    x.setValue(null);
                }
            });
        }

        Optional<ItemStackSnapshot> oiss = Sponge.getDataManager().deserialize(ItemStackSnapshot.class,
                DataTranslators.CONFIGURATION_NODE.translate(value));
        if (oiss.isPresent()) {
            return oiss.get();
        }

        // If we get here, we have had a problem with the data. We should
        // therefore remove all the data
        // (except enchants) and see what happens.
        ConfigurationNode unsafe = value.getNode("UnsafeData");
        unsafe.getChildrenMap().forEach((k, v) -> {
            if (!k.toString().equalsIgnoreCase("ench")) {
                v.setValue(null);
            }
        });

        // Try again
        oiss = Sponge.getDataManager().deserialize(ItemStackSnapshot.class, DataTranslators.CONFIGURATION_NODE.translate(value));
        if (oiss.isPresent()) {
            logger.warn(Util.getMessageWithFormat("config.itemstacksnapshot.data", value.getNode("ItemType").getString()));
            return oiss.get();
        }

        logger.warn(Util.getMessageWithFormat("config.itemstacksnapshot.unable", value.getNode("ItemType").getString()));

        // Return an empty snapshot
        return ItemStackSnapshot.NONE;
    }

    @Override
    public void serialize(TypeToken<?> type, ItemStackSnapshot obj, ConfigurationNode value) throws ObjectMappingException {
        DataContainer container = obj.toContainer();
        value.setValue(DataTranslators.CONFIGURATION_NODE.translate(container));
    }
}
