/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.api.service.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.argumentparsers.HomeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.home.events.UseHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand("home")
@EssentialsEquivalent(value = {"home", "homes"}, notes = "'/homes' will list homes, '/home' will teleport like Essentials did.")
@NonnullByDefault
public class HomeCommand extends AbstractCommand<Player> implements Reloadable {

    private final String home = "home";

    public static final String EXEMPT_SAMEDIMENSION_SUFFIX = "exempt.samedimension";

    private final HomeService homeService = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(HomeService.class);

    private boolean isSafeTeleport = true;
    private boolean isPreventOverhang = true;
    private boolean isOnlySameDimension = false;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.optional(new HomeArgument(Text.of(this.home), Nucleus.getNucleus())))
        };
    }

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put(EXEMPT_SAMEDIMENSION_SUFFIX, PermissionInformation.getWithTranslation("permission.home.exempt.samedimension", SuggestedLevel.ADMIN));
        }};
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        int max = this.homeService.getMaximumHomes(src) ;
        int current = this.homeService.getHomeCount(src) ;
        if (this.isPreventOverhang && max < current) {
            // If the player has too many homes, tell them
            throw ReturnMessageException.fromKey("command.home.overhang", String.valueOf(max), String.valueOf(current));
        }

        // Get the home.
        Optional<Home> owl = args.getOne(this.home);
        Home wl;
        if (owl.isPresent()) {
            wl = owl.get();
        } else {
            wl = this.homeService.getHome(src, NucleusHomeService.DEFAULT_HOME_NAME)
                .orElseThrow(() -> new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.home.nohome", "home")));
        }

        Sponge.getServer().loadWorld(wl.getWorldProperties()
                .orElseThrow(() -> ReturnMessageException.fromKey("command.home.invalid", wl.getName())));

        Location<World> targetLocation = wl.getLocation().orElseThrow(() -> ReturnMessageException.fromKey("command.home.invalid", wl.getName()));

        if (this.isOnlySameDimension) {
            if (!targetLocation.getExtent().getUniqueId().equals(src.getLocation().getExtent().getUniqueId())) {
                if (!this.permissions.testSuffix(src, "exempt.samedimension", src, true)) {
                    throw ReturnMessageException.fromKey("command.home.invalid", wl.getName());
                }
            }
        }

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(src);
            UseHomeEvent event = new UseHomeEvent(frame.getCurrentCause(), src, wl);

            if (Sponge.getEventManager().post(event)) {
                throw new ReturnMessageException(event.getCancelMessage().orElseGet(() ->
                        Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("nucleus.eventcancelled")
                ));
            }
        }

        TeleportResult result =
                Nucleus.getNucleus()
                        .getInternalServiceManager()
                        .getServiceUnchecked(HomeService.class)
                        .warpToHome(
                                src,
                                wl,
                                this.isSafeTeleport
                        );

        // Warp to it safely.
        if (result.isSuccessful()) {
            if (!wl.getName().equalsIgnoreCase(NucleusHomeService.DEFAULT_HOME_NAME)) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.home.success", wl.getName()));
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.home.successdefault"));
            }

            return CommandResult.success();
        } else {
            throw ReturnMessageException.fromKey("command.home.fail", wl.getName());
        }
    }

    @Override
    public void onReload() {
        HomeConfig hc = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(HomeConfigAdapter.class).getNodeOrDefault();
        this.isSafeTeleport = hc.isSafeTeleport();
        this.isPreventOverhang = hc.isPreventHomeCountOverhang();
        this.isOnlySameDimension = hc.isOnlySameDimension();
    }
}
