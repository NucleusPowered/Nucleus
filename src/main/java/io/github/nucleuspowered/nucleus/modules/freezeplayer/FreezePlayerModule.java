/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.qsml.module.StandardModule;
import io.github.nucleuspowered.nucleus.internal.text.Tokens;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.commands.FreezePlayerCommand;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.service.FreezeService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.Map;
import java.util.Optional;

@ModuleData(id = "freeze-subject", name = "Freeze Player")
public class FreezePlayerModule extends StandardModule implements InternalServiceManagerTrait {

    @Override public void performEnableTasks() {
        createSeenModule(FreezePlayerCommand.class, (c, u) -> {
            if (getServiceUnchecked(FreezeService.class).getFromUUID(u.getUniqueId())) {
                return Lists.newArrayList(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.frozen"));
            }

            return Lists.newArrayList(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.notfrozen"));
        });
    }

    @Override protected Map<String, Tokens.Translator> tokensToRegister() {
        return ImmutableMap.<String, Tokens.Translator>builder()
                .put("frozen", new Tokens.TrueFalseVariableTranslator() {
                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    final Optional<Text> def = Optional.of(Text.of(TextColors.GRAY, "[Frozen]"));

                    @Override protected Optional<Text> getDefault() {
                        return this.def;
                    }

                    @Override protected boolean condition(CommandSource commandSource) {
                        return commandSource instanceof Player &&
                                getServiceUnchecked(FreezeService.class).getFromUUID(((Player) commandSource).getUniqueId());
                    }
                })
                .build();
    }
}
