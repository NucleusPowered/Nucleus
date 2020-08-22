/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules.commands;

import io.github.nucleuspowered.nucleus.modules.rules.RulesModule;
import io.github.nucleuspowered.nucleus.modules.rules.RulesPermissions;
import io.github.nucleuspowered.nucleus.modules.rules.config.RulesConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@EssentialsEquivalent("rules")
@Command(aliases = "rules", basePermission = RulesPermissions.BASE_RULES, commandDescriptionKey = "rules", async = true)
public class RulesCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private Text title = Text.EMPTY;

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        context.getServiceCollection()
                .textFileControllerCollection()
                .get(RulesModule.RULES_KEY)
                .orElseThrow(() -> context.createException("command.rules.empty"))
                .sendToPlayer(context.getCommandSource(), this.title);
        return context.successResult();
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        RulesConfig config = serviceCollection.moduleDataProvider().getModuleConfig(RulesConfig.class);
        String title = config.getRulesTitle();
        if (title.isEmpty()) {
            this.title = Text.EMPTY;
        } else {
            this.title = TextSerializers.FORMATTING_CODE.deserialize(title);
        }
    }
}
