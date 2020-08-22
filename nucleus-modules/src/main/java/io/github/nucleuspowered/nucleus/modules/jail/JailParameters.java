/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import io.github.nucleuspowered.nucleus.modules.jail.parameter.JailArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

public final class JailParameters {

    private JailParameters() {}

    public static final String JAIL_KEY = "jail";

    public static final NucleusParameters.LazyLoadedCommandElement JAIL = new NucleusParameters.LazyLoadedCommandElement() {
        @Override protected CommandElement create(INucleusServiceCollection serviceCollection) {
            return new JailArgument(Text.of(JAIL_KEY), serviceCollection);
        }
    };

    public static final NucleusParameters.LazyLoadedCommandElement OPTIONAL_JAIL = new NucleusParameters.LazyLoadedCommandElement() {

        @Override protected CommandElement create(INucleusServiceCollection serviceCollection) {
            return GenericArguments.optional(JAIL.get(serviceCollection));
        }
    };

}
