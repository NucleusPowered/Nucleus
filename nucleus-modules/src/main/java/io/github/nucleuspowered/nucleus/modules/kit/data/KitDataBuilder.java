/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.data;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;

import java.util.Optional;

public final class KitDataBuilder extends AbstractDataBuilder<Kit> {

    public static final int CONTENT_VERSION = 1;

    private static final DataQuery STACKS = DataQuery.of("stacks");
    private static final DataQuery COMMANDS = DataQuery.of("commands");
    private static final DataQuery COST = DataQuery.of("cost");
    private static final DataQuery AUTOREDEEM = DataQuery.of("autoRedeem");
    private static final DataQuery ONETIME = DataQuery.of("oneTime");
    private static final DataQuery DISPLAYONREDEEM = DataQuery.of("displayOnRedeem");
    private static final DataQuery IGNORESPERMISSION = DataQuery.of("ignoresPermission");
    private static final DataQuery HIDDEN = DataQuery.of("hidden");
    private static final DataQuery FIRSTJOIN = DataQuery.of("firstJoin");
    private static final DataQuery COOLDOWN = DataQuery.of("cooldown");

    public KitDataBuilder() {
        super(Kit.class, KitDataBuilder.CONTENT_VERSION);
    }

    @Override
    protected Optional<Kit> buildContent(final DataView container) throws InvalidDataException {
        if (!container.contains(Queries.CONTENT_VERSION)) {
            Updater0to1.Holder.INSTANCE.update(container);
        }
        return Optional.empty();
    }

    // So that things are in the same place.
    public static DataContainer kitToContainer(final Kit kit) {
        final DataContainer dataContainer = DataContainer.createNew();
        dataContainer
                .set(Queries.CONTENT_VERSION, KitDataBuilder.CONTENT_VERSION)
                .set(KitDataBuilder.STACKS, kit.getStacks())
                .set(KitDataBuilder.COMMANDS, kit.getCommands())
                .set(KitDataBuilder.COST, kit.getCost())
                .set(KitDataBuilder.AUTOREDEEM, kit.isAutoRedeem())
                .set(KitDataBuilder.ONETIME, kit.isOneTime())
                .set(KitDataBuilder.DISPLAYONREDEEM, kit.isDisplayMessageOnRedeem())
                .set(KitDataBuilder.IGNORESPERMISSION, kit.ignoresPermission())
                .set(KitDataBuilder.HIDDEN, kit.isHiddenFromList())
                .set(KitDataBuilder.FIRSTJOIN, kit.isFirstJoinKit());
        kit.getCooldown().ifPresent(duration -> dataContainer.set(KitDataBuilder.COOLDOWN, duration.getSeconds()));
        return dataContainer;
    }

    public final static class Updater0to1 implements DataContentUpdater {

        // removed keys.
        private final static DataQuery INTERVAL = DataQuery.of("interval");

        public final static class Holder {
            public final static Updater0to1 INSTANCE = new Updater0to1();
        }

        @Override
        public int inputVersion() {
            return 0;
        }

        @Override
        public int outputVersion() {
            return 1;
        }

        @Override
        public DataView update(final DataView content) {
            final Optional<Long> cooldown = content.getLong(Updater0to1.INTERVAL);
            content.set(Queries.CONTENT_VERSION, 1);
            cooldown.ifPresent(l -> content.set(KitDataBuilder.COOLDOWN, l).remove(Updater0to1.INTERVAL));
            return content;
        }
    }

}
