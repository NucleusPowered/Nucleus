/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolKeys;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.item.ItemType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class PowertoolService implements ServiceBase {

    private final Map<UUID, Map<String, List<String>>> powertools = new HashMap<>();

    private final IStorageManager storageManager;

    @Inject
    public PowertoolService(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
    }


    public Map<String, List<String>> getPowertools(UUID uuid) {
        Map<String, List<String>> m = this.powertools.get(uuid);
        if (m == null) {
            // grab the user data
            m = this.storageManager.getUserService()
                    .getOrNewOnThread(uuid)
                    .get(PowertoolKeys.POWERTOOLS)
                    .orElseGet(HashMap::new);
            this.powertools.put(uuid, m);
        }

        return ImmutableMap.copyOf(m);
    }

    public Optional<List<String>> getPowertoolForItem(UUID uuid, ItemType item) {
        List<String> tools = getPowertools(uuid).get(item.getId());
        if (tools != null) {
            return Optional.of(ImmutableList.copyOf(tools));
        }

        return Optional.empty();
    }

    public void setPowertool(UUID uuid, ItemType type, List<String> commands) {
        getPowertools(uuid).put(type.getId(), commands);
        setBack(uuid);
    }

    public void clearPowertool(UUID uuid, ItemType type) {
        clearPowertool(uuid, type.getId());
    }

    public void clearPowertool(UUID uuid, String type) {
        getPowertools(uuid).remove(type);
        setBack(uuid);
    }

    public void reset(UUID uuid) {
        this.powertools.remove(uuid);
        setBack(uuid);
    }

    private void setBack(UUID uuid) {
        this.storageManager
                .getUserService()
                .getOrNew(uuid)
                .thenAccept(x -> x.set(PowertoolKeys.POWERTOOLS, this.powertools.getOrDefault(uuid, new HashMap<>())));
    }
}
