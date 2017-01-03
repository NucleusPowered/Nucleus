/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.ban.commands.CheckBanCommand;
import io.github.nucleuspowered.nucleus.modules.ban.config.BanConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.ban.Ban;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.Optional;

@ModuleData(id = "ban", name = "Bans")
public class BanModule extends ConfigurableModule<BanConfigAdapter> {

    @Override
    public BanConfigAdapter createAdapter() {
        return new BanConfigAdapter();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Take base permission from /checkban.
        createSeenModule(CheckBanCommand.class, (c, u) -> {

                // If we have a ban service, then check for a ban.
                Optional<BanService> obs = Sponge.getServiceManager().provide(BanService.class);
                if (obs.isPresent()) {
                    Optional<Ban.Profile> bs = obs.get().getBanFor(u.getProfile());
                    if (bs.isPresent()) {

                        // Lightweight checkban.
                        Text.Builder m;
                        if (bs.get().getExpirationDate().isPresent()) {
                            m = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.isbanned.temp", Util.getTimeToNow(bs.get().getExpirationDate().get())).toBuilder();
                        } else {
                            m = NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.isbanned.perm").toBuilder();
                        }

                        return Lists.newArrayList(
                            m.onClick(TextActions.runCommand("/checkban " + u.getName()))
                              .onHover(TextActions.showText(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.clicktoseemore"))).build(),
                                NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.reason",
                                        TextSerializers.FORMATTING_CODE.serialize(
                                                bs.get().getReason().orElse(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.unknown")))));
                    }
                }

                return Lists.newArrayList(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("seen.notbanned"));
          });
    }
}
