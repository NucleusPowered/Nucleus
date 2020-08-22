/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.settingprocessor;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.neutrino.settingprocessor.SettingProcessor;
import io.github.nucleuspowered.nucleus.modules.core.services.UUIDChangeService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.UUID;

import com.google.inject.Inject;

public class WorldMigrationSettingProcessor implements SettingProcessor {

    private final UUIDChangeService service;

    private final static TypeToken<UUID> uuidTypeToken = TypeToken.of(UUID.class);

    @Inject
    public WorldMigrationSettingProcessor(UUIDChangeService service) {
        this.service = service;
    }

    @Override
    public void process(ConfigurationNode cn) {
        try {
            UUID uuid = cn.getValue(uuidTypeToken);
            this.service.getMappedUUID(uuid)
                    .ifPresent(x -> {
                        try {
                            cn.setValue(uuidTypeToken, x);
                        } catch (ObjectMappingException e) {
                            // Don't bother
                        }
                    });
        } catch (Exception e) {
            // Swallow
        }
    }
}
