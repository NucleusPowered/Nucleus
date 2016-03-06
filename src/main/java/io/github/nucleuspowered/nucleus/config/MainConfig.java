/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.PluginModule;
import io.github.nucleuspowered.nucleus.config.enumerations.ModuleOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainConfig extends AbstractConfig<CommentedConfigurationNode, HoconConfigurationLoader> {

    private Map<PluginModule, ModuleOptions> moduleOptions;
    private final String modulesSection = "modules";
    private int afkTime;
    private int afkTimeKick;
    private boolean serperateWarpPermissions;
    private List<String> allowedCommandsInJail;
    private long teleportWarmup;
    private boolean modifyChat;
    private String chatTemplate;
    private int minNickLength;
    private String nickprefix;
    private boolean debugMode;

    public MainConfig(Path file) throws IOException, ObjectMappingException {
        super(file);
    }

    @Override
    public void load() throws IOException, ObjectMappingException {
        super.load();

        // Because we execute this command from the superclass constructor, if
        // we create moduleOptions
        // as part of the declaration, it apparently doesn't get constructed
        // until AFTER the superclass constructor
        // runs. This means that when this is called from the superclass,
        // moduleOptions actually doesn't exist.
        //
        // So, the solution is to just construct it here, rather than call load
        // in every config file I create.
        if (moduleOptions == null) {
            moduleOptions = new HashMap<>();
        }

        // Recreate anything we need to re-create.
        moduleOptions.clear();
        CommentedConfigurationNode mNode = node.getNode(modulesSection);
        Arrays.asList(PluginModule.values()).forEach(p -> {
            try {
                moduleOptions.put(p, mNode.getNode(p.getKey().toLowerCase()).getValue(TypeToken.of(ModuleOptions.class), ModuleOptions.DEFAULT));
            } catch (ObjectMappingException e) {
                mNode.getNode(p.getKey().toLowerCase()).setValue(ModuleOptions.DEFAULT.name().toLowerCase());
                moduleOptions.put(p, ModuleOptions.DEFAULT);
            }
        });

        // AFK
        afkTime = node.getNode("afk", "afktime").getInt(300);
        afkTimeKick = node.getNode("afk", "afktimetokick").getInt(0);

        // Jail
        allowedCommandsInJail = node.getNode("jail", "allowed-commands").getList(TypeToken.of(String.class));

        // Warps
        serperateWarpPermissions =
                node.getNode("warps", "separate-permissions").setComment(Util.getMessageWithFormat("config.warps.separate")).getBoolean(false);

        // Teleports
        teleportWarmup = node.getNode("teleport", "warmup").getLong(3);

        // Chat
        modifyChat = node.getNode("chat", "modifychat").getBoolean(true);
        chatTemplate = node.getNode("chat", "template").getString("{{prefix}} {{name}}&f: {{message}} {{suffix}}");

        // Nicknames
        minNickLength = node.getNode("nicknames", "min-nickname-length").getInt(3);
        nickprefix = node.getNode("nicknames", "prefix").getString("&b~");
        debugMode = node.getNode("debug-mode").getBoolean(false);
    }

    @Override
    protected HoconConfigurationLoader getLoader(Path file) {
        return HoconConfigurationLoader.builder().setPath(file).build();
    }

    @Override
    protected CommentedConfigurationNode getDefaults() {
        CommentedConfigurationNode ccn = SimpleCommentedConfigurationNode.root();

        // Load in the modules.
        CommentedConfigurationNode modules =
                ccn.getNode(modulesSection).setComment(Util.getMessageWithFormat("config.modules", "default", "forceload", "disabled"));
        Arrays.asList(PluginModule.values())
                .forEach(m -> modules.getNode(m.getKey().toLowerCase()).setValue(ModuleOptions.DEFAULT.name().toLowerCase()));

        // AFK module
        CommentedConfigurationNode afkc = ccn.getNode("afk").setComment(Util.getMessageWithFormat("config.afk"));
        afkc.getNode("afktime").setComment(Util.getMessageWithFormat("config.afk.time")).setValue(300);
        afkc.getNode("afktimetokick").setComment(Util.getMessageWithFormat("config.afk.timetokick")).setValue(0);

        try {
            ccn.getNode("jail", "allowed-commands").setComment(Util.getMessageWithFormat("config.jail.commands")).setValue(new TypeToken<List<String>>() {
            }, Lists.newArrayList("m", "msg", "r", "mail", "rules", "info"));
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }

        ccn.getNode("warps", "separate-permissions").setComment(Util.getMessageWithFormat("config.warps.separate")).setValue(false);

        ccn.getNode("chat", "modifychat").setComment(Util.getMessageWithFormat("config.chat.modify")).setValue(true);
        ccn.getNode("chat", "template").setComment(Util.getMessageWithFormat("config.chat.template"))
                .setValue("{{prefix}} {{name}}&f: {{message}} {{suffix}}");

        ccn.getNode("nicknames", "min-nickname-length").setComment(Util.getMessageWithFormat("config.nicknames.min")).setValue(3);
        ccn.getNode("nicknames", "prefix").setComment(Util.getMessageWithFormat("config.nicknames.prefix")).setValue("&b~");

        ccn.getNode("debug-mode").setComment(Util.getMessageWithFormat("config.debugmode")).setValue(false);
        return ccn;
    }

    public Map<PluginModule, ModuleOptions> getModuleOptions() {
        return ImmutableMap.copyOf(moduleOptions);
    }

    public int getAfkTime() {
        return afkTime;
    }

    public int getAfkTimeToKick() {
        return afkTimeKick;
    }

    public List<String> getAllowedCommandsInJail() {
        return ImmutableList.copyOf(allowedCommandsInJail);
    }

    public boolean useSeparatePermissionsForWarp() {
        return serperateWarpPermissions;
    }

    public long getTeleportWarmup() {
        if (teleportWarmup < 0) {
            return 0;
        }

        return teleportWarmup;
    }

    public boolean getModifyChat() {
        return modifyChat;
    }

    public String getChatTemplate() {
        return chatTemplate;
    }

    public int getMinNickLength() {
        return minNickLength;
    }

    public String getNickPrefix() {
        return nickprefix;
    }

    public boolean getDebugMode() {
        return debugMode;
    }
}
