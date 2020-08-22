/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import static io.github.nucleuspowered.nucleus.NucleusPluginInfo.GIT_HASH;
import static io.github.nucleuspowered.nucleus.NucleusPluginInfo.NAME;
import static io.github.nucleuspowered.nucleus.NucleusPluginInfo.VERSION;

import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.interfaces.IModuleDataProvider;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;

import javax.annotation.Nullable;

@Command(
        aliases = "nucleus",
        basePermission = CorePermissions.BASE_NUCLEUS,
        commandDescriptionKey = "nucleus",
        prefixAliasesWithN = false
)
@NonnullByDefault
public class NucleusCommand implements ICommandExecutor<CommandSource> {

    private final Text version = Text.of(TextColors.GREEN, NAME + " version " + VERSION + " (built from commit " + GIT_HASH + ")");
    @Nullable private Text modules = null;

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        if (this.modules == null) {
            Text.Builder tb = Text.builder("Modules: ").color(TextColors.GREEN);

            boolean addComma = false;
            IModuleDataProvider dataProvider = context.getServiceCollection().moduleDataProvider();
            Collection<String> enabled = dataProvider.getModules(Tristate.TRUE);
            for (String module : dataProvider.getModules(Tristate.UNDEFINED)) {
                if (addComma) {
                    tb.append(Text.of(TextColors.GREEN, ", "));
                }

                tb.append(Text.of(enabled.contains(module) ? TextColors.GREEN : TextColors.RED, module));
                addComma = true;
            }

            this.modules = tb.append(Text.of(TextColors.GREEN, ".")).build();
        }

        context.getCommandSource().sendMessage(this.version);
        context.getCommandSource().sendMessage(this.modules);
        return context.successResult();
    }
}
