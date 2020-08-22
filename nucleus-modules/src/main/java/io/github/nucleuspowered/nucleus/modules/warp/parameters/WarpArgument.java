/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.parameters;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.api.module.warp.NucleusWarpService;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@NonnullByDefault
public class WarpArgument extends CommandElement {

    private final NucleusWarpService service;
    private final IMessageProviderService messageProviderService;
    private final IPermissionService permissionService;
    private final boolean permissionCheck;

    public WarpArgument(@Nullable Text key,
            INucleusServiceCollection serviceCollection,
            NucleusWarpService warpService,
            boolean permissionCheck) {
        super(key);
        this.permissionCheck = permissionCheck;
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.service = warpService;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String warpName = args.next();
        String warp = warpName.toLowerCase();
        if (!this.service.warpExists(warp)) {
            throw args.createError(this.messageProviderService.getMessageFor(source, "args.warps.noexist"));
        }

        if (this.permissionCheck && !checkPermission(source, warpName) && !checkPermission(source, warpName.toLowerCase())) {
            throw args.createError(this.messageProviderService.getMessageFor(source, "args.warps.noperms"));
        }

        return this.service
                .getWarp(warpName)
                .orElseThrow(() -> args.createError(this.messageProviderService.getMessageFor(source, "args.warps.notavailable")));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        try {
            String el = args.peek();
            String name = el.toLowerCase();
            List<String> elements = this.service.getWarpNames().stream()
                    .filter(s -> s.startsWith(name))
                    .limit(21).collect(Collectors.toList());
            if (elements.size() >= 21) {
                this.messageProviderService.sendMessageTo(src, "args.warps.maxselect", el);
                return ImmutableList.of(el);
            } else if (elements.isEmpty()) {
                return ImmutableList.of();
            } else if (!this.permissionCheck) { // permissioncheck and requires location were always the same
                return elements;
            }

            Predicate<String> predicate = s -> {
                return this.service.getWarp(s).get().getLocation().isPresent() && checkPermission(src, s);
            };

            return elements.stream().filter(predicate).collect(Collectors.toList());
        } catch (ArgumentParseException e) {
            return ImmutableList.of();
        }
    }

    private boolean checkPermission(CommandSource src, String name) {
        // No permissions, no entry!
        return this.permissionService.hasPermission(src, WarpPermissions.getWarpPermission(name));
    }

}
