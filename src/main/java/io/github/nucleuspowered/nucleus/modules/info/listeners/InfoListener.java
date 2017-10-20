/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.info.InfoModule;
import io.github.nucleuspowered.nucleus.modules.info.commands.MotdCommand;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfig;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfoListener extends ListenerBase implements Reloadable, ListenerBase.Conditional {

    private final String motdPerm = Nucleus.getNucleus().getPermissionRegistry()
            .getPermissionsForNucleusCommand(MotdCommand.class).getPermissionWithSuffix("login");

    private boolean usePagination = true;
    private Text title = Text.EMPTY;

    private int delay = 500;

    @Listener
    public void playerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        // Send message one second later on the Async thread.
        Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> {
                if (player.hasPermission(this.motdPerm)) {
                    plugin.getTextFileController(InfoModule.MOTD_KEY).ifPresent(x -> {
                        if (this.usePagination) {
                            x.sendToPlayer(player, title);
                        } else {
                            x.getTextFromNucleusTextTemplates(player).forEach(player::sendMessage);
                        }
                    });
                }
            }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> msp = Maps.newHashMap();
        msp.put(this.motdPerm, PermissionInformation.getWithTranslation("permission.motd.join", SuggestedLevel.USER));
        return msp;
    }

    @Override public void onReload() throws Exception {
        InfoConfig config = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(InfoConfigAdapter.class).getNodeOrDefault();
        this.delay = (int)(config.getMotdDelay() * 1000);
        this.usePagination = config.isMotdUsePagination();

        String title = config.getMotdTitle();
        if (title.isEmpty()) {
            this.title = Text.EMPTY;
        } else {
            this.title = TextSerializers.FORMATTING_CODE.deserialize(title);
        }

    }

    @Override public boolean shouldEnable() {
        try {
            return Nucleus.getNucleus().getModuleContainer().getConfigAdapterForModule(InfoModule.ID, InfoConfigAdapter.class)
                .getNodeOrDefault().isShowMotdOnJoin();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            if (Nucleus.getNucleus().isDebugMode()) {
                e.printStackTrace();
            }

            return false;
        }

    }
}
