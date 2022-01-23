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
import io.leangen.geantyref.TypeToken;

public final class WarpKeys {

    public static final DataKey.MapKey<String, Warp, IGeneralDataObject> WARP_NODES
            = DataKey.ofMap(TypeTokens.STRING, TypeToken.get(Warp.class), IGeneralDataObject.class, "warps");

    public static final DataKey.MapKey<String, WarpCategory, IGeneralDataObject> WARP_CATEGORIES
            = DataKey.ofMap(TypeTokens.STRING, TypeToken.get(WarpCategory.class), IGeneralDataObject.class, "warpCategories");

}
