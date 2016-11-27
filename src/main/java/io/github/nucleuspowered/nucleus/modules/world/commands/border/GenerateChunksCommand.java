/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands.border;

import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.argumentparsers.NucleusWorldPropertiesArgument;
import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.world.WorldHelper;
import io.github.nucleuspowered.nucleus.modules.world.commands.border.gen.EnhancedGeneration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.inject.Inject;

@Permissions(prefix = "world.border")
@RegisterCommand(value = {"gen", "genchunks", "generatechunks", "chunkgen"}, subcommandOf = BorderCommand.class)
public class GenerateChunksCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String worldKey = "world";

    @Inject
    private WorldHelper worldHelper;

    private final BiFunction<World, CommandSource, CommandResult> standardGeneration = (world, source) -> {
        // Create the task.
        this.worldHelper.startPregenningForWorld(world);
        source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.gen.using.standard"));
        source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.gen.started", world.getProperties().getWorldName()));

        return CommandResult.success();
    };

    private BiFunction<World, CommandSource, CommandResult> generator;

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("notify", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.world.border.gen.notify"), SuggestedLevel.ADMIN));
        }};
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().flag("s").buildWith(
                GenericArguments.optional(GenericArguments.onlyOne(new NucleusWorldPropertiesArgument(Text.of(worldKey), NucleusWorldPropertiesArgument.Type.ENABLED_ONLY))))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        setupGenerationClass();
        WorldProperties wp = getWorldFromUserOrArgs(src, worldKey, args);
        if (worldHelper.isPregenRunningForWorld(wp.getUniqueId())) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.gen.alreadyrunning", wp.getWorldName()));
            return CommandResult.empty();
        }

        Optional<World> w = Sponge.getServer().getWorld(wp.getUniqueId());
        if (!w.isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.gen.notloaded", wp.getWorldName()));
            return CommandResult.empty();
        }

        if (args.hasAny("s")) {
            return standardGeneration.apply(w.get(), src);
        } else {
            return this.generator.apply(w.get(), src);
        }
    }

    // Lazy load.
    private void setupGenerationClass() {
        if (generator == null) {
            Optional<MixinConfigProxy> mixinConfigProxy = plugin.getMixinConfigIfAvailable();
            if (mixinConfigProxy.isPresent() && mixinConfigProxy.get().get().config.isWorldgeneration()) {
                this.generator = new EnhancedGeneration(worldHelper, permissions.getPermissionWithSuffix("notify"));
            } else {
                this.generator = standardGeneration;
            }
        }
    }
}
