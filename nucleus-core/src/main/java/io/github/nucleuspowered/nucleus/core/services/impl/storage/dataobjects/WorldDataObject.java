/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.AbstractKeyBasedDataObject;
import org.spongepowered.api.data.persistence.DataView;

public class WorldDataObject extends AbstractKeyBasedDataObject<IWorldDataObject> implements IWorldDataObject {
    public WorldDataObject() {
        super();
    }

    public WorldDataObject(final DataView view) {
        super(view);
    }

}
