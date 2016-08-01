/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.data;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Instant;
import java.util.UUID;

@ConfigSerializable
public class NoteData {
    @Setting
    private UUID noter;

    @Setting
    private String note;

    @Setting
    private long date;

    public NoteData() { }

    public NoteData(Instant date, UUID noter, String note) {
        this.noter = noter;
        this.note = note;
        this.date = date.toEpochMilli();
    }

    public String getNote() {
        return note;
    }


    public UUID getNoter() {
        return noter;
    }

    public Instant getDate() {
        return Instant.ofEpochMilli(date);
    }
}
