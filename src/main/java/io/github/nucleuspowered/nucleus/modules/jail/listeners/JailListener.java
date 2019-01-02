/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.events.NucleusSendToSpawnEvent;
import io.github.nucleuspowered.nucleus.api.events.NucleusTeleportEvent;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularUserService;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusOnLoginEvent;
import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.datamodules.JailUserDataModule;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.PermissionMessageChannel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class JailListener implements Reloadable, ListenerBase {

    private final JailHandler handler = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(JailHandler.class);
    private final String notify;
    private final String teleport;
    private final String teleportto;

    private List<String> allowedCommands;

    @Inject
    public JailListener() {
        CommandPermissionHandler cph = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(JailCommand.class);
        this.notify = cph.getPermissionWithSuffix("notify");
        this.teleport = cph.getPermissionWithSuffix("teleportjailed");
        this.teleportto = cph.getPermissionWithSuffix("teleporttojailed");
    }

    // fires after spawn login event
    @Listener
    public void onPlayerLogin(final NucleusOnLoginEvent event, @Getter("getTargetUser") User user, @Getter("getUserService") IUserDataObject qs) {
        JailUserDataModule userDataModule = qs.get(JailUserDataModule.class);
        if (!userDataModule.getJailData().isPresent()) {
            return;
        }

        JailData jd = userDataModule.getJailData().get();

        // Send them back to where they should be.
        Optional<NamedLocation> owl = this.handler.getWarpLocation(user);
        if (!owl.isPresent()) {
            new PermissionMessageChannel(this.notify)
                    .send(Text.of(TextColors.RED, "WARNING: No jail is defined. Jailed players are going free!"));
            this.handler.unjailPlayer(user);
            return;
        }

        // always send the player back to the jail location
        event.setTo(owl.get().getTransform().get());

        // Jailing the subject if we need to.
        if (userDataModule.jailOnNextLogin()) {
            // only set previous location if the player hasn't been moved to the jail before.
            if (event.getFrom().equals(owl.get().getTransform().get())) {
                jd.setPreviousLocation(event.getFrom().getLocation());
            }
            userDataModule.setJailData(jd);
            qs.set(FlyKeys.FLY_TOGGLE, false);
        }
    }

    /**
     * At the time the subject joins, check to see if the subject is jailed.
     *
     * @param event The event.
     */
    @Listener(order = Order.LATE)
    public void onPlayerJoin(final ClientConnectionEvent.Join event) {
        final Player user = event.getTargetEntity();
        Optional<ModularUserService> oqs = Nucleus.getNucleus().getUserDataManager().get(user);
        if (!oqs.isPresent()) {
            return;
        }

        JailUserDataModule qs = oqs.get().get(JailUserDataModule.class);

        // Jailing the subject if we need to.
        Optional<JailData> data = this.handler.getPlayerJailDataInternal(user);
        if (qs.jailOnNextLogin() && data.isPresent()) {
            // It exists.
            NamedLocation owl = this.handler.getWarpLocation(user).get();
            JailData jd = data.get();
            Optional<Duration> timeLeft = jd.getRemainingTime();
            Text message = timeLeft.map(duration -> Nucleus.getNucleus().getMessageProvider()
                .getTextMessageWithFormat("command.jail.jailedfor", owl.getName(),
                        Nucleus.getNucleus().getNameUtil().getNameFromUUID(jd.getJailerInternal()),
                        Util.getTimeStringFromSeconds(duration.getSeconds())))
                .orElseGet(() -> Nucleus.getNucleus().getMessageProvider()
                    .getTextMessageWithFormat("command.jail.jailedperm", owl.getName(),
                            Nucleus.getNucleus().getNameUtil().getNameFromUUID(jd.getJailerInternal()), "",
                        ""));

            user.sendMessage(message);
            user.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.reasoncoloured", jd.getReason()));
        }

        qs.setJailOnNextLogin(false);

        // Kick off a scheduled task to do jail time checks.
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Optional<JailData> omd = qs.getJailData();
            if (omd.isPresent()) {
                JailData md = omd.get();
                md.nextLoginToTimestamp();

                if (md.expired()) {
                    // free.
                    this.handler.unjailPlayer(user);
                } else {
                    // ensure jailing is current
                    this.handler.onJail(md, event.getTargetEntity());
                }
            }
        }).submit(Nucleus.getNucleus());
    }

    @Listener
    public void onRequestSent(NucleusTeleportEvent.Request event, @Root Player cause, @Getter("getTargetEntity") Player player) {
        if (this.handler.isPlayerJailed(cause)) {
            event.setCancelled(true);
            event.setCancelMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.teleportcause.isjailed"));
        } else if (this.handler.isPlayerJailed(player)) {
            event.setCancelled(true);
            event.setCancelMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.teleporttarget.isjailed", player.getName()));
        }
    }

    @Listener
    public void onAboutToTeleport(NucleusTeleportEvent.AboutToTeleport event, @Root CommandSource cause, @Getter("getTargetEntity") Player player) {
        if (this.handler.isPlayerJailed(player)) {
            if (!hasPermission(cause, this.teleport)) {
                event.setCancelled(true);
                event.setCancelMessage(
                        Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.abouttoteleporttarget.isjailed", player.getName()));
            } else if (!hasPermission(cause, this.teleportto)) {
                event.setCancelled(true);
                event.setCancelMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.abouttoteleportcause.targetisjailed",
                        player.getName()));
            }
        }
    }

    @Listener
    public void onCommand(SendCommandEvent event, @Root Player player) {
        // Only if the command is not in the control list.
        if (this.handler.checkJail(player, false) && this.allowedCommands.stream().noneMatch(x -> event.getCommand().equalsIgnoreCase(x))) {
            event.setCancelled(true);

            // This is the easiest way to send the messages.
            this.handler.checkJail(player, true);
        }
    }

    @Listener
    public void onBlockChange(ChangeBlockEvent event, @Root Player player) {
        event.setCancelled(this.handler.checkJail(player, true));
    }

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        event.setCancelled(this.handler.checkJail(player, true));
    }

    @Listener
    public void onSpawn(RespawnPlayerEvent event) {
        if (this.handler.checkJail(event.getTargetEntity(), false)) {
            event.setToTransform(event.getToTransform().setLocation(this.handler.getWarpLocation(event.getTargetEntity()).get().getLocation().get()));
        }
    }

    @Listener
    public void onSendToSpawn(NucleusSendToSpawnEvent event, @Getter("getTargetUser") User user) {
        if (this.handler.checkJail(user, false)) {
            event.setCancelled(true);
            event.setCancelReason(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("jail.isjailed"));
        }
    }

    @Override public void onReload() {
        this.allowedCommands = Nucleus.getNucleus().getInternalServiceManager()
                .getServiceUnchecked(JailConfigAdapter.class).getNodeOrDefault().getAllowedCommands();
    }
}
