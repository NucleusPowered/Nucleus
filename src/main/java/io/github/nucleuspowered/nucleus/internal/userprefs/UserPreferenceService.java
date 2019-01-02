/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.userprefs;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.service.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class UserPreferenceService implements NucleusUserPreferenceService {

    private final NucleusKeysProvider provider = new NucleusKeysProvider();

    private final Map<String, NucleusUserPreferenceService.PreferenceKey<?>> registered = new HashMap<>();
    private final Element element = new Element();

    public static final Text PREFERENCE_ARG = Text.of("preference");
    public static final Text VALUE_ARG = Text.of("value");

    public CommandElement getElement() {
        return this.element;
    }

    public void postInit() {
        // Get fields
        Arrays.stream(NucleusKeysProvider.class.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(TargetID.class))
                .forEach(field -> {
                    TargetID id = field.getAnnotation(TargetID.class);
                    NucleusUserPreferenceService.PreferenceKey<?> key = this.registered.get(id.value());
                    if (key != null) {
                        try {
                            field.setAccessible(true);
                            field.set(this.provider, key);
                        } catch (IllegalAccessException e) {
                            Nucleus.getNucleus().getLogger().error("Could not set " + id.value() + " in the User Preference Service", e);
                        }
                    }
                });
    }

    public void register(PreferenceKeyImpl<?> key) {
        if (this.registered.containsKey(key.getID())) {
            throw new IllegalArgumentException("ID already registered");
        }
        this.registered.put(key.getID(), key);
        this.element.keys.put(key.getID().toLowerCase().replaceAll("^nucleus:", ""), key);
        this.element.keys.put(key.getID().toLowerCase(), key);
    }

    public <T> void set(UUID uuid, NucleusUserPreferenceService.PreferenceKey<T> key, @Nullable T value) {
        PreferenceKeyImpl pki;
        if (Objects.requireNonNull(key) instanceof PreferenceKeyImpl) {
            pki = (PreferenceKeyImpl) key;
        } else {
            throw new IllegalArgumentException("Cannot have custom preference keys");
        }

        set(uuid, pki, value);
    }

    public <T> void set(UUID uuid, PreferenceKeyImpl<T> key, @Nullable T value) {
        Nucleus.getNucleus().getStorageManager()
                .getUserService()
                .getOrNew(uuid)
                .thenAccept(x -> x.set(key, value));
    }

    public Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> get(User user) {
        Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> ret = new HashMap<>();
        for (NucleusUserPreferenceService.PreferenceKey<?> key : this.registered.values()) {
            if (key.canAccess(user)) {
                ret.put(key, get(user.getUniqueId(), key).orElse(null));
            }
        }

        return ret;
    }

    public <T> Optional<T> get(UUID uuid, NucleusUserPreferenceService.PreferenceKey<T> key) {
        if (!this.registered.containsValue(key)) {
            throw new IllegalArgumentException("Key is not registered.");
        }

        if (!(key instanceof PreferenceKeyImpl)) {
            throw new IllegalArgumentException("Custom preference keys are not supported.");
        }

        PreferenceKeyImpl<T> prefKey = (PreferenceKeyImpl<T>) key;
        Optional<T> ot = Optional.empty();
        try {
            ot = Nucleus.getNucleus().getStorageManager()
                    .getUserService()
                    .getOnThread(uuid)
                    .map(x -> x.getOrDefault(prefKey));
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        return ot;
    }

    public <T> T getUnwrapped(UUID uuid, NucleusUserPreferenceService.PreferenceKey<T> key) {
        return get(uuid, key).orElse(null);
    }

    @Override
    public Keys keys() {
        return this.provider;
    }

    @Override
    public <T> Optional<T> getPreferenceFor(User user, NucleusUserPreferenceService.PreferenceKey<T> key) {
        return get(user.getUniqueId(), key);
    }

    @Override
    public <T> void setPreferenceFor(User user, NucleusUserPreferenceService.PreferenceKey<T> key, T value) {
        set(user.getUniqueId(), key, value);
    }

    @Override
    public void removePreferenceFor(User user, NucleusUserPreferenceService.PreferenceKey<?> key) {
        set(user.getUniqueId(), key, null);
    }

    public static class Element extends CommandElement {

        private enum Type {
            BOOLEAN(GenericArguments.bool(VALUE_ARG)),
            DOUBLE(GenericArguments.doubleNum(VALUE_ARG)),
            INTEGER(GenericArguments.integer(VALUE_ARG)),
            STRING(GenericArguments.remainingRawJoinedStrings(VALUE_ARG));

            final CommandElement element;

            Type(CommandElement element) {
                this.element = element;
            }
        }

        Element() {
            super(null);
        }

        private final Map<String, NucleusUserPreferenceService.PreferenceKey<?>> keys = new HashMap<>();

        @Nullable @Override public Text getKey() {
            return Text.of("<preference> [value]");
        }

        @Override
        public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            String next = args.next().toLowerCase();
            Type type = parseFirst(source, args, context, next);

            if (args.hasNext()) {
                type.element.parse(source, args, context);
            }
        }

        private Type parseFirst(CommandSource source, CommandArgs args, CommandContext context, String next) throws ArgumentParseException {
            NucleusUserPreferenceService.PreferenceKey<?> key = this.keys.get(next);
            if (key != null) {
                Type type = null;
                Class<?> cls = key.getValueClass();
                if (cls == boolean.class || cls == Boolean.class) {
                    type = Type.BOOLEAN;
                } else if (cls == int.class || cls == Integer.class) {
                    type = Type.INTEGER;
                } else if (cls == double.class || cls == Double.class) {
                    type = Type.DOUBLE;
                } else if (cls == String.class) {
                    type = Type.STRING;
                }

                if (type != null) {
                    checkAccess(key, getUser(source, args, context), args, source);
                    context.putArg(PREFERENCE_ARG, key);
                    return type;
                }
            }

            throw args.createError(Nucleus.getNucleus().getMessageProvider()
                    .getTextMessageWithFormat("args.userprefs.incorrect", next));
        }

        private void checkAccess(NucleusUserPreferenceService.PreferenceKey<?> key, User user, CommandArgs args, CommandSource source)
                throws ArgumentParseException {
            if (!key.canAccess(user)) {
                if (source instanceof Player && ((Player) source).getUniqueId().equals(user.getUniqueId())) {
                    throw args.createError(Nucleus.getNucleus().getMessageProvider()
                            .getTextMessageWithFormat("args.userprefs.noperm.self", key.getID()));
                }
                throw args.createError(Nucleus.getNucleus().getMessageProvider()
                        .getTextMessageWithFormat("args.userprefs.noperm.other", user.getName(), key.getID()));
            }
        }

        private User getUser(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
            Optional<User> o = context.getOne(NucleusParameters.Keys.USER);
            if (!o.isPresent()) {
                if (source instanceof User) {
                    return (User) source;
                }

                throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.user.none"));
            } else {
                return o.get();
            }
        }

        @Nullable
        @Override
        protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            try {
                final User user = getUser(src, args, context);
                CommandArgs.Snapshot snapshot = args.getSnapshot();
                final String arg1 = args.next().toLowerCase();

                if (!args.hasNext()) {
                    args.applySnapshot(snapshot);
                    // complete what we have.
                    return this.keys.entrySet().stream()
                            .filter(x -> x.getKey().startsWith(arg1))
                            .filter(x -> x.getValue().canAccess(user))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                } else {
                    return parseFirst(src, args, context, arg1).element.complete(src, args, context);
                }
            } catch (ArgumentParseException e) {
                return ImmutableList.of();
            }
        }

        @Override
        public Text getUsage(CommandSource src) {
            return getKey();
        }
    }

    Map<String, NucleusUserPreferenceService.PreferenceKey<?>> getRegistered() {
        return this.registered;
    }

}
