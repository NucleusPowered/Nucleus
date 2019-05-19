/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataaccess;

import io.github.nucleuspowered.storage.dataobjects.IDataObject;

public interface IDataTranslator<R extends IDataObject, O> {

    R createNew();

    R fromDataAccessObject(O object);

    O toDataAccessObject(R object);

}
