/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.data.interfaces.EndTimestamp;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.util.Action;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.translation.Translatable;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class Util {

    private Util() {
    }

    public static final UUID consoleFakeUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static Inventory getStandardInventory(Player player) {
        return player.getInventory().query(Hotbar.class, GridInventory.class);
    }

    /**
     * Adds items to a {@link Player}s {@link Inventory}
     * @param player The {@link Player}
     * @param itemStacks The {@link ItemStackSnapshot}s to add.
     * @return {@link Tristate#TRUE} if everything is successful, {@link Tristate#FALSE} if nothing was added, {@link Tristate#UNDEFINED}
     * if some stacks were added.
     */
    public static Tristate addToStandardInventory(Player player, Collection<ItemStackSnapshot> itemStacks) {
        Tristate ts = Tristate.FALSE;
        Inventory target = Util.getStandardInventory(player);
        for (ItemStackSnapshot stack : itemStacks) {
            // Ignore anything that is NONE
            if (stack.getType() != ItemTypes.NONE) {
                // Give them the kit.
                InventoryTransactionResult itr = target.offer(stack.createStack());

                // If some items were rejected...
                if (!itr.getRejectedItems().isEmpty()) {
                    // ...tell the user and break out.
                    return ts;
                }

                ts = Tristate.UNDEFINED;
            }
        }

        return Tristate.TRUE;
    }

    public static UUID getUUID(CommandSource src) {
        if (src instanceof Identifiable) {
            return ((Identifiable) src).getUniqueId();
        }

        return consoleFakeUUID;
    }

    public static String getTimeToNow(Instant time) {
        return getTimeStringFromSeconds(Instant.now().getEpochSecond() - time.getEpochSecond());
    }

    public static String getTimeStringFromSeconds(long time) {
        time = Math.abs(time);
        long sec = time % 60;
        long min = (time / 60) % 60;
        long hour = (time / 3600) % 24;
        long day = time / 86400;

        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();
        if (time == 0) {
            return messageProvider.getMessageWithFormat("standard.inamoment");
        }

        StringBuilder sb = new StringBuilder();
        if (day > 0) {
            sb.append(day).append(" ");
            if (day > 1) {
                sb.append(messageProvider.getMessageWithFormat("standard.days"));
            } else {
                sb.append(messageProvider.getMessageWithFormat("standard.day"));
            }
        }

        if (hour > 0) {
            appendComma(sb);
            sb.append(hour).append(" ");
            if (hour > 1) {
                sb.append(messageProvider.getMessageWithFormat("standard.hours"));
            } else {
                sb.append(messageProvider.getMessageWithFormat("standard.hour"));
            }
        }

        if (min > 0) {
            appendComma(sb);
            sb.append(min).append(" ");
            if (min > 1) {
                sb.append(messageProvider.getMessageWithFormat("standard.minutes"));
            } else {
                sb.append(messageProvider.getMessageWithFormat("standard.minute"));
            }
        }

        if (sec > 0) {
            appendComma(sb);
            sb.append(sec).append(" ");
            if (sec > 1) {
                sb.append(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.seconds"));
            } else {
                sb.append(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.second"));
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return messageProvider.getMessageWithFormat("standard.unknown");
        }
    }

    public static <T extends EndTimestamp> Optional<T> testForEndTimestamp(Optional<T> omd, Action function) {
        if (omd.isPresent()) {
            T md = omd.get();
            if (md.getEndTimestamp().isPresent() && md.getEndTimestamp().get().isBefore(Instant.now())) {
                // Mute expired.
                function.action();
                return Optional.empty();
            }
        }

        return omd;
    }

    public static String getTimeFromTicks(long ticks) {
        if (ticks < 0 || ticks > 23999) {
            // Normalise
            ticks = ticks % 24000;
        }

        int mins = (int) ((ticks % 1000) / (100. / 6.));
        long hours = (ticks / 1000 + 6) % 24;

        NumberFormat m = NumberFormat.getIntegerInstance();
        m.setMinimumIntegerDigits(2);

        if (hours < 12) {
            long ahours = hours == 0 ? 12 : hours;
            return MessageFormat.format(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.time.am"), ahours, hours, m.format(mins));
        } else {
            hours -= 12;
            long ahours = hours == 0 ? 12 : hours;
            return MessageFormat.format(Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.time.pm"), ahours, hours, m.format(mins));
        }
    }

    private static void appendComma(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
    }

    public static boolean isFirstPlay(Player player) {
        Instant firstPlayed = player.getJoinData().firstPlayed().get();
        Instant lastPlayed = player.getJoinData().lastPlayed().get();

        // lastPlayed is always ticking - give three seconds.
        // TODO: Better way of doing this.
        return firstPlayed.isAfter(lastPlayed.minus(3, ChronoUnit.SECONDS));
    }

    /**
     * Utility method for getting the first available option from an {@link Subject}
     *
     * @param player The {@link User} to get the subject from.
     * @param options The option keys to check.
     * @return An {@link Optional} that might contain a value.
     */
    public static Optional<String> getOptionFromSubject(User player, String... options) {
        for (String option : options) {
            String o = option.toLowerCase();

            // Option for context.
            Optional<String> os = player.getOption(player.getActiveContexts(), o);
            if (os.isPresent()) {
                return os;
            }

            // General option
            os = player.getOption(o);
            if (os.isPresent()) {
                return os;
            }
        }

        return Optional.empty();
    }

    public static Optional<String> getTranslatedStringFromItemId(String id) {
        Optional<CatalogType> type = getCatalogTypeForItemFromId(id);
        if (type.isPresent()) {
            return Optional.of(getTranslatableIfPresentOnCatalogType(type.get()));
        }

        return Optional.empty();
    }

    /**
     * Gets the {@link ItemType} or {@link BlockState} for an ID.
     *
     * @param id The ID to check.
     * @return An {@link Optional} containing the intended {@link CatalogType}
     */
    public static Optional<CatalogType> getCatalogTypeForItemFromId(String id) {
        // Check for ItemType.
        Optional<ItemType> oit = Sponge.getRegistry().getAllOf(ItemType.class).stream().filter(x -> x.getId().equalsIgnoreCase(id)).findFirst();
        if (oit.isPresent()) {
            return Optional.of(oit.get());
        }

        // BlockState, you're up next!
        Optional<BlockState> obs = Sponge.getRegistry().getAllOf(BlockState.class).stream().filter(x -> x.getId().equalsIgnoreCase(id)).findFirst();
        if (obs.isPresent()) {
            return Optional.of(obs.get());
        }

        return Optional.empty();
    }

    public static <T extends CatalogType> String getTranslatableIfPresentOnCatalogType(T ct) {
        if (ct instanceof Translatable) {
            return getTranslatableIfPresent((Translatable & CatalogType)ct);
        }

        return ct.getName();
    }

    /**
     * As some {@link Translatable#getTranslation()} methods have not been implemented yet, this allows us to try to use
     * the method in a safer manner for {@link CatalogType}s.
     *
     * @param translatable The {@link Translatable} to get the translation from, if appropriate.
     * @param <T> The {@link CatalogType} that is also a {@link Translatable}
     * @return A {@link String} that represents the item.
     */
    public static <T extends Translatable & CatalogType> String getTranslatableIfPresent(T translatable) {
        try {
            String result = translatable.getTranslation().get();
            if (!result.isEmpty()) {
                return result;
            }
        } catch (AbstractMethodError e) {
            //
        }

        return translatable.getName();
    }

    /**
     * Gets a key from a map based on a case insensitive key.
     *
     * @param map The {@link Map} to check.
     * @param key The {@link String} key.
     * @return An {@link Optional}, which contains the key if it exists.
     */
    public static Optional<String> getKeyIgnoreCase(Map<String, ?> map, String key) {
        return map.entrySet().stream().filter(x -> x.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getKey).findFirst();
    }

    /**
     * Gets a value from a map based on a case insensitive key.
     *
     * @param map The {@link Map} to check.
     * @param key The {@link String} key.
     * @param <T> The type of values in the map.
     * @return An {@link Optional}, which contains a value if the key exists in some case form.
     */
    public static <T> Optional<T> getValueIgnoreCase(Map<String, T> map, String key) {
        return map.entrySet().stream().filter(x -> x.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue).findFirst();
    }

    /**
     * Tests to see if the supplied {@link Location} is within the world's {@link org.spongepowered.api.world.WorldBorder}
     *
     * @param location The {@link Location} to test.
     * @return <code>true</code> if the location is within the border.
     */
    public static boolean isLocationInWorldBorder(Location<World> location) {
        World world = location.getExtent();

        // Diameter, not radius - we'll want the radius later. We use long, we want the floor!
        long radius = (long)Math.floor(world.getWorldBorder().getDiameter() / 2.0);

        // We get the current position and subtract the border centre. This gives us an effective distance from the
        // centre in all three dimensions. We just care about the magnitude in the x and z directions, so we get the
        // positive amount.
        Vector3d displacement = location.getPosition().sub(world.getWorldBorder().getCenter()).abs();

        // Check that we're not too far out.
        return !(displacement.getX() > radius || displacement.getZ() > radius);
    }

    /**
     * Gets all of the player's parent {@link Subject}s
     *
     * @param pl The {@link Subject} to get the parents of
     * @return The {@link List} of {@link Subject}s, or an empty list if there nothing was found.
     */
    public static List<Subject> getParentSubjects(Subject pl) {
        try {
            return pl.getSubjectData().getAllParents().values().stream().flatMap(Collection::stream)
                    .sorted((x, y) -> y.getParents().size() - x.getParents().size())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Lists.newArrayList();
        }
    }

    public static boolean compressAndDeleteFile(Path from) throws IOException {
        // Get the file.
        if (Files.exists(from)) {
            Path to = Paths.get(from.toString() + ".gz");
            try (OutputStream os = new GZIPOutputStream(new FileOutputStream(to.toFile()))) {
                Files.copy(from, os);
                os.flush();
                Files.delete(from);
            }

            return true;
        }

        return false;
    }

    public static PaginationList.Builder getPaginationBuilder(CommandSource source) {
        PaginationList.Builder plb = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder();
        if (!(source instanceof Player)) {
            plb.linesPerPage(-1);
        }

        return plb;
    }

    public static Inventory.Builder getKitInventoryBuilder() {
        return Inventory.builder().of(InventoryArchetypes.CHEST).property(InventoryDimension.PROPERTY_NAM, new InventoryDimension(9, 4));
    }

    public static Optional<CatalogType> getTypeFromItemInHand(Player src) {
        // If player, get the item in hand, otherwise, we can't continue.
        if (src.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return Optional.of(getTypeFromItem(src.getItemInHand(HandTypes.MAIN_HAND).get()));
        } else {
            return Optional.empty();
        }
    }

    public static CatalogType getTypeFromItem(ItemStack is) {
        Optional<BlockState> blockState = is.get(Keys.ITEM_BLOCKSTATE);
        if (blockState.isPresent()) {
            return blockState.get();
        }

        return is.getItem();
    }
}
