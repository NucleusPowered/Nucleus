/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.api.nucleusdata.MailMessage;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.warn.data.WarnData;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TypeTokens {

    public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(Boolean.class);

    public static final TypeToken<Instant> INSTANT = TypeToken.of(Instant.class);

    public static final TypeToken<LocationNode> LOCATION_NODE = TypeToken.of(LocationNode.class);

    public static final TypeToken<Integer> INTEGER = TypeToken.of(Integer.class);

    public static final TypeToken<Long> LONG = TypeToken.of(Long.class);

    public static final TypeToken<String> STRING = TypeToken.of(String.class);

    public static final TypeToken<Map<String, LocationNode>> LOCATIONS = new TypeToken<Map<String, LocationNode>>() {};

    public static final TypeToken<List<UUID>> UUID_LIST = new TypeToken<List<UUID>>() {};

    public static final TypeToken<Vector3d> VECTOR_3D = TypeToken.of(Vector3d.class);

    public static final TypeToken<MuteData> MUTE_DATA = TypeToken.of(MuteData.class);

    public static final TypeToken<NoteData> NOTE_DATA = TypeToken.of(NoteData.class);

    public static final TypeToken<WarnData> WARN_DATA = TypeToken.of(WarnData.class);

    public static final TypeToken<JailData> JAIL_DATA = TypeToken.of(JailData.class);

    public static final TypeToken<Warp> WARP = TypeToken.of(Warp.class);

    public static final TypeToken<WarpCategory> WARP_CATEGORY = TypeToken.of(WarpCategory.class);

    public static final TypeToken<UUID> UUID = TypeToken.of(UUID.class);

    public static final TypeToken<NamedLocation> NAMEDLOCATION = TypeToken.of(NamedLocation.class);

    public static final TypeToken<MailMessage> MAIL_MESSAGE = TypeToken.of(MailMessage.class);
}
