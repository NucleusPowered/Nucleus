/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommandInterceptors;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.internal.text.Tokens;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.modules.afk.commands.AFKCommand;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.afk.interceptors.AFKCommandInterceptor;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RegisterCommandInterceptors(AFKCommandInterceptor.class)
@ModuleData(id = AFKModule.ID, name = "AFK")
public class AFKModule extends ConfigurableModule<AFKConfigAdapter> implements PermissionTrait, MessageProviderTrait {

    public static final String ID = "afk";

    @Override
    public AFKConfigAdapter createAdapter() {
        return new AFKConfigAdapter();
    }

    @Override protected Map<String, Tokens.Translator> tokensToRegister() {
        return ImmutableMap.<String, Tokens.Translator>builder()
                .put("afk", new Tokens.TrueFalseVariableTranslator() {
                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    final Optional<Text> def = Optional.of(Text.of(TextColors.GRAY, "[AFK]"));

                    @Override protected Optional<Text> getDefault() {
                        return this.def;
                    }

                    @Override protected boolean condition(CommandSource source) {
                        return source instanceof Player && Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(AFKHandler.class).isAFK((Player) source);
                    }
                })
                .build();
    }

    @Override
    protected void performEnableTasks() throws Exception {
        super.performEnableTasks();

        final String perm = getPermissionHandlerFor(AFKCommand.class).getPermissionWithSuffix("notify");
        createSeenModule(perm, this::createAFKSeenData);
    }

    private List<Text> createAFKSeenData(CommandSource source, User user) {
        AFKHandler handler = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(AFKHandler.class);
        if (user.isOnline()) {
            Player player = user.getPlayer().get();
            String timeToNow = Util.getTimeToNow(handler.lastActivity(player), "standard.now");
            if (handler.canGoAFK(player)) {
                if (handler.isAFK(player)) {
                    return Lists.newArrayList(getMessageFor(source.getLocale(), "command.seen.afk",
                            getMessageFor(source.getLocale(), "standard.yesno.true"), timeToNow));
                } else {
                    return Lists.newArrayList(getMessageFor(source.getLocale(), "command.seen.afk",
                            getMessageFor(source.getLocale(), "standard.yesno.false"), timeToNow));
                }
            } else {
                return Lists.newArrayList(getMessageFor(source.getLocale(), "command.seen.afk",
                        getMessageFor(source.getLocale(), "standard.yesno.false"), timeToNow));
            }
        } else {
            return ImmutableList.of();
        }
    }
}
