/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.commands;

import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.modules.message.config.MessageConfig;
import io.github.nucleuspowered.nucleus.modules.message.parameter.MessageTargetArgument;
import io.github.nucleuspowered.nucleus.modules.message.services.MessageHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.NotifyIfAFK;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.DisplayNameArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.SelectorArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent({"msg", "tell", "m", "t", "whisper"})
@NonnullByDefault
@NotifyIfAFK(MessageCommand.TO)
@Command(
        aliases = { "message", "m", "msg", "whisper", "w", "t" },
        basePermission = MessagePermissions.BASE_MESSAGE,
        commandDescriptionKey = "message",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MessagePermissions.EXEMPT_COOLDOWN_MESSAGE),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MessagePermissions.EXEMPT_WARMUP_MESSAGE),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MessagePermissions.EXEMPT_COST_MESSAGE)
        },
        associatedPermissions = {
                MessagePermissions.MESSAGE_COLOUR,
                MessagePermissions.MESSAGE_STYLE,
                MessagePermissions.MESSAGE_URLS
        }
)
public class MessageCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {
    final static String TO = "to";
    boolean canMessageSelf = false;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.firstParsing(
                    new MessageTargetArgument(serviceCollection.getServiceUnchecked(MessageHandler.class), Text.of(TO)),
                    new SelectorArgument(new DisplayNameArgument(Text.of(TO), DisplayNameArgument.Target.PLAYER_CONSOLE, serviceCollection),
                            Player.class, serviceCollection)
            )),
            NucleusParameters.MESSAGE
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        if (context.is(context.requireOne(TO, CommandSource.class))) {
            return context.errorResult("command.message.self");
        }
        boolean b = context.getServiceCollection()
                .getServiceUnchecked(MessageHandler.class)
                .sendMessage(context.getCommandSource(),
                        context.requireOne(TO, CommandSource.class),
                        context.requireOne(NucleusParameters.Keys.MESSAGE, String.class));
        return b ? context.successResult() : context.failResult();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.canMessageSelf = serviceCollection.moduleDataProvider().getModuleConfig(MessageConfig.class).isCanMessageSelf();
    }

}
