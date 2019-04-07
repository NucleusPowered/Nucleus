/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import io.github.nucleuspowered.nucleus.configurate.datatypes.WarpCategoryDataNode;
import io.github.nucleuspowered.nucleus.configurate.datatypes.WarpNode;
import io.github.nucleuspowered.nucleus.internal.TypeTokens;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class WarpKeys {

    public static final DataKey.MapKey<String, WarpNode, IGeneralDataObject> WARP_NODES
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.WARP_NODE, IGeneralDataObject.class, "warps");

    public static final DataKey.MapKey<String, WarpCategoryDataNode, IGeneralDataObject> WARP_CATEGORIES
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.WARP_CATEGORY_DATA_NODE, IGeneralDataObject.class, "warpCategories");

}
