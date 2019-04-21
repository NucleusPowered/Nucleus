/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.text.Tokens;
import io.github.nucleuspowered.nucleus.modules.jail.commands.CheckJailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.Map;
import java.util.Optional;

@ModuleData(id = JailModule.ID, name = "Jail")
public class JailModule extends ConfigurableModule<JailConfigAdapter> {

    public static final String ID = "jail";

    @Override
    public JailConfigAdapter createAdapter() {
        return new JailConfigAdapter();
    }

    @Override
    public void performEnableTasks() {
        createSeenModule(CheckJailCommand.class, (c, u) -> {

            // If we have a ban service, then check for a ban.
            JailHandler jh = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(JailHandler.class);
            if (jh.isPlayerJailed(u)) {
                JailData jd = jh.getPlayerJailDataInternal(u).get();
                Text.Builder m;
                if (jd.getRemainingTime().isPresent()) {
                    m = getMessageFor(c.getLocale(), "seen.isjailed.temp",
                                    Util.getTimeStringFromSeconds(jd.getRemainingTime().get().getSeconds())).toBuilder();
                } else {
                    m = getMessageFor(c.getLocale(), "seen.isjailed.perm").toBuilder();
                }

                return Lists.newArrayList(
                        m.onClick(TextActions.runCommand("/nucleus:checkjail " + u.getName()))
                                .onHover(TextActions.showText(getMessageFor(c.getLocale(), "standard.clicktoseemore"))).build(),
                        getMessageFor(c.getLocale(), "standard.reason", jd.getReason()));
            }

            return Lists.newArrayList(getMessageFor(c.getLocale(), "seen.notjailed"));
        });
    }

    @Override
    protected Map<String, Tokens.Translator> tokensToRegister() {
        return ImmutableMap.<String, Tokens.Translator>builder()
                .put("jailed", new Tokens.TrueFalseVariableTranslator() {
                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    final Optional<Text> def = Optional.of(Text.of(TextColors.GRAY, "[Jailed]"));

                    @Override protected Optional<Text> getDefault() {
                        return this.def;
                    }

                    @Override protected boolean condition(CommandSource commandSource) {
                        return commandSource instanceof Player &&
                                getServiceUnchecked(JailHandler.class).isPlayerJailed((Player) commandSource);
                    }
                })
                .put("jail", (source, variableString, variables) -> {
                    if (source instanceof Player) {
                        return getServiceUnchecked(JailHandler.class).getPlayerJailData((Player) source).map(x -> Text.of(x.getJailName()));
                    }

                    return Optional.empty();
                })
                .build();
    }
}
