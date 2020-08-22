/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.RegexArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.UUIDArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@NonnullByDefault
@Command(
        aliases = "getuser",
        basePermission = CorePermissions.BASE_NUCLEUS_GETUSER,
        commandDescriptionKey = "nucleus.getuser",
        parentCommand = NucleusCommand.class
)
public class GetUserCommand implements ICommandExecutor<CommandSource> {

    private final String uuidKey = "UUID";
    private final String playerKey = "name";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.firstParsing(
                new UUIDArgument<>(Text.of(this.uuidKey), Optional::ofNullable, serviceCollection),
                new RegexArgument(Text.of(this.playerKey), "^[A-Za-z0-9_]{3,16}$", "command.nucleus.getuser.regex", serviceCollection)
            )
        };
    }

    @Override public ICommandResult execute(final ICommandContext<? extends CommandSource> context) {
        CompletableFuture<GameProfile> profile;
        final String toGet;
        final GameProfileManager manager = Sponge.getServer().getGameProfileManager();
        if (context.hasAny(this.uuidKey)) {
            UUID u = context.requireOne(this.uuidKey, UUID.class);
            toGet = u.toString();
            profile = manager.get(u, false);
        } else {
            toGet = context.requireOne(this.playerKey, String.class);
            profile = manager.get(toGet, false);
        }

        context.sendMessage("command.nucleus.getuser.starting", toGet);

        profile.handle((gp, th) -> {
            if (th != null || gp == null) {
                if (th != null) {
                    th.printStackTrace();
                }

                context.sendMessage("command.nucleus.getuser.failed", toGet);
                return 0; // I have to return something, even though I don't care about it.
            }

            // We have a game profile, it's been added to the cache. Create the user too, just in case.
            Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getOrCreate(gp);
            context.sendMessage("command.nucleus.getuser.success", gp.getUniqueId().toString(), gp.getName().orElse("unknown"));

            return 0;
        });


        return context.successResult();
    }
}
