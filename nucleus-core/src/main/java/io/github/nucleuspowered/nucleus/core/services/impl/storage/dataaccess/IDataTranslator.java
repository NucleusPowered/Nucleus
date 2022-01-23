/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.exceptions.DataLoadException;
import io.github.nucleuspowered.storage.exceptions.DataSaveException;

public interface IDataTranslator<R extends IDataObject, O> {

    R createNew();

    R fromDataAccessObject(O object) throws DataLoadException;

    O toDataAccessObject(R object) throws DataSaveException;

}
