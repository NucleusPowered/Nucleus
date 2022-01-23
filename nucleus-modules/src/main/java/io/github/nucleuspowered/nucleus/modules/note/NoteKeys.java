/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note;

import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.DataKey;
import io.leangen.geantyref.TypeToken;

public final class NoteKeys {

    public static final TypeToken<NoteData> NOTE_DATA_KEY = TypeToken.get(NoteData.class);

    public final static DataKey.ListKey<NoteData, IUserDataObject> NOTE_DATA =
            DataKey.ofList(NoteKeys.NOTE_DATA_KEY, IUserDataObject.class, "notes");
}
