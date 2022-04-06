/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.AbstractKeyBasedDataObject;
import org.spongepowered.api.data.persistence.DataView;

public class GeneralDataObject extends AbstractKeyBasedDataObject<IGeneralDataObject> implements IGeneralDataObject {
    public GeneralDataObject() {
        super();
    }

    public GeneralDataObject(final DataView view) {
        super(view);
    }

}
