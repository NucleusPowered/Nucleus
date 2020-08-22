/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.modules.playerinfo.config.PlayerInfoConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerOnlineService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EssentialsEquivalent("near")
@NonnullByDefault
@Command(
        aliases = {"near"},
        basePermission = PlayerInfoPermissions.BASE_NEAR,
        commandDescriptionKey = "near",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = PlayerInfoPermissions.EXEMPT_COOLDOWN_NEAR),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = PlayerInfoPermissions.EXEMPT_WARMUP_NEAR),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = PlayerInfoPermissions.EXEMPT_COST_NEAR)
        }
)
public class NearCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {
        // SimpleReloadable {

    private static final NumberFormat formatter =  NumberFormat.getInstance();
    private final String radiusKey = "radius";
    private int maxRadius;

    static {
        formatter.setMaximumFractionDigits(2);
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                    .createOtherUserPermissionElement(false, PlayerInfoPermissions.OTHERS_NEAR),
                GenericArguments.optionalWeak(GenericArguments.integer(Text.of(this.radiusKey)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        final User user = context.getUserFromArgs();
        final Location<World> location;
        final Vector3d position;
        if (user.isOnline()) {
            location = user.getPlayer().get().getLocation();
            position = location.getPosition();
        } else {
            World world = user.getWorldUniqueId()
                    .flatMap(x -> Sponge.getServer().getWorld(x))
                    .orElseThrow((() -> context.createException("command.near.location.nolocation", user.getName())));
            position = user.getPosition();
            location = new Location<>(world, position);
        }

        int radius = this.maxRadius;
        final Optional<Integer> radiusOpt = context.getOne(this.radiusKey, Integer.class);
        if (radiusOpt.isPresent()) {
            int inputRadius = radiusOpt.get();
            // Check if executor has max radius override permission
            if (inputRadius > this.maxRadius && context.testPermission(PlayerInfoPermissions.EXEMPT_MAXRADIUS_NEAR)) {
                radius = inputRadius;
            } else {
                context.sendMessage("command.near.radiustoobig", this.maxRadius);
            }
        }

        final CommandSource src = context.getCommandSource();
        final IPlayerOnlineService playerOnlineService = context.getServiceCollection().playerOnlineService();
        final List<Text> messagesToSend =
                location.getExtent()
                        .getNearbyEntities(location.getPosition(), radius)
                        .stream()
                        .filter(Player.class::isInstance)
                        .map(Player.class::cast)
                        .filter(e -> e.getUniqueId() != user.getUniqueId() && playerOnlineService.isOnline(src, e))
                        .map(x -> Tuple.of(x, position.distance(x.getPosition())))
                        .sorted(Comparator.comparingDouble(Tuple::getSecond))
                        .map(tuple -> createPlayerLine(context, tuple))
                        .collect(Collectors.toList());

        Util.getPaginationBuilder(src)
                        .title(context.getMessage("command.near.playersnear", user.getName()))
                        .contents(messagesToSend)
                        .sendTo(src);

        return context.successResult();
    }

    private Text createPlayerLine(ICommandContext<? extends CommandSource> context, Tuple<Player, Double> player) {
        Text.Builder line = Text.builder();
        context.getMessage("command.near.playerdistancefrom", player.getFirst().getName());
        line.append(context.getMessage("command.near.playerdistancefrom", player.getFirst().getName(),
                formatter.format(Math.abs(player.getSecond()))))
                .onClick(TextActions.runCommand("/tp " + player.getFirst().getName()))
                .onHover(TextActions.showText(context.getMessage("command.near.tpto", player.getFirst().getName())));
        return line.build();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        PlayerInfoConfig configAdapter = serviceCollection
                .moduleDataProvider()
                .getModuleConfig(PlayerInfoConfig.class);
        this.maxRadius = configAdapter.getNear().getMaxRadius();
    }

}
