/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.runnables;

import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.NameUtil;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.commands.afk.AFKCommand;
import io.github.essencepowered.essence.config.MainConfig;
import io.github.essencepowered.essence.internal.CommandPermissionHandler;
import io.github.essencepowered.essence.internal.ConfigMap;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.TaskBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.permissions.PermissionInformation;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import io.github.essencepowered.essence.internal.services.AFKHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import javax.inject.Inject;
import java.util.*;

@Modules(PluginModule.AFK)
public class AFKTask extends TaskBase {
    @Inject private Essence plugin;
    @Inject private PermissionRegistry permissionRegistry;
    private CommandPermissionHandler afkService = null;

    @Override
    protected Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(PermissionRegistry.PERMISSIONS_PREFIX + "afk.exempt.toggle", new PermissionInformation(Util.getMessageWithFormat("permission.afk.exempt.toggle"), SuggestedLevel.NONE));
        m.put(PermissionRegistry.PERMISSIONS_PREFIX + "afk.exempt.kick", new PermissionInformation(Util.getMessageWithFormat("permission.afk.exempt.kick"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public void accept(Task task) {
        // Don't run the task until we have a permission service.
        if (afkService == null) {
            Optional<CommandPermissionHandler> ops = permissionRegistry.getService(AFKCommand.class);
            if (!ops.isPresent()) {
                return;
            }

            afkService = ops.get();
        }

        // Check to see if anyone has gone across the timeout.
        AFKHandler a = plugin.getAfkHandler();
        a.purgeNotOnline();

        MainConfig c = plugin.getConfig(ConfigMap.MAIN_CONFIG).get();

        // AFK time
        if (c.getAfkTime() > 0) {
            List<UUID> afking = plugin.getAfkHandler().checkForAfk(c.getAfkTime());
            if (!afking.isEmpty()) {
                Sponge.getServer().getOnlinePlayers().stream().filter(x -> !x.hasPermission(afkService.getPermissionWithSuffix("exempt.toggle")) &&
                            afking.contains(x.getUniqueId())).map(NameUtil::getName)
                        .forEach(x -> MessageChannel.TO_ALL.send(Text.of(TextColors.GRAY, "* ", x, TextColors.GRAY, " " + Util.getMessageWithFormat("afk.toafk"))));
            }
        }

        // Kick after AFK time
        if (c.getAfkTimeToKick() > 0) {
            List<UUID> afking = plugin.getAfkHandler().checkForAfkKick(c.getAfkTimeToKick());
            if (!afking.isEmpty()) {
                Sponge.getServer().getOnlinePlayers().stream().filter(x -> !x.hasPermission(afkService.getPermissionWithSuffix("exempt.kick")) &&
                        afking.contains(x.getUniqueId()))
                        .forEach(x -> {
                            Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> x.kick(Text.of(Util.getMessageWithFormat("afk.kickreason"))));
                            MessageChannel.TO_ALL.send(Text.of(TextColors.GRAY, "* ", NameUtil.getName(x), TextColors.GRAY, " " + Util.getMessageWithFormat("afk.kickedafk")));
                        });
            }
        }
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public int secondsPerRun() {
        return 2;
    }
}
