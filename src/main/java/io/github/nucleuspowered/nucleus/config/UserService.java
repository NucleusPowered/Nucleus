/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.config;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.data.JailData;
import io.github.nucleuspowered.nucleus.api.data.MuteData;
import io.github.nucleuspowered.nucleus.api.data.WarpLocation;
import io.github.nucleuspowered.nucleus.api.data.mail.MailData;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchWorldException;
import io.github.nucleuspowered.nucleus.config.bases.AbstractSerialisableClassConfig;
import io.github.nucleuspowered.nucleus.config.serialisers.LocationNode;
import io.github.nucleuspowered.nucleus.config.serialisers.UserDataNode;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.interfaces.InternalNucleusUser;
import io.github.nucleuspowered.nucleus.modules.message.commands.SocialSpyCommand;
import io.github.nucleuspowered.nucleus.modules.nickname.config.NicknameConfigAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserService extends AbstractSerialisableClassConfig<UserDataNode, ConfigurationNode, GsonConfigurationLoader>
        implements InternalNucleusUser {

    private final Nucleus plugin;
    private final User user;

    // Used as a cache.
    private Text nickname = null;

    public UserService(Nucleus plugin, Path file, User user) throws Exception {
        super(file, TypeToken.of(UserDataNode.class), UserDataNode::new);
        this.plugin = plugin;
        this.user = user;
    }

    @Override
    protected GsonConfigurationLoader getLoader(Path file) {
        return GsonConfigurationLoader.builder().setPath(file).build();
    }

    @Override
    protected ConfigurationNode getNode() {
        return SimpleConfigurationNode.root();
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Optional<MuteData> getMuteData() {
        return Optional.ofNullable(data.getMuteData());
    }

    @Override
    public void setMuteData(MuteData mData) {
        data.setMuteData(mData);
    }

    @Override
    public void removeMuteData() {
        data.setMuteData(null);
    }

    @Override
    public boolean isSocialSpy() {
        // Only a spy if they have the permission!
        Optional<CommandPermissionHandler> ps = plugin.getPermissionRegistry().getService(SocialSpyCommand.class);
        if (ps.isPresent()) {
            return data.isSocialspy() && ps.get().testBase(user);
        }

        return data.isSocialspy();
    }

    @Override
    public boolean setSocialSpy(boolean socialSpy) {
        data.setSocialspy(socialSpy);

        // Permission checks! Return true if it's what we wanted.
        return isSocialSpy() == socialSpy;
    }

    @Override
    public boolean isInvulnerable() {
        return data.isInvulnerable();
    }

    @Override
    public void setInvulnerable(boolean invuln) {
        data.setInvulnerable(invuln);
    }

    @Override
    public boolean isFlying() {
        if (user.isOnline()) {
            data.setFly(user.getPlayer().get().get(Keys.CAN_FLY).orElse(false));
        }

        return data.isFly();
    }

    @Override
    public boolean setFlying(boolean fly) {
        if (user.isOnline()) {
            Player pl = user.getPlayer().get();
            if (!fly && !pl.offer(Keys.IS_FLYING, false).isSuccessful()) {
                return false;
            }

            if (!pl.offer(Keys.CAN_FLY, fly).isSuccessful()) {
                return false;
            }
        }

        data.setFly(fly);
        return true;
    }

    @Override
    public Optional<JailData> getJailData() {
        return Optional.ofNullable(data.getJailData());
    }

    @Override
    public Instant getLastLogin() {
        return Instant.ofEpochMilli(data.getLogin());
    }

    @Override
    public void setLastLogin(Instant login) {
        data.setLogin(login.toEpochMilli());
    }

    @Override
    public Instant getLastLogout() {
        return Instant.ofEpochMilli(data.getLogout());
    }

    @Override
    public Optional<WarpLocation> getHome(String home) {
        if (data.getHomeData() == null) {
            return Optional.empty();
        }

        LocationNode ln = data.getHomeData().get(home.toLowerCase());
        if (ln != null) {
            try {
                return Optional.of(new WarpLocation(home.toLowerCase(), ln.getLocation(), ln.getRotation()));
            } catch (NoSuchWorldException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @Override
    public Map<String, WarpLocation> getHomes() {
        if (data.getHomeData() == null) {
            return Maps.newHashMap();
        }

        return data.getHomeData().entrySet().stream().map(x -> {
            try {
                return new WarpLocation(x.getKey(), x.getValue().getLocation(), x.getValue().getRotation());
            } catch (NoSuchWorldException e) {
                return null;
            }
        }).filter(x -> x != null)
                .collect(Collectors.toMap(WarpLocation::getName, y -> new WarpLocation(y.getName(), y.getLocation(), y.getRotation())));
    }

    @Override
    public boolean setHome(String home, Location<World> location, Vector3d rotation) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        Map<String, LocationNode> homeData = data.getHomeData();
        if (homeData == null) {
            homeData = Maps.newHashMap();
        }

        if (homeData.containsKey(home.toLowerCase()) || !warpName.matcher(home).matches()) {
            return false;
        }

        homeData.put(home.toLowerCase(), new LocationNode(location, rotation));
        data.setHomeData(homeData);
        return true;
    }

    @Override
    public boolean deleteHome(String home) {
        Map<String, LocationNode> homeData = data.getHomeData();
        if (homeData == null) {
            return false;
        }

        if (homeData.containsKey(home.toLowerCase())) {
            homeData.remove(home.toLowerCase());
            data.setHomeData(homeData);
            return true;
        }

        return false;
    }

    @Override
    public boolean isTeleportToggled() {
        return data.isTeleportToggled();
    }

    @Override
    public void setTeleportToggled(boolean toggle) {
        data.setTeleportToggled(toggle);
    }

    @Override
    public Optional<Text> getNicknameWithPrefix() {
        if (getNicknameAsText().isPresent()) {
            String p = getNickPrefix();
            if (p == null || p.isEmpty()) {
                return getNicknameAsText();
            }

            return Optional.of(Text.join(TextSerializers.formattingCode('&').deserialize(p), getNicknameAsText().get()));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Text> getNicknameAsText() {
        if (this.nickname != null) {
            return Optional.of(this.nickname);
        }

        Optional<String> os = getNicknameAsString();
        if (!os.isPresent()) {
            return Optional.empty();
        }

        nickname = TextSerializers.formattingCode('&').deserialize(os.get());
        return Optional.of(nickname);
    }

    @Override
    public Optional<String> getNicknameAsString() {
        return Optional.ofNullable(data.getNickname());
    }

    @Override
    public void setNickname(String nickname) {
        data.setNickname(nickname);
        this.nickname = null;
        String p = getNickPrefix();
        if (p != null && !p.isEmpty()) {
            nickname = p + nickname;
        }

        Text nick = TextSerializers.formattingCode('&').deserialize(nickname);
        user.offer(Keys.DISPLAY_NAME, nick);
    }

    @Override
    public void removeNickname() {
        nickname = null;
        user.remove(Keys.DISPLAY_NAME);
        data.setNickname(null);
    }

    @Override
    public List<MailData> getMail() {
        return ImmutableList.copyOf(data.getMailDataList());
    }

    @Override
    public void addMail(MailData mailData) {
        List<MailData> mailDataList = data.getMailDataList();
        if (mailDataList == null) {
            mailDataList = Lists.newArrayList();
        }

        mailDataList.add(mailData);
        data.setMailDataList(mailDataList);
    }

    @Override
    public boolean clearMail() {
        if (!data.getMailDataList().isEmpty()) {
            data.setMailDataList(Lists.newArrayList());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isFlyingSafe() {
        return data.isFly();
    }

    @Override
    public void setJailData(JailData jdata) {
        data.setJailData(jdata);
    }

    @Override
    public void removeJailData() {
        setJailData(null);
    }

    @Override
    public void setOnLogout() {
        setLastLogout(Instant.now());

        // Set data based toggles.
        isFlying();
        isInvulnerable();
    }

    @Override
    public Optional<Location<World>> getLocationOnLogin() {
        LocationNode ln = data.getLocationOnLogin();
        if (ln == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(ln.getLocation());
        } catch (NoSuchWorldException e) {
            return Optional.empty();
        }
    }

    @Override
    public void sendToLocationOnLogin(Location<World> worldLocation) {
        data.setLocationOnLogin(new LocationNode(worldLocation));
    }

    @Override
    public void removeLocationOnLogin() {
        data.setLocationOnLogin(null);
    }

    @Override
    public void setLastLogout(Instant logout) {
        data.setLogout(logout.toEpochMilli());
    }

    @Override
    public UUID getUniqueID() {
        return user.getUniqueId();
    }

    @Override
    public boolean jailOnNextLogin() {
        return data.isJailOffline();
    }

    @Override
    public void setJailOnNextLogin(boolean set) {
        data.setJailOffline(!user.isOnline() && set);
    }

    @Override
    public Map<String, Instant> getKitLastUsedTime() {
        final Map<String, Instant> r = Maps.newHashMap();
        data.getKitLastUsedTime().forEach((k, v) -> r.put(k, Instant.ofEpochSecond(v)));
        return r;
    }

    @Override
    public void addKitLastUsedTime(String kitName, Instant lastTime) {
        Map<String, Long> kitLastUsedTime = data.getKitLastUsedTime();
        kitLastUsedTime.put(kitName, lastTime.getEpochSecond());
        data.setKitLastUsedTime(kitLastUsedTime);
    }

    @Override
    public void removeKitLastUsedTime(String kitName) {
        Map<String, Long> kitLastUsedTime = data.getKitLastUsedTime();
        kitLastUsedTime.remove(kitName);
        data.setKitLastUsedTime(kitLastUsedTime);
    }

    // -- Powertools
    @Override
    public Map<String, List<String>> getPowertools() {
        return ImmutableMap.copyOf(data.getPowertools());
    }

    @Override
    public Optional<List<String>> getPowertoolForItem(ItemType item) {
        List<String> tools = data.getPowertools().get(item.getId());
        if (tools != null) {
            return Optional.of(ImmutableList.copyOf(tools));
        }

        return Optional.empty();
    }

    @Override
    public void setPowertool(ItemType type, List<String> commands) {
        data.getPowertools().put(type.getId(), commands);
    }

    @Override
    public void clearPowertool(ItemType type) {
        data.getPowertools().remove(type.getId());
    }

    @Override
    public void clearPowertool(String type) {
        data.getPowertools().remove(type);
    }

    @Override
    public boolean isPowertoolToggled() {
        return data.isPowertoolToggle();
    }

    @Override
    public void setPowertoolToggle(boolean set) {
        data.setPowertoolToggle(set);
    }

    @Override
    public List<UUID> getIgnoreList() {
        return ImmutableList.copyOf(data.getIgnoreList());
    }

    @Override
    public boolean addToIgnoreList(UUID uuid) {
        if (!data.getIgnoreList().contains(uuid)) {
            data.getIgnoreList().add(uuid);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeFromIgnoreList(UUID uuid) {
        return data.getIgnoreList().remove(uuid);
    }

    @Override
    public boolean isFrozen() {
        return data.isFrozen();
    }

    @Override
    public void setFrozen(boolean value) {
        data.setFrozen(value);
    }

    private String getNickPrefix() {
        try {
            return plugin.getModuleContainer().getConfigAdapterForModule("nickname", NicknameConfigAdapter.class).getNodeOrDefault().getPrefix();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return null;
        }
    }
}
