/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.modules;

import java.util.UUID;

public interface IDataModule {

    // TODO: remove this
    abstract class Identifiable {

        private UUID parentIdentifier;

        public void init(UUID id) {
            if (this.parentIdentifier != null) {
                this.parentIdentifier = id;
            }
        }

        public UUID getParentIdentifier() {
            return this.parentIdentifier;
        }
    }
}
