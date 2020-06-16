/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.placeholder.standard;

import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.placeholder.PlaceholderContext;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class OptionPlaceholder implements PlaceholderParser {

    private final IPermissionService permissionService;

    public OptionPlaceholder(IPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public Text parse(PlaceholderContext placeholderContext) {
        Optional<Subject> subjectOptional = placeholderContext.getAssociatedObject().filter(x -> x instanceof Subject).map(x -> (Subject) x);
        if (subjectOptional.isPresent() && placeholderContext.getArgumentString().isPresent()) {
            return this.permissionService
                    .getOptionFromSubject(subjectOptional.get(), placeholderContext.getArgumentString().get())
                    .map(TextSerializers.FORMATTING_CODE::deserialize)
                    .orElse(Text.EMPTY);
        }
        return Text.EMPTY;
    }

    @Override
    public String getId() {
        return "nucleus:option";
    }

    @Override
    public String getName() {
        return "Nucleus Permission Option Placeholder";
    }

}
