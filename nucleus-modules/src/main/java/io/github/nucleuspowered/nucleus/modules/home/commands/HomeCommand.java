/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.api.module.home.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.modules.home.HomePermissions;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.events.UseHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.parameters.HomeArgument;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@EssentialsEquivalent(value = {"home", "homes"}, notes = "'/homes' will list homes, '/home' will teleport like Essentials did.")
@Command(
        aliases = {"home"},
        basePermission = HomePermissions.BASE_HOME,
        commandDescriptionKey = "home",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = HomePermissions.EXEMPT_COOLDOWN_HOME),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = HomePermissions.EXEMPT_WARMUP_HOME),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = HomePermissions.EXEMPT_COST_HOME)
        },
        associatedPermissions = HomePermissions.HOME_EXEMPT_SAMEDIMENSION
)
@NonnullByDefault
public class HomeCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private final String home = "home";

    private boolean isSafeTeleport = true;
    private boolean isPreventOverhang = true;
    private boolean isOnlySameDimension = false;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.optional(
                    new HomeArgument(Text.of(this.home), serviceCollection.getServiceUnchecked(HomeService.class), serviceCollection.messageProvider()))
            )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        HomeService homeService = context.getServiceCollection().getServiceUnchecked(HomeService.class);
        Player player = context.getIfPlayer();
        int max = homeService.getMaximumHomes(player) ;
        int current = homeService.getHomeCount(player);
        if (this.isPreventOverhang && max < current) {
            // If the player has too many homes, tell them
            return context.errorResult("command.home.overhang", max, current);
        }

        // Get the home.
        Optional<Home> owl = context.getOne(this.home, Home.class);
        Home wl;
        if (owl.isPresent()) {
            wl = owl.get();
        } else {
            Optional<Home> home = homeService.getHome(player, NucleusHomeService.DEFAULT_HOME_NAME);
            if (!home.isPresent()) {
                return context.errorResult("args.home.nohome", NucleusHomeService.DEFAULT_HOME_NAME);
            }
            wl = home.get();;
        }

        Sponge.getServer().loadWorld(wl.getWorldProperties()
                .orElseThrow(() -> context.createException("command.home.invalid", wl.getName())));

        Location<World> targetLocation = wl.getLocation().orElseThrow(() -> context.createException("command.home.invalid", wl.getName()));

        if (this.isOnlySameDimension) {
            if (!targetLocation.getExtent().getUniqueId().equals(player.getLocation().getExtent().getUniqueId())) {
                if (!context.testPermission(HomePermissions.HOME_EXEMPT_SAMEDIMENSION)) {
                    return context.errorResult("command.home.invalid", wl.getName());
                }
            }
        }

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(player);
            UseHomeEvent event = new UseHomeEvent(frame.getCurrentCause(), player, wl);

            if (Sponge.getEventManager().post(event)) {
                return event.getCancelMessage().map(x -> context.errorResultLiteral(Text.of(x)))
                        .orElseGet(() -> context.errorResult("nucleus.eventcancelled"));
            }
        }

        TeleportResult result = homeService.warpToHome(
                player,
                wl,
                this.isSafeTeleport
        );

        // Warp to it safely.
        if (result.isSuccessful()) {
            if (!wl.getName().equalsIgnoreCase(NucleusHomeService.DEFAULT_HOME_NAME)) {
                context.sendMessage("command.home.success", wl.getName());
            } else {
                context.sendMessage("command.home.successdefault");
            }

            return context.successResult();
        } else {
            return context.errorResult("command.home.fail", wl.getName());
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        HomeConfig hc = serviceCollection.moduleDataProvider().getModuleConfig(HomeConfig.class);
        this.isSafeTeleport = hc.isSafeTeleport();
        this.isPreventOverhang = hc.isPreventHomeCountOverhang();
        this.isOnlySameDimension = hc.isOnlySameDimension();
    }
}
