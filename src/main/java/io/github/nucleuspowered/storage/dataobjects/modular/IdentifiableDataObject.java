/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.modular;

import io.github.nucleuspowered.storage.dataobjects.modules.IDataModule;

import java.util.UUID;

public abstract class IdentifiableDataObject<DM extends IDataModule>
        extends ModularDataObject<DM> {

    private UUID uuid = null;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        if (this.uuid == null) {
            this.uuid = uuid;
        }
    }
}
