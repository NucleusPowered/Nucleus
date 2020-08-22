/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import io.github.nucleuspowered.storage.dataobjects.keyed.DataKey;

public class WarpKeys {

    public static final DataKey.MapKey<String, Warp, IGeneralDataObject> WARP_NODES
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.WARP, IGeneralDataObject.class, "warps");

    public static final DataKey.MapKey<String, WarpCategory, IGeneralDataObject> WARP_CATEGORIES
            = DataKey.ofMap(TypeTokens.STRING, TypeTokens.WARP_CATEGORY, IGeneralDataObject.class, "warpCategories");

}
