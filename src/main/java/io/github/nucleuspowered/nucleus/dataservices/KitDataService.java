/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.dataservices;

import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.configurate.datatypes.KitConfigDataNode;
import io.github.nucleuspowered.nucleus.configurate.datatypes.KitDataNode;
import io.github.nucleuspowered.nucleus.configurate.wrappers.NucleusItemStackSnapshot;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProvider;
import io.github.nucleuspowered.nucleus.modules.kit.misc.SingleKit;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KitDataService extends AbstractService<KitConfigDataNode> {

    public KitDataService(DataProvider<KitConfigDataNode> dataProvider) {
        super(dataProvider);
    }

    public Set<String> getKitNames(boolean showHidden) {
        return this.data.getKits().entrySet().stream().filter(x -> showHidden || !x.getValue().hidden)
                    .map(Map.Entry::getKey).collect(ImmutableSet.toImmutableSet());
    }

    public Optional<KitDataNode> getKit(String name) {
        return Util.getKeyIgnoreCase(this.data.getKits(), name).map(s -> this.data.getKits().get(s));
    }

    public List<Kit> getFirstJoinKits() {
        return this.data.getKits().entrySet().stream().filter(x -> x.getValue().firstJoin)
                .map(x -> new SingleKit(x.getKey(), x.getValue())).collect(Collectors.toList());
    }

    public List<Kit> getAutoRedeemable() {
        return this.data.getKits()
                .entrySet()
                .stream()
                .filter(x -> x.getValue().autoRedeem && x.getValue().cost <= 0)
                .map(x -> new SingleKit(x.getKey(), x.getValue()))
                .collect(Collectors.toList());
    }

    public boolean addKit(String name, Kit kit) {
        if (Util.getKeyIgnoreCase(this.data.getKits(), name).isPresent()) {
            return false;
        }

        this.data.getKits().put(name, new KitDataNode(
                kit.getStacks().stream().map(NucleusItemStackSnapshot::new).collect(Collectors.toList()),
                kit.getCooldown().map(Duration::getSeconds).orElse(0L),
                kit.getCost(),
                kit.isAutoRedeem(),
                kit.isOneTime(),
                kit.isDisplayMessageOnRedeem(),
                kit.ignoresPermission(),
                kit.isHiddenFromList(),
                kit.getCommands(),
                kit.isFirstJoinKit()
        ));
        return true;
    }

    public boolean removeKit(String name) {
        Map<String, KitDataNode> msk = this.data.getKits();
        Optional<String> key = msk.keySet().stream().filter(name::equalsIgnoreCase).findFirst();
        return key.isPresent() && this.data.getKits().remove(key.get()) != null;
    }

    @Override protected String serviceName() {
        return "Kits";
    }
}
