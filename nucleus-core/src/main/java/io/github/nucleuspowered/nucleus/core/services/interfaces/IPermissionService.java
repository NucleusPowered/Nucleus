/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.core.services.impl.permission.NucleusPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.core.services.interfaces.data.SuggestedLevel;
import io.github.nucleuspowered.nucleus.core.util.PermissionMessageChannel;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ImplementedBy(NucleusPermissionService.class)
public interface IPermissionService {

    void assignUserRoleToDefault();

    void assignRoleToGroup(SuggestedLevel role, Subject subject);

    void registerContextCalculator(ContextCalculator calculator);

    boolean hasPermission(UUID playerUUID, String permission);

    boolean hasPermission(Subject subject, String permission);

    Tristate hasPermissionTristate(Subject subject, String permission);

    boolean hasPermissionWithConsoleOverride(Subject subject, String permission, boolean permissionIfConsoleAndOverridden);

    boolean isConsoleOverride(Subject subject);

    void registerDescriptions();

    void register(String permission, PermissionMetadata metadata, String moduleid);

    OptionalDouble getDoubleOptionFromSubject(Subject player, String... options);

    OptionalLong getPositiveLongOptionFromSubject(Subject player, String... options);

    OptionalInt getPositiveIntOptionFromSubject(Subject player, String... options);

    OptionalInt getIntOptionFromSubject(Subject player, String... options);

    Optional<String> getOptionFromSubject(Subject player, String... options);

    PermissionMessageChannel permissionMessageChannel(String permission);

    Collection<Metadata> getAllMetadata();

    Optional<Metadata> getMetadataFor(String permission);

    default OptionalInt getDeclaredLevel(final Subject subject, final String key) {
        return this.getIntOptionFromSubject(subject, key);
    }

    boolean isPermissionLevelOkay(Subject actor, Subject actee, String key, String permission, boolean isSameOkay);

    CompletableFuture<Boolean> isPermissionLevelOkay(Subject actor, UUID actee, String key, String permission, boolean isSameOkay);

    void setContext(Subject subject, Context context);

    NoExceptionAutoClosable setContextTemporarily(Subject subject, Context context);

    void removeContext(UUID subject, String key);

    void removePlayerContexts(UUID uuid);

    void register(String id, Class<?> permissions);

    interface Metadata {

        boolean isPrefix();

        SuggestedLevel getSuggestedLevel();

        String getDescription(IMessageProviderService service);

        String getPermission();

        String getModuleId();
    }

}
