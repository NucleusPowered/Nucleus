/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus.debug;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.nucleus.DebugCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

@NonnullByDefault
@Command(
        aliases = "getuuids",
        basePermission = CorePermissions.BASE_DEBUG_GETUUIDS,
        commandDescriptionKey = "nucleus.debug.getuuids",
        parentCommand = DebugCommand.class
)
public class GetUUIDSCommand implements ICommandExecutor<CommandSource> {

    private final IMessageProviderService messageProvider;

    @Inject
    public GetUUIDSCommand(INucleusServiceCollection serviceCollection) {
        this.messageProvider = serviceCollection.messageProvider();
    }

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.MANY_USER_NO_SELECTOR.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Collection<User> users = context.getAll(NucleusParameters.Keys.USER, User.class);
        if (users.isEmpty()) {
            return context.errorResult("command.nucleus.debug.uuid.none");
        }

        CommandSource source = context.getCommandSource();
        Util.getPaginationBuilder(context.is(Player.class))
            .title(this.messageProvider.getMessageFor(source, "command.nucleus.debug.uuid.title", users.iterator().next().getName()))
            .header(this.messageProvider.getMessageFor(source,"command.nucleus.debug.uuid.header"))
            .contents(
                users.stream()
                    .map(
                        x -> Text.builder(x.getUniqueId().toString())
                                .color(x.isOnline() ? TextColors.GREEN : TextColors.RED)
                                .onHover(TextActions.showText(this.messageProvider.getMessageFor(source,
                                    "command.nucleus.debug.uuid.clicktodelete"
                                )))
                    .onClick(TextActions.runCommand("/nucleus resetuser -a " + x.getUniqueId().toString()))
                    .build()
                ).collect(Collectors.toList())
            ).sendTo(source);
        return context.successResult();
    }
}
