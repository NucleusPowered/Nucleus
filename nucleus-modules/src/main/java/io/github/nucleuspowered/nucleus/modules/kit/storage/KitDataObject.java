/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.storage;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KitDataObject implements IKitDataObject {

    private final Map<String, Kit> kits = new HashMap<>();

    @Override
    public Map<String, Kit> getKitMap() {
        return Collections.unmodifiableMap(this.kits);
    }

    @Override
    public void setKitMap(final Map<String, Kit> map) {
        this.kits.clear();
        this.kits.putAll(map);
    }

    @Override
    public boolean hasKit(final String name) {
        return this.getKitMap().containsKey(name.toLowerCase());
    }

    @Override
    public Optional<Kit> getKit(final String name) {
        return Optional.ofNullable(this.getKitMap().get(name.toLowerCase()));
    }

    @Override
    public void setKit(final Kit kit) {
        final Map<String, Kit> m = new HashMap<>(this.getKitMap());
        m.put(kit.getName().toLowerCase(), kit);
        this.setKitMap(m);
    }

    @Override
    public boolean removeKit(final String name) {
        final Map<String, Kit> m = new HashMap<>(this.getKitMap());
        final boolean b = m.remove(name.toLowerCase()) != null;
        this.setKitMap(m);
        return b;
    }

}
