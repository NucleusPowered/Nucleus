/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.datamodules;

import io.github.nucleuspowered.storage.dataobjects.modules.ITransientDataModule;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.Optional;

import javax.annotation.Nullable;

public class StaffChatTransientModule implements ITransientDataModule {

    @Nullable private MessageChannel previousMessageChannel;

    public Optional<MessageChannel> getPreviousMessageChannel() {
        return Optional.ofNullable(this.previousMessageChannel);
    }

    public void setPreviousMessageChannel(@Nullable MessageChannel previousMessageChannel) {
        this.previousMessageChannel = previousMessageChannel;
    }
}
