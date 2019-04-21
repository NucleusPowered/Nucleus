/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.back.listeners.BackListeners;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.Optional;

@Permissions
@RegisterCommand({"back", "return"})
@EssentialsEquivalent({"back", "return"})
@NonnullByDefault
public class BackCommand extends AbstractCommand<Player> implements Reloadable {

    private final BackHandler handler = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(BackHandler.class);
    private boolean sameDimensionCheck = false;
    private final String EXEMPT_PERMISSION = this.permissions.getPermissionWithSuffix(BackListeners.SAME_DIMENSION);

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags()
                    .permissionFlag(this.permissions.getPermissionWithSuffix("exempt.bordercheck"),"b", "-border")
                    .flag("f", "-force")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = Maps.newHashMap();
        m.put(BackListeners.ON_DEATH, PermissionInformation.getWithTranslation("permission.back.ondeath", SuggestedLevel.USER));
        m.put(BackListeners.ON_TELEPORT, PermissionInformation.getWithTranslation("permission.back.onteleport", SuggestedLevel.USER));
        m.put(BackListeners.ON_PORTAL, PermissionInformation.getWithTranslation("permission.back.onportal", SuggestedLevel.USER));
        m.put(BackListeners.SAME_DIMENSION, PermissionInformation.getWithTranslation("permission.back.exempt.samedimension", SuggestedLevel.MOD));
        m.put("exempt.bordercheck", PermissionInformation.getWithTranslation("permission.tppos.border", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        Optional<Transform<World>> ol = this.handler.getLastLocation(src);
        if (!ol.isPresent()) {
            throw ReturnMessageException.fromKey(src, "command.back.noloc");
        }

        boolean border = args.hasAny("b");
        Transform<World> loc = ol.get();
        if (this.sameDimensionCheck && src.getWorld().getUniqueId() != loc.getExtent().getUniqueId()) {
            if (!hasPermission(src, EXEMPT_PERMISSION)) {
                throw ReturnMessageException.fromKey(src, "command.back.sameworld");
            }
        }

        SafeTeleportService service = getServiceUnchecked(SafeTeleportService.class);

        try (AutoCloseable ac = service.temporarilyDisableBorder(border, loc.getExtent())) {

            TeleportResult result = getServiceUnchecked(SafeTeleportService.class)
                    .teleportPlayerSmart(
                            src,
                            loc,
                            false,
                            !args.hasAny("f"),
                            TeleportScanners.NO_SCAN
                    );
            if (result.isSuccessful()) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.back.success"));
                return CommandResult.success();
            } else if (result == TeleportResults.FAIL_NO_LOCATION) {
                throw ReturnMessageException.fromKey("command.back.nosafe");
            }

            throw ReturnMessageException.fromKey("command.back.cancelled");
        }
    }

    @Override
    public void onReload() throws Exception {
        this.sameDimensionCheck = getServiceUnchecked(BackConfigAdapter.class).getNodeOrDefault().isOnlySameDimension();
    }
}
