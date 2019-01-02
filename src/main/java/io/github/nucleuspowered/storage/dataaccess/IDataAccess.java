/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataaccess;

import com.google.gson.JsonObject;
import io.github.nucleuspowered.storage.dataobjects.IDataObject;

public interface IDataAccess<R extends IDataObject> {

    R createNew();

    R fromJsonObject(JsonObject object);

    JsonObject toJsonObject(R object);

}
