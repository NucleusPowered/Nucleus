/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.argumentparsers.AlternativeUsageArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.IfConditionElseArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ContinueMode;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.services.TeleportHandler;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Permissions(prefix = "teleport", mainOverride = "teleport", suggestedLevel = SuggestedLevel.MOD, supportsOthers = true)
@RegisterCommand(value = "teleport", rootAliasRegister = "tp")
@EssentialsEquivalent(value = {"tp", "tele", "tp2p", "teleport", "tpo"}, isExact = false,
        notes = "If you have permission, this will override '/tptoggle' automatically.")
@NonnullByDefault
public class TeleportCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final String playerToKey = "Player to warp to";
    private final String quietKey = "quiet";

    private boolean isDefaultQuiet = false;

    private final TeleportHandler handler = getServiceUnchecked(TeleportHandler.class);

    @Override public void onReload() {
        this.isDefaultQuiet = getServiceUnchecked(TeleportConfigAdapter.class).getNodeOrDefault().isDefaultQuiet();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("offline", PermissionInformation.getWithTranslation("permission.teleport.offline", SuggestedLevel.ADMIN));
        m.put("exempt.bordercheck", PermissionInformation.getWithTranslation("permission.tppos.border", SuggestedLevel.ADMIN));
        m.put("quiet", PermissionInformation.getWithTranslation("permission.teleport.quiet", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
       return new CommandElement[]{
                GenericArguments.flags().flag("f")
                    .setAnchorFlags(true)
                    .valueFlag(requirePermissionArg(GenericArguments.bool(Text.of(this.quietKey)), this.permissions.getPermissionWithSuffix("quiet")), "q")
                    .buildWith(GenericArguments.none()),

                    new AlternativeUsageArgument(
                        GenericArguments.seq(
                                IfConditionElseArgument.permission(this.permissions.getPermissionWithSuffix("offline"),
                                        NucleusParameters.ONE_USER_PLAYER_KEY,
                                        NucleusParameters.ONE_PLAYER),

                            new IfConditionElseArgument(
                                GenericArguments.optionalWeak(
                                        new SelectorArgument(new NicknameArgument(Text.of(this.playerToKey), NicknameArgument.Target.PLAYER), Player.class)),
                                GenericArguments.none(),
                                this::testForSecondPlayer)),

                        src -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("<player to warp to>");
                            if (this.permissions.testOthers(src)) {
                                sb.append("|<player to warp> <player to warp to>");
                            }

                            if (this.permissions.testOthers(src)) {
                                sb.append("|<offline player to warp to>");
                            }

                            return Text.of(sb.toString());
                        }
                    )
       };
    }

    private boolean testForSecondPlayer(CommandSource source, CommandContext context) {
        try {
            if (context.hasAny(NucleusParameters.Keys.PLAYER) && this.permissions.testOthers(source)) {
                return context.<User>getOne(NucleusParameters.Keys.PLAYER).map(y -> y.getPlayer().isPresent()).orElse(false);
            }
        } catch (Exception e) {
            // ignored
        }

        return false;
    }

    @Override protected ContinueMode preProcessChecks(CommandSource source, CommandContext args) {
        return TeleportHandler.canTeleportTo(source, args.<User>getOne(NucleusParameters.Keys.PLAYER).get()) ? ContinueMode.CONTINUE : ContinueMode.STOP;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        boolean beQuiet = args.<Boolean>getOne(this.quietKey).orElse(this.isDefaultQuiet);
        Optional<Player> oTo = args.getOne(this.playerToKey);
        User to;
        Player from;
        if (oTo.isPresent()) { // Two player argument.
            from = args.<User>getOne(NucleusParameters.Keys.PLAYER).map(x -> x.getPlayer().orElse(null))
                .orElseThrow(() -> ReturnMessageException.fromKey("command.playeronly"));
            to = oTo.get();
            if (to.equals(src)) {
                throw ReturnMessageException.fromKey("command.teleport.player.noself");
            }
        } else if (src instanceof Player) {
            from = (Player) src;
            to = args.<User>getOne(NucleusParameters.Keys.PLAYER).get();
        } else {
            throw ReturnMessageException.fromKey("command.playeronly");
        }

        if (to.getPlayer().isPresent()) {
            if (this.handler.getBuilder().setSource(src).setFrom(from).setTo(to.getPlayer().get())
                    .setSafe(!args.hasAny("f"))
                    .setSilentTarget(beQuiet)
                    .startTeleport()) {
                return CommandResult.success();
            }

            return CommandResult.empty();
        }

        // We have an offline player.
        this.permissions.checkSuffix(src, "offline", () -> ReturnMessageException.fromKey("command.teleport.noofflineperms"));

        // Can we get a location?
        Supplier<ReturnMessageException> r = () -> ReturnMessageException.fromKey("command.teleport.nolastknown", to.getName());
        Location<World> l = getUserOnThread(to.getUniqueId()).orElseThrow(r).get(CoreKeys.LAST_LOCATION).flatMap(LocationNode::getLocationIfExists)
                .orElseThrow(r);

        MessageProvider provider = Nucleus.getNucleus().getMessageProvider();
        if (CauseStackHelper.createFrameWithCausesWithReturn(c ->
                Nucleus.getNucleus().getTeleportHandler().teleportPlayer(from, l, NucleusTeleportHandler.StandardTeleportMode.FLYING_THEN_SAFE, c).isSuccess(), src)) {
            if (!(src instanceof Player && ((Player) src).getUniqueId().equals(from.getUniqueId()))) {
                src.sendMessage(provider.getTextMessageWithFormat("command.teleport.offline.other", from.getName(), to.getName()));
            }

            from.sendMessage(provider.getTextMessageWithFormat("command.teleport.offline.self", to.getName()));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.teleport.error");
    }

}
