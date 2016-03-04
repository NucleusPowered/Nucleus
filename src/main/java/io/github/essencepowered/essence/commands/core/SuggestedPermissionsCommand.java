/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.core;

import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.annotations.RunAsync;
import io.github.essencepowered.essence.internal.permissions.PermissionInformation;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RunAsync
@Permissions(root = "essence")
@RegisterCommand(value = "printperms", subcommandOf = EssenceCommand.class)
public class SuggestedPermissionsCommand extends CommandBase<CommandSource> {
    private final String file = "essence-perms.txt";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Map<String, PermissionInformation> l = plugin.getPermissionRegistry().getPermissions();
        List<String> notsuggested = l.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.NONE).map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> admin = l.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.ADMIN).map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> mod = l.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.MOD).map(Map.Entry::getKey).collect(Collectors.toList());
        List<String> user = l.entrySet().stream().filter(x -> x.getValue().level == SuggestedLevel.USER).map(Map.Entry::getKey).collect(Collectors.toList());

        BufferedWriter f = new BufferedWriter(new FileWriter(file));
        Consumer<String> permWriter = x -> {
            try {
                f.write(x);
                f.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        f.write("Not Suggested");
        f.write("-----");
        f.newLine();
        notsuggested.stream().sorted().forEach(permWriter);
        f.newLine();

        f.write("Admin");
        f.write("-----");
        f.newLine();

        admin.stream().sorted().forEach(permWriter);
        f.newLine();
        f.write("Mod");
        f.write("-----");
        f.newLine();

        mod.stream().sorted().forEach(permWriter);
        f.newLine();
        f.write("User");
        f.write("-----");
        f.newLine();

        user.stream().sorted().forEach(permWriter);
        f.flush();
        f.close();

        src.sendMessage(Text.of(Util.getMessageWithFormat("command.printperms", file)));
        return CommandResult.success();
    }
}
