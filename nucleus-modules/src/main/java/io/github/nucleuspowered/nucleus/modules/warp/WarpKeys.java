/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.github.nucleuspowered.nucleus.modules.warp.data.NucleusWarpCategory;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class WarpKeys {

    public static final DataKey.StringKeyedMapKey<Warp, IGeneralDataObject> WARP_NODES
            = DataKey.ofMap(TypeToken.get(Warp.class), IGeneralDataObject.class, "warps");

    public static final DataKey.ListKey<WarpCategory, IGeneralDataObject> WARP_CATEGORIES
            = DataKey.ofList(TypeToken.get(WarpCategory.class), IGeneralDataObject.class, (dataQuery, dataContainer) -> {
                if (dataContainer.getList(dataQuery).isPresent()) {
                    return dataContainer;
                }
                // we have a map.
                final DataView internal = dataContainer.getView(dataQuery).orElse(null);
                if (internal != null) {
                    final List<DataView> replacement = dataContainer.keys(false).stream()
                            .map(x -> dataContainer.getView(x).map(view -> view.set(NucleusWarpCategory.ID, x)).orElse(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    dataContainer.set(dataQuery, replacement);
                }
                return dataContainer;
            }, "warpCategories");

}
