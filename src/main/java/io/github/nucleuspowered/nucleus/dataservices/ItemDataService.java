/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.dataservices;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProvider;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.Tuple;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemDataService extends Service<Map<String, ItemDataNode>> {

    private Map<String, String> aliasToItemIdCache = null;

    public ItemDataService(DataProvider<Map<String, ItemDataNode>> dataProvider) throws Exception {
        super(dataProvider, false);
    }

    @Override
    public boolean load() {
        aliasToItemIdCache = null;
        return super.load();
    }

    public Set<String> getAliases() {
        return getCache().keySet();
    }

    public ItemDataNode getDataForItem(ItemStackSnapshot itemStackSnapshot) {
        return getDataForItem(getIdFromSnapshot(itemStackSnapshot));
    }

    public ItemDataNode getDataForItem(String id) {
        Preconditions.checkNotNull(id);
        return data.getOrDefault(id.toLowerCase(), new ItemDataNode());
    }

    public void setDataForItem(ItemStackSnapshot itemStackSnapshot, ItemDataNode node) {
        setDataForItem(getIdFromSnapshot(itemStackSnapshot), node);
    }

    public void setDataForItem(String id, ItemDataNode node) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(node);
        data.put(id, node);
        aliasToItemIdCache = null;
        save();
    }

    public void resetDataForItem(ItemStackSnapshot itemStackSnapshot) {
        resetDataForItem(getIdFromSnapshot(itemStackSnapshot));
    }

    public void resetDataForItem(String id) {
        Preconditions.checkNotNull(id);
        data.remove(id);
        aliasToItemIdCache = null;
        save();
    }

    public Optional<String> getIdFromAlias(String alias) {
        return Optional.ofNullable(getCache().get(alias));
    }

    private Map<String, String> getCache() {
        if (aliasToItemIdCache == null) {
            aliasToItemIdCache = data.entrySet().stream()
                    .flatMap(k -> k.getValue().getAliases().stream().map(i -> Tuple.of(i, k.getKey())))
                    .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
        }

        return aliasToItemIdCache;
    }

    private String getIdFromSnapshot(ItemStackSnapshot stackSnapshot) {
        Optional<BlockState> blockState = stackSnapshot.get(Keys.ITEM_BLOCKSTATE);
        if (blockState.isPresent()) {
            return blockState.get().getId().toLowerCase();
        }

        return stackSnapshot.getType().getId();
    }
}
