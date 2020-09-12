/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.core.NucleusWarmupManagerService;
import io.github.nucleuspowered.nucleus.services.impl.warmup.WarmupService;
import org.spongepowered.api.entity.living.player.Player;

import java.time.Duration;

@ImplementedBy(WarmupService.class)
public interface IWarmupService extends NucleusWarmupManagerService {

}
