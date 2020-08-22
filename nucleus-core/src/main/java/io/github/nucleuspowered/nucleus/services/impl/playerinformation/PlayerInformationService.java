/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.playerinformation;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerInformationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class PlayerInformationService implements IPlayerInformationService {

    private final List<Provider> providers = new ArrayList<>();

    @Override
    public void registerProvider(Provider provider) {
        this.providers.add(provider);
    }

    @Override public Collection<Provider> getProviders() {
        return ImmutableList.copyOf(this.providers);
    }
}
