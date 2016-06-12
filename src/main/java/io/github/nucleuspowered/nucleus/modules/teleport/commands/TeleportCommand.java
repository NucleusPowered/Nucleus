/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.NoCostArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NoWarmupArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.ConfigCommandAlias;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.CommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.handlers.TeleportHandler;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(root = "teleport", alias = "teleport", suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand({"teleport", "tp"})
@ConfigCommandAlias("teleport")
public class TeleportCommand extends CommandBase<CommandSource> {

    private final String playerFromKey = "playerFrom";
    private final String playerKey = "player";
    private final String quietKey = "quiet";

    @Inject private TeleportHandler handler;
    @Inject private TeleportConfigAdapter tca;

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation("permission.teleport.others", SuggestedLevel.ADMIN));
        m.put("quiet", new PermissionInformation("permission.teleport.quiet", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
       return new CommandElement[] {
                GenericArguments.flags().flag("f")
                        .valueFlag(GenericArguments.requiringPermission(GenericArguments.bool(Text.of(quietKey)), permissions.getPermissionWithSuffix("quiet")), "q")
                        .buildWith(GenericArguments.none()),

                // Either we get two arguments, or we get one.
                GenericArguments.firstParsing(
                        // <player> <player>
                        GenericArguments.requiringPermission(
                                new NoCostArgument(new NoWarmupArgument(
                                    GenericArguments.seq(
                                            GenericArguments.player(Text.of(playerFromKey)),
                                            GenericArguments.player(Text.of(playerKey))
                                    ))), permissions.getPermissionWithSuffix("others")),
                // <player>
                GenericArguments.onlyOne(GenericArguments.player(Text.of(playerKey)))
            )
       };
    }

    @Override
    public ContinueMode preProcessChecks(CommandSource source, CommandContext args) throws Exception {
        // Do the /tptoggle check now, no need to go through a warmup then...
        if (source instanceof Player && !TeleportHandler.canBypassTpToggle(source)) {
            Player to = args.<Player>getOne(playerKey).get();
            if (!plugin.getUserLoader().getUser(to).isTeleportToggled()) {
                source.sendMessage(Util.getTextMessageWithFormat("teleport.fail.targettoggle", to.getName()));
                return ContinueMode.STOP;
            }
        }

        return ContinueMode.CONTINUE;
    }

    @Override
    public String[] getAliases() {
        if (aliases == null) {
            // Some people want /tp to be held by minecraft. This will allow us
            // to do so.
            if (plugin.getCommandsConfig().getCommandNode("teleport").getNode("use-tp-command").getBoolean(true)) {
                aliases = new String[] {"tp", "teleport"};
            } else {
                aliases = new String[] {"teleport"};
            }
        }

        return super.getAliases();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        boolean beQuiet = args.<Boolean>getOne(quietKey).orElse(tca.getNodeOrDefault().isDefaultQuiet());
        Optional<Player> ofrom = args.getOne(playerFromKey);
        Player from;
        if (ofrom.isPresent()) {
            from = ofrom.get();
            if (from.equals(src)) {
                src.sendMessage(Util.getTextMessageWithFormat("command.teleport.player.noself"));
                return CommandResult.empty();
            }
        } else if (src instanceof Player) {
            from = (Player) src;
        } else {
            src.sendMessage(Util.getTextMessageWithFormat("command.playeronly"));
            return CommandResult.empty();
        }

        Player pl = args.<Player>getOne(playerKey).get();
        if (handler.getBuilder().setSource(src).setFrom(from).setTo(pl).setSafe(!args.<Boolean>getOne("f").orElse(false))
                .setSilentTarget(beQuiet).startTeleport()) {
            return CommandResult.success();
        }

        return CommandResult.empty();
    }

    @Override
    public CommentedConfigurationNode getDefaults() {
        CommentedConfigurationNode ccn = super.getDefaults();
        ccn.getNode("use-tp-command").setComment(Util.getMessageWithFormat("config.command.teleport.tp")).setValue(true);
        return ccn;
    }
}
