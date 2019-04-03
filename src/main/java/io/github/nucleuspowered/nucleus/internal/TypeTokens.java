/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import org.spongepowered.api.item.ItemType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TypeTokens {

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(boolean.class);

    public static final TypeToken<Instant> INSTANT = TypeToken.of(Instant.class);

    public static final TypeToken<LocationNode> LOCATION_NODE = TypeToken.of(LocationNode.class);

    public static final TypeToken<Integer> INTEGER = TypeToken.of(int.class);

    public static final TypeToken<Long> LONG = TypeToken.of(long.class);

    public static final TypeToken<String> STRING = TypeToken.of(String.class);

    public static final TypeToken<Map<String, LocationNode>> LOCATIONS = new TypeToken<Map<String, LocationNode>>() {};

    public static final TypeToken<List<UUID>> UUID_LIST = new TypeToken<List<UUID>>() {};

    public static final TypeToken<Vector3d> VECTOR_3D = TypeToken.of(Vector3d.class);

    public static final TypeToken<ItemType> ITEM_TYPE = TypeToken.of(ItemType.class);

    public static final TypeToken<MuteData> MUTE_DATA = TypeToken.of(MuteData.class);

    public static final TypeToken<NoteData> NOTE_DATA = TypeToken.of(NoteData.class);
}
