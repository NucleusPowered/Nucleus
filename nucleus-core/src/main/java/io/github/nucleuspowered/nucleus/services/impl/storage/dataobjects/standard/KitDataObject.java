/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.standard;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.serialiser.SingleKitTypeSerilaiser;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.configurate.AbstractConfigurateBackedDataObject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KitDataObject extends AbstractConfigurateBackedDataObject implements IKitDataObject {

    private Map<String, Kit> cached;

    @Override
    public Map<String, Kit> getKitMap() {
        if (this.cached == null) {
            try {
                Map<String, Kit> map = SingleKitTypeSerilaiser.INSTANCE.deserialize(this.backingNode);
                if (map == null) {
                    this.cached = Collections.emptyMap();
                } else {
                    this.cached = Collections.unmodifiableMap(map);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        }
        return this.cached;
    }

    @Override
    public void setKitMap(Map<String, Kit> map) throws Exception {
        final ConfigurationNode node = this.backingNode.getNode("kits");
        this.backingNode.setValue(null); // clear for this serialisation
        SingleKitTypeSerilaiser.INSTANCE.serialize(map, this.backingNode);
        if (!node.isVirtual()) {
            this.backingNode.getNode("kits").setValue(node);
        }
        this.cached = ImmutableMap.copyOf(map);
    }

    @Override
    public boolean hasKit(String name) {
        return this.getKitMap().containsKey(name.toLowerCase());
    }

    @Override
    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(this.getKitMap().get(name.toLowerCase()));
    }

    @Override
    public void setKit(Kit kit) throws Exception {
        Map<String, Kit> m = new HashMap<>(getKitMap());
        m.put(kit.getName().toLowerCase(), kit);
        setKitMap(m);
    }

    @Override
    public boolean removeKit(String name) throws Exception {
        Map<String, Kit> m = new HashMap<>(getKitMap());
        boolean b = m.remove(name.toLowerCase()) != null;
        setKitMap(m);
        return b;
    }

    @Override
    public void setBackingNode(ConfigurationNode node) {
        super.setBackingNode(node);
        this.cached = null;
    }

}
