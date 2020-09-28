package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.module.ModuleContainer;
import io.github.nucleuspowered.nucleus.services.impl.modulereporter.ModuleReporter;

import java.util.Collection;

@ImplementedBy(ModuleReporter.class)
public interface IModuleReporter {

    Collection<String> discoveredModules();

    Collection<ModuleContainer> enabledModules();

    void provideDiscoveredModules(Collection<String> discoveredModules);

    void provideEnabledModule(ModuleContainer moduleContainer);

    boolean isLoaded(String module);
}