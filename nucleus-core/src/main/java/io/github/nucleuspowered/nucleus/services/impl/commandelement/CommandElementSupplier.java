/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.commandelement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.NucleusRequirePermissionArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandElementSupplier;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.Optional;

@Singleton
public class CommandElementSupplier implements ICommandElementSupplier {

    private final IPermissionService permissionService;
    private final INucleusServiceCollection serviceCollection;

    @Inject
    public CommandElementSupplier(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.permissionService = serviceCollection.permissionService();
    }

    @Override
    public CommandElement createLocaleElement(Text key) {
        return new LocaleElement(key, this.serviceCollection);
    }

    @Override
    public CommandElement createOnlyOtherUserPermissionElement(String permission) {
        return GenericArguments.optional(
                new NucleusRequirePermissionArgument(
                        NucleusParameters.ONE_USER.get(this.serviceCollection),
                        this.permissionService,
                        permission,
                        false
                )
        );
    }

    @Override public CommandElement createOnlyOtherUserPermissionElement(boolean isPlayer, String permission) {
        return GenericArguments.optional(
                new NucleusRequirePermissionArgument(
                        isPlayer ? NucleusParameters.ONE_PLAYER.get(this.serviceCollection) : NucleusParameters.ONE_USER.get(this.serviceCollection),
                        this.permissionService,
                        permission,
                        false
                )
        );
    }

    @Override public CommandElement createOtherUserPermissionElement(boolean isPlayer, String permission) {
        return GenericArguments.optionalWeak(
                new NucleusRequirePermissionArgument(
                    isPlayer ? NucleusParameters.ONE_PLAYER.get(this.serviceCollection) : NucleusParameters.ONE_USER.get(this.serviceCollection),
                    this.permissionService,
                    permission,
                        false
                )
        );
    }

    @Override public NucleusRequirePermissionArgument createPermissionParameter(CommandElement wrapped, String permission, boolean isOptional) {
        return new NucleusRequirePermissionArgument(wrapped, this.permissionService, permission, isOptional);
    }

    @Override public User getUserFromParametersElseSelf(ICommandContext<? extends CommandSource> context) throws CommandException {
        Optional<User> user = context.getOne(NucleusParameters.Keys.USER, User.class).filter(context::isNot);
        if (!user.isPresent()) {
            return context.getIfPlayer();
        }

        // If not self, we set no cooldown etc.
        context.setCooldown(0);
        context.setCost(0);
        context.setWarmup(0);
        return user.map(x -> x.getPlayer().isPresent() ? x.getPlayer().get() : x).get();
    }

}
