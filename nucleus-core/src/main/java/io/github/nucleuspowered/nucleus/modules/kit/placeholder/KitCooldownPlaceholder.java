/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.placeholder;

import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.placeholder.PlaceholderContext;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import org.spongepowered.api.util.Tuple;

import java.util.Locale;

public class KitCooldownPlaceholder implements PlaceholderParser {

    private final KitService kitService;
    private final IStorageManager storageManager;
    private final IMessageProviderService messageProviderService;

    public KitCooldownPlaceholder(final IStorageManager storageManager, final KitService kitService, final IMessageProviderService messageProviderService) {
        this.storageManager = storageManager;
        this.kitService = kitService;
        this.messageProviderService = messageProviderService;
    }

    @Override
    public Text parse(final PlaceholderContext placeholderContext) {
        return placeholderContext.getAssociatedObject()
                .filter(x -> x instanceof User)
                .map(x -> new Tuple<>((User) x, this.storageManager.getOrCreateUserOnThread(((User) x).getUniqueId())))
                .flatMap(userObject ->
                        placeholderContext.getArgumentString()
                            .flatMap(this.kitService::getKit)
                            .map(kit -> {
                                final User user = userObject.getFirst();
                                final Locale locale;
                                if (user instanceof Player) {
                                    locale = this.messageProviderService.getLocaleFor((Player) user);
                                } else {
                                    locale = this.messageProviderService.getDefaultLocale();
                                }
                                if (this.kitService.checkOneTime(kit, userObject.getFirst())) {
                                    return userObject.getSecond()
                                            .get(KitKeys.REDEEMED_KITS, kit.getName().toLowerCase(Locale.ROOT))
                                            .<Text>map(time -> Text.of(this.messageProviderService.getTimeToNow(locale, time)))
                                            .orElseGet(() -> this.messageProviderService.getMessageFor(locale, "standard.now"));
                                } else {
                                    // Never
                                    return this.messageProviderService.getMessageFor(locale, "standard.never");
                                }
                            }))
                .orElse(Text.of());
    }

    @Override
    public String getId() {
        return "nucleus:kitcooldown";
    }

    @Override
    public String getName() {
        return "Kit Cooldown";
    }

}
