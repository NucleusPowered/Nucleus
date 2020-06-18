/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.services.impl.permission.NucleusPermissionService;
import io.github.nucleuspowered.nucleus.services.impl.permission.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.impl.permission.SuggestedLevel;
import io.github.nucleuspowered.nucleus.util.PermissionMessageChannel;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.service.ProviderRegistration;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

@ImplementedBy(NucleusPermissionService.class)
public interface IPermissionService {

    void assignUserRoleToDefault();

    void assignRoleToGroup(SuggestedLevel role, Subject subject);

    boolean isOpOnly();

    void registerContextCalculator(ContextCalculator<Subject> calculator);

    void checkServiceChange(ProviderRegistration<PermissionService> service);

    boolean hasPermission(Subject subject, String permission);

    Tristate hasPermissionTristate(Subject subject, String permission);

    boolean hasPermissionWithConsoleOverride(Subject subject, String permission, boolean permissionIfConsoleAndOverridden);

    boolean isConsoleOverride(Subject subject);

    void registerDescriptions();

    void register(String permission, PermissionMetadata metadata, String moduleid);

    CommandElement createOtherUserPermissionElement(String permission);

    OptionalDouble getDoubleOptionFromSubject(Subject player, String... options);

    OptionalLong getPositiveLongOptionFromSubject(Subject player, String... options);

    OptionalInt getPositiveIntOptionFromSubject(Subject player, String... options);

    OptionalInt getIntOptionFromSubject(Subject player, String... options);

    Optional<String> getOptionFromSubject(Subject player, String... options);

    PermissionMessageChannel permissionMessageChannel(String permission);

    List<Metadata> getAllMetadata();

    Optional<Metadata> getMetadataFor(String permission);

    default OptionalInt getDeclaredLevel(Subject subject, String key) {
        return getIntOptionFromSubject(subject, key);
    }

    boolean isPermissionLevelOkay(Subject actor, Subject actee, String key, String permission, boolean isSameOkay);

    void setContext(Subject subject, Context context);

    NoExceptionAutoClosable setContextTemporarily(Subject subject, Context context);

    void removeContext(UUID subject, String key);

    void removePlayerContexts(UUID uuid);

    interface Metadata {

        boolean isPrefix();

        SuggestedLevel getSuggestedLevel();

        String getDescription(IMessageProviderService service);

        String getPermission();

        String getModuleId();
    }

}
