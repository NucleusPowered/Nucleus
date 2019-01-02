/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist.datamodules;

import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IGeneralDataModule;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;

public class ServerListGeneralDataModule implements IGeneralDataModule {

    @Nullable
    @DataKey("lineone")
    private String lineOne = null;

    @Nullable
    @DataKey("linetwo")
    private String lineTwo = null;

    @Nullable
    @DataKey("expiry")
    private Instant expiry = null;

    private Text messageCache;

    public Optional<String> getLineOne() {
        return Optional.ofNullable(this.lineOne);
    }

    public void setLineOne(@Nullable String lineOne) {
        this.lineOne = lineOne;
        this.messageCache = null;
    }

    public Optional<String> getLineTwo() {
        return Optional.ofNullable(this.lineTwo);
    }

    public void setLineTwo(@Nullable String lineTwo) {
        this.lineTwo = lineTwo;
        this.messageCache = null;
    }

    public Optional<Instant> getExpiry() {
        if (this.expiry == null || this.expiry.isBefore(Instant.now())) {
            remove();
        }

        return Optional.ofNullable(this.expiry);
    }

    public void setExpiry(@Nullable Instant expiry) {
        this.expiry = expiry;
        this.messageCache = null;
    }

    public Optional<Text> getMessage() {
        if ((this.lineOne != null || this.lineTwo != null) && getExpiry().isPresent()) {
            if (this.messageCache == null) {
                this.messageCache =
                    Text.of(this.lineOne == null ? Text.EMPTY : TextSerializers.FORMATTING_CODE.deserializeUnchecked(this.lineOne),
                        Text.NEW_LINE,
                        this.lineTwo == null ? Text.EMPTY : TextSerializers.FORMATTING_CODE.deserializeUnchecked(this.lineTwo));
            }

            return Optional.of(this.messageCache);
        }

        return Optional.empty();
    }

    public void remove() {
        this.lineOne = null;
        this.lineTwo = null;
        this.expiry = null;
    }
}
