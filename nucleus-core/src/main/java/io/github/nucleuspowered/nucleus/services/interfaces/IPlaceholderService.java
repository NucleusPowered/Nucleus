/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.placeholder.NucleusPlaceholderService;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.PlaceholderMetadata;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.PlaceholderService;
import org.spongepowered.api.text.placeholder.PlaceholderParser;

import java.util.Map;
import java.util.Set;

@ImplementedBy(PlaceholderService.class)
public interface IPlaceholderService extends NucleusPlaceholderService {

    Set<PlaceholderParser> getParsers();

    void registerToken(String tokenName, PlaceholderParser parser);

    void registerToken(String tokenName, PlaceholderParser parser, boolean document);

    Map<String, PlaceholderMetadata> getNucleusParsers();
}
