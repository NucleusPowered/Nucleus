/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname.datamodules;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

import javax.annotation.Nullable;

public class NicknameUserDataModule implements IUserDataModule, InternalServiceManagerTrait {

    @DataKey("nickname-text")
    @Nullable
    private Text nickname = null;

    public Optional<Text> getNicknameAsText() {
        return Optional.ofNullable(this.nickname);
    }

    public Optional<String> getNicknameAsString() {
        return getNicknameAsText().map(Text::toPlain);
    }

    public void setNickname(Text nickname) {
        this.nickname = Preconditions.checkNotNull(nickname);

        getService().getPlayer().ifPresent(x -> {
            Text p = getServiceUnchecked(NicknameService.class).getNickPrefix();
            x.offer(Keys.DISPLAY_NAME, Text.join(p, nickname));
        });
    }

    public void removeNickname() {
        this.nickname = null;
        getService().getPlayer().ifPresent(x -> x.offer(Keys.DISPLAY_NAME, Text.of(x.getName())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Optional<T> getValue(TypeToken<T> token, String path, ConfigurationNode node) {
        if (TextRepresentable.class.isAssignableFrom(token.getRawType())) {
            String str = node.getNode(path).getString();
            if (str == null || str.isEmpty()) {
                return Optional.empty();
            }

            try {
                return (Optional<T>) Optional.of(TextSerializers.JSON.deserialize(str));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        return super.getValue(token, path, node);
    }

    @Override
    protected <T> void saveNode(TypeToken<T> typeToken, T value, String path, ConfigurationNode node) throws ObjectMappingException {
        if (value == null) {
            node.getNode(path).setValue(null);
            return;
        }

        if (value instanceof TextRepresentable) {
            node.getNode(path).setValue(TextSerializers.JSON.serialize(((TextRepresentable) value).toText()));
            return;
        }

        super.saveNode(typeToken, value, path, node);
    }

}
