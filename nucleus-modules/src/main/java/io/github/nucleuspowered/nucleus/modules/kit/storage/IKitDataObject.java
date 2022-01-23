/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.storage;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;

import java.util.Map;
import java.util.Optional;

public interface IKitDataObject extends IDataObject {

    Map<String, Kit> getKitMap();

    void setKitMap(Map<String, Kit> map);

    boolean hasKit(String name);

    Optional<Kit> getKit(String name);

    void setKit(Kit kit) throws Exception;

    boolean removeKit(String name) throws Exception;

}
