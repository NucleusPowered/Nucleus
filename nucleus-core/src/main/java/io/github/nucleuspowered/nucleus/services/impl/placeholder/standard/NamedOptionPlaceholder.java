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

public class NamedOptionPlaceholder implements PlaceholderParser {

    private final IPermissionService permissionService;
    private final String namedOption;

    public NamedOptionPlaceholder(IPermissionService permissionService, String namedOption) {
        this.permissionService = permissionService;
        this.namedOption = namedOption;
    }

    @Override
    public Text parse(PlaceholderContext placeholderContext) {
        Optional<Subject> subjectOptional = placeholderContext.getAssociatedObject().filter(x -> x instanceof Subject).map(x -> (Subject) x);
        return subjectOptional.flatMap(subject -> this.permissionService
                .getOptionFromSubject(subject, this.namedOption)
                .map(TextSerializers.FORMATTING_CODE::deserialize))
                .orElse(Text.EMPTY);
    }

    @Override
    public String getId() {
        return "nucleus:option_" + this.namedOption;
    }

    @Override
    public String getName() {
        return "Nucleus Named Permission Option Placeholder (" + this.namedOption + ")";
    }

}
