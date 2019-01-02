/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.datamodules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;
import org.spongepowered.api.item.ItemType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PowertoolUserDataModule implements IUserDataModule {

    @DataKey("powertools")
    private Map<String, List<String>> powertools = Maps.newHashMap();

    public Map<String, List<String>> getPowertools() {
        return ImmutableMap.copyOf(this.powertools);
    }

    public Optional<List<String>> getPowertoolForItem(ItemType item) {
        List<String> tools = this.powertools.get(item.getId());
        if (tools != null) {
            return Optional.of(ImmutableList.copyOf(tools));
        }

        return Optional.empty();
    }

    public void setPowertool(ItemType type, List<String> commands) {
        this.powertools.put(type.getId(), commands);
    }

    public void clearPowertool(ItemType type) {
        this.powertools.remove(type.getId());
    }

    public void clearPowertool(String type) {
        this.powertools.remove(type);
    }

}
