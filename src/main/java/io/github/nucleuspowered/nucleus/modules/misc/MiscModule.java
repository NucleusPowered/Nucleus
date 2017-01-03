/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.misc.commands.GodCommand;
import io.github.nucleuspowered.nucleus.modules.misc.config.MiscConfigAdapter;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.Optional;

@ModuleData(id = "misc", name = "Miscellaneous")
public class MiscModule extends ConfigurableModule<MiscConfigAdapter> {

    public final static String ID = "misc";

    @Override
    public MiscConfigAdapter createAdapter() {
        return new MiscConfigAdapter();
    }

    @Override public void onEnable() {
        super.onEnable();

        createSeenModule(GodCommand.class, GodCommand.OTHER_SUFFIX, (cs, user) -> {
            Optional<UserService> userServiceOptional = plugin.getUserDataManager().get(user);
            boolean godMode = userServiceOptional.isPresent() && userServiceOptional.get().isInvulnerable();
            return Lists.newArrayList(
                plugin.getMessageProvider().getTextMessageWithFormat("seen.godmode",
                    plugin.getMessageProvider().getMessageWithFormat("standard.yesno." + Boolean.toString(godMode).toLowerCase())));
        });
    }
}
