/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.nameban.exception.NameBanException;
import io.github.nucleuspowered.nucleus.modules.nameban.NameBanPermissions;
import io.github.nucleuspowered.nucleus.modules.nameban.config.NameBanConfig;
import io.github.nucleuspowered.nucleus.modules.nameban.services.NameBanHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.RegexArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.stream.Collectors;

@NonnullByDefault
@Command(aliases = "nameban", basePermission = NameBanPermissions.BASE_NAMEBAN, commandDescriptionKey = "nameban")
public class NameBanCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private final String nameKey = "name";

    private String defaultReason = "Your name is inappropriate";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            new RegexArgument(Text.of(this.nameKey),
                    Util.usernameRegexPattern, "command.nameban.notvalid", ((commandSource, commandArgs, commandContext) -> {
                try {
                    String arg = commandArgs.peek().toLowerCase();
                    return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().toLowerCase().startsWith(arg))
                        .map(User::getName)
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    return Lists.newArrayList();
                }
            }), serviceCollection),
            NucleusParameters.OPTIONAL_REASON
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        String name = context.requireOne(this.nameKey, String.class).toLowerCase();
        String reason = context.getOne(NucleusParameters.Keys.REASON, String.class).orElse(this.defaultReason);
        NameBanHandler handler = context.getServiceCollection().getServiceUnchecked(NameBanHandler.class);

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSource());
                handler.addName(name, reason, frame.getCurrentCause());
                context.sendMessage("command.nameban.success", name);
                return context.successResult();
        } catch (NameBanException ex) {
            ex.printStackTrace();
            return context.errorResult("command.nameban.failed", name);
        }
    }


    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.defaultReason = serviceCollection.moduleDataProvider().getModuleConfig(NameBanConfig.class).getDefaultReason();
    }
}
