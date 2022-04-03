/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.repository;

import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.template.ArgumentElement;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.template.Template;
import io.github.nucleuspowered.nucleus.core.services.impl.messageprovider.template.TextElement;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract class AbstractMessageRepository implements IMessageRepository {

    private final static Pattern STRING_REPLACER = Pattern.compile("\\{+[^0-9]+}+");
    private final static Pattern STRING_LOCALISER = Pattern.compile("loc:([a-z\\-.]+)");

    final Map<String, String> cachedStringMessages = new HashMap<>();
    final Map<String, Template> cachedMessages = new HashMap<>();
    private final IPlayerDisplayNameService playerDisplayNameService;
    private final ITextStyleService textStyleService;

    AbstractMessageRepository(
            final ITextStyleService textStyleService,
            final IPlayerDisplayNameService playerDisplayNameService) {
        this.textStyleService = textStyleService;
        this.playerDisplayNameService = playerDisplayNameService;
    }

    abstract String getEntry(String key);

    private String getStringEntry(final String key) {
        return STRING_REPLACER.matcher(
                this.getEntry(key).replaceAll("'", "''")
        ).replaceAll("'$0'");
    }

    private Template getTextTemplate(final String key) {
        return this.cachedMessages.computeIfAbsent(key, k -> this.templateCreator(this.getEntry(k)));
    }

    @Override
    public Component getText(final String key) {
        return this.getTextTemplate(key).create();
    }

    @Override
    public Component getText(final String key, final Object[] args) {
        return this.getTextMessageWithTextFormat(key,
                Arrays.stream(args).map(x -> {
                    final Component component;
                    if (x instanceof User) {
                        component = this.playerDisplayNameService.getDisplayName(((User) x).uniqueId());
                    } else if (x instanceof ServerPlayer) {
                        component = this.playerDisplayNameService.getDisplayName(((ServerPlayer) x).uniqueId());
                    } else if (x instanceof Component) {
                        component = (Component) x;
                    } else if (x instanceof String) {
                        final String s = (String) x;
                        final Matcher matcher = STRING_LOCALISER.matcher(s);
                        if (matcher.matches()) {
                             return this.getText(matcher.group(1));
                        }

                        component = Component.text(s);
                    } else {
                        component = Component.text(x.toString());
                    }
                    return component;
                }).collect(Collectors.toList()));
    }

    @Override
    public String getString(final String key) {
        return this.cachedStringMessages.computeIfAbsent(key, this::getStringEntry);
    }

    @Override
    public String getString(final String key, final Object[] args) {
        return MessageFormat.format(this.getString(key), args);
    }

    private Component getTextMessageWithTextFormat(final String key, final List<? extends Component> textList) {
        final Template template = this.getTextTemplate(key);
        if (textList.isEmpty()) {
            return template.create();
        }

        final Map<String, Component> objs = new HashMap<>();
        for (int i = 0; i < textList.size(); i++) {
            objs.put(String.valueOf(i), textList.get(i));
        }

        return template.create(objs);
    }

    final Template templateCreator(final String string) {
        // regex!
        final Matcher mat = Pattern.compile("\\{([\\d]+)}").matcher(string);
        final List<Integer> map = new ArrayList<>();

        while (mat.find()) {
            map.add(Integer.parseInt(mat.group(1)));
        }

        final String[] s = string.split("\\{([\\d]+)}");

        final List<TextElement> objects = new ArrayList<>();
        final Component t = this.textStyleService.oldLegacy(s[0]);
        ITextStyleService.TextFormat tuple = this.textStyleService.getLastColourAndStyle(t, null);
        objects.add(input -> t);
        int count = 1;
        for (final Integer x : map) {
            objects.add(new ArgumentElement(tuple, String.valueOf(x)));
            if (s.length > count) {
                final Component r = tuple.apply(this.textStyleService.oldLegacy(s[count])).build();
                tuple = this.textStyleService.getLastColourAndStyle(t, null);
                objects.add(element -> r);
            }

            count++;
        }

        return new Template(objects);
    }

}
