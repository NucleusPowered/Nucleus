/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.storage;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.GeneralDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.UserDataObject;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.WorldDataObject;
import io.github.nucleuspowered.storage.dataobjects.AbstractConfigurateBackedDataObject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A Configurate translator that allows for {@link AbstractConfigurateBackedDataObject}s to be translated.
 */
public class DataObjectTranslator implements TypeSerializer<AbstractConfigurateBackedDataObject> {

    public static final DataObjectTranslator INSTANCE = new DataObjectTranslator();

    private DataObjectTranslator() {}

    @Nullable
    @Override
    public AbstractConfigurateBackedDataObject deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        AbstractConfigurateBackedDataObject ado = null;
        if (type.isSupertypeOf(UserDataObject.class)) {
            ado = new UserDataObject();
        } else if (type.isSupertypeOf(WorldDataObject.class)) {
            ado = new WorldDataObject();
        } else if (type.isSupertypeOf(GeneralDataObject.class)) {
            ado = new GeneralDataObject();
        }

        if (ado != null) {
            ado.setBackingNode(value);
        }

        return ado;
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable AbstractConfigurateBackedDataObject obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (obj != null) {
            value.setValue(obj.getBackingNode());
        }
    }
}
