/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.data;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.UUID;

@ConfigSerializable
public class NoteData {
    @Setting
    private UUID noter;

    @Setting
    private String note;

    public NoteData() { }

    public NoteData(UUID noter, String note) {
        this.noter = noter;
        this.note = note;
    }

    public String getNote() {
        return note;
    }


    public UUID getNoter() {
        return noter;
    }

}
