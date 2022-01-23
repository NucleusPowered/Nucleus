/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.core.services.impl.dataversioning.DataVersioning;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IWorldDataObject;

@ImplementedBy(DataVersioning.class)
public interface IDataVersioning {

    void setVersion(IUserDataObject userDataObject);

    void setVersion(IWorldDataObject worldDataObject);

    void setVersion(IGeneralDataObject generalDataObject);

    void migrate(IUserDataObject userDataObject);

    void migrate(IWorldDataObject userDataObject);

    void migrate(IGeneralDataObject generalDataObject);

}
