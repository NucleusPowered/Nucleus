/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.internal.services.datastore;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.api.data.JailData;
import io.github.essencepowered.essence.api.data.MuteData;
import io.github.essencepowered.essence.api.data.WarpLocation;
import io.github.essencepowered.essence.api.data.mail.MailData;
import io.github.essencepowered.essence.api.exceptions.NoSuchWorldException;
import io.github.essencepowered.essence.commands.message.SocialSpyCommand;
import io.github.essencepowered.essence.config.serialisers.LocationNode;
import io.github.essencepowered.essence.config.serialisers.UserConfig;
import io.github.essencepowered.essence.internal.CommandPermissionHandler;
import io.github.essencepowered.essence.internal.ConfigMap;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.InvulnerabilityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserService implements InternalEssenceUser {
    private final Essence plugin;
    private final User user;
    private UserConfig config;
    private final GsonConfigurationLoader loader;

    // Used as a cache.
    private Text nickname = null;

    public UserService(Essence plugin, Path file, User user) throws IOException, ObjectMappingException {
        this.plugin = plugin;
        this.loader = GsonConfigurationLoader.builder().setPath(file).build();
        this.user = user;

        load();
    }

    private void load() throws IOException, ObjectMappingException {
        ConfigurationNode cn = loader.load();
        config = cn.getValue(TypeToken.of(UserConfig.class), new UserConfig());
    }

    public void save() throws IOException, ObjectMappingException {
        ConfigurationNode cn = SimpleConfigurationNode.root();
        cn.setValue(TypeToken.of(UserConfig.class), config);
        loader.save(cn);
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Optional<MuteData> getMuteData() {
        return Optional.ofNullable(config.getMuteData());
    }

    @Override
    public void setMuteData(MuteData data) {
        config.setMuteData(data);
    }

    @Override
    public void removeMuteData() {
        config.setMuteData(null);
    }

    @Override
    public boolean isSocialSpy() {
        // Only a spy if they have the permission!
        Optional<CommandPermissionHandler> ps = plugin.getPermissionRegistry().getService(SocialSpyCommand.class);
        if (ps.isPresent()) {
            return config.isSocialspy() && ps.get().testBase(user);
        }

        return config.isSocialspy();
    }

    @Override
    public boolean setSocialSpy(boolean socialSpy) {
        config.setSocialspy(socialSpy);

        // Permission checks! Return true if it's what we wanted.
        return isSocialSpy() == socialSpy;
    }

    @Override
    public boolean isInvulnerable() {
        if (user.isOnline()) {
            config.setInvulnerable(user.getPlayer().get().get(Keys.INVULNERABILITY_TICKS).orElse(0) > 0);
        }

        return config.isInvulnerable();
    }

    @Override
    public boolean setInvulnerable(boolean invuln) {
        if (user.isOnline()) {
            Player pl = user.getPlayer().get();
            Optional<InvulnerabilityData> oid = pl.get(InvulnerabilityData.class);

            if (!oid.isPresent()) {
                return false;
            }

            InvulnerabilityData id = oid.get();
            id.invulnerableTicks().set(invuln ? Integer.MAX_VALUE : 0);
            if (!pl.offer(id).isSuccessful()) {
                return false;
            }
        }

        config.setInvulnerable(invuln);
        return true;
    }

    @Override
    public boolean isFlying() {
        if (user.isOnline()) {
            config.setFly(user.getPlayer().get().get(Keys.CAN_FLY).orElse(false));
        }

        return config.isFly();
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

        config.setFly(fly);
        return true;
    }

    @Override
    public Optional<JailData> getJailData() {
        return Optional.ofNullable(config.getJailData());
    }

    @Override
    public Instant getLastLogin() {
        return Instant.ofEpochMilli(config.getLogin());
    }

    @Override
    public void setLastLogin(Instant login) {
        config.setLogin(login.toEpochMilli());
    }

    @Override
    public Instant getLastLogout() {
        return Instant.ofEpochMilli(config.getLogout());
    }

    @Override
    public Optional<WarpLocation> getHome(String home) {
        if (config.getHomeData() == null) {
            return Optional.empty();
        }

        LocationNode ln = config.getHomeData().get(home.toLowerCase());
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
        if (config.getHomeData() == null) {
            return Maps.newHashMap();
        }

        return config.getHomeData().entrySet().stream().map(x -> {
            try {
                return new WarpLocation(x.getKey(), x.getValue().getLocation(), x.getValue().getRotation());
            } catch (NoSuchWorldException e) {
                return null;
            }
        }).filter(x -> x != null).collect(Collectors.toMap(WarpLocation::getName, y -> new WarpLocation(y.getName(), y.getLocation(), y.getRotation())));
    }

    @Override
    public boolean setHome(String home, Location<World> location, Vector3d rotation) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        Map<String, LocationNode> homeData = config.getHomeData();
        if (homeData == null) {
            homeData = Maps.newHashMap();
        }

        if (homeData.containsKey(home.toLowerCase()) || !warpName.matcher(home).matches()) {
            return false;
        }

        homeData.put(home.toLowerCase(), new LocationNode(location, rotation));
        config.setHomeData(homeData);
        return true;
    }

    @Override
    public boolean deleteHome(String home) {
        Map<String, LocationNode> homeData = config.getHomeData();
        if (homeData == null) {
            return false;
        }

        if (homeData.containsKey(home.toLowerCase())) {
            homeData.remove(home.toLowerCase());
            config.setHomeData(homeData);
            return true;
        }

        return false;
    }

    @Override
    public boolean isTeleportToggled() {
        return config.isTeleportToggled();
    }

    @Override
    public void setTeleportToggled(boolean toggle) {
        config.setTeleportToggled(toggle);
    }

    @Override
    public Optional<Text> getNicknameWithPrefix() {
        if (getNicknameAsText().isPresent()) {
            String p = plugin.getConfig(ConfigMap.MAIN_CONFIG).get().getNickPrefix();
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
        return Optional.ofNullable(config.getNickname());
    }

    @Override
    public void setNickname(String nickname) {
        config.setNickname(nickname);
        this.nickname = null;
        String p = plugin.getConfig(ConfigMap.MAIN_CONFIG).get().getNickPrefix();
        if (p != null && !p.isEmpty()) {
            nickname = p + nickname;
        }

        Text nick = TextSerializers.formattingCode('&').deserialize(nickname);
        user.offer(Keys.DISPLAY_NAME, nick);
        user.offer(Keys.SHOWS_DISPLAY_NAME, true);
    }

    @Override
    public void removeNickname() {
        nickname = null;
        user.remove(Keys.DISPLAY_NAME);
        user.offer(Keys.SHOWS_DISPLAY_NAME, false);
        config.setNickname(null);
    }

    @Override
    public List<MailData> getMail() {
        return ImmutableList.copyOf(config.getMailDataList());
    }

    @Override
    public void addMail(MailData mailData) {
        List<MailData> mailDataList = config.getMailDataList();
        if (mailDataList == null) {
            mailDataList = Lists.newArrayList();
        }

        mailDataList.add(mailData);
        config.setMailDataList(mailDataList);
    }

    @Override
    public void clearMail() {
        config.setMailDataList(Lists.newArrayList());
    }

    @Override
    public boolean isFlyingSafe() {
        return config.isFly();
    }

    @Override
    public boolean isInvulnerableSafe() {
        return config.isInvulnerable();
    }

    @Override
    public void setJailData(JailData data) {
        config.setJailData(data);
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
        LocationNode ln = config.getLocationOnLogin();
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
        config.setLocationOnLogin(new LocationNode(worldLocation));
    }

    @Override
    public void removeLocationOnLogin() {
        config.setLocationOnLogin(null);
    }

    @Override
    public void setLastLogout(Instant logout) {
        config.setLogout(logout.toEpochMilli());
    }

    @Override
    public UUID getUniqueID() {
        return user.getUniqueId();
    }

    @Override
    public boolean jailOnNextLogin() {
        return config.isJailOffline();
    }

    @Override
    public void setJailOnNextLogin(boolean set) {
        config.setJailOffline(!user.isOnline() && set);
    }
}
