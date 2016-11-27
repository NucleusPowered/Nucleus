/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.NoCostArgument;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.CostCancellableTask;
import io.github.nucleuspowered.nucleus.internal.annotations.ConfigCommandAlias;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.NotifyIfAFK;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RequireMixinPlugin;
import io.github.nucleuspowered.nucleus.internal.annotations.RequiresEconomy;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.afk.handlers.AFKHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.TextMessageException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The basis for any command.
 */
public abstract class AbstractCommand<T extends CommandSource> implements CommandExecutor {

    private final boolean isAsync = this.getClass().getAnnotation(RunAsync.class) != null;

    // A period separated list of parent commands, starting with the prefix. Period terminateed.
    private final String commandPath;

    // Null until set, then should be considered immutable.
    private Set<Class<? extends AbstractCommand<?>>> moduleCommands = null;

    private final Map<UUID, Instant> cooldownStore = Maps.newHashMap();
    protected CommandPermissionHandler permissions;
    protected String[] aliases;
    protected String[] forcedAliases;
    private final Class<T> sourceType;
    private boolean bypassWarmup;
    private boolean generateWarmupAnyway;
    private boolean bypassCooldown;
    private boolean bypassCost;
    private boolean requiresEconomy;
    private String configSection;
    private boolean generateDefaults;
    private CommandSpec cs = null;
    private final List<String> afkArgs = Lists.newArrayList();
    private final boolean isRoot;

    private final UsageCommand usageCommand = new UsageCommand();

    @Inject protected NucleusPlugin plugin;
    @Inject private CoreConfigAdapter cca;
    @Inject private WarmupManager warmupService;

    @SuppressWarnings("all")
    private Optional<AFKHandler> afkHandler = null;

    @SuppressWarnings("all")
    private Optional<AFKConfigAdapter> aca = null;
    private CommandBuilder builder;
    private String module;
    private String moduleId;

    @SuppressWarnings("unchecked")
    public AbstractCommand() {
        // I hate type erasure - it leads to a hack like this. Admittedly, I
        // could've just created a subclass that does
        // the same thing, but I like to beat the system! :)
        //
        // This code reflectively looks for methods called "executeCommand",
        // which is defined in this class. However,
        // due to type erasure, if a generic type is specified, there are two
        // "executeCommand" methods, one that satisfies
        // this abstract class (with the CommandSource argument) and a second
        // method that fulfils the generic type T.
        //
        // Thus, we need to check that there is a method called executeCommand
        // that has a more restrictive argument than
        // "CommandSource" in order to check for the generic type.
        //
        // This allows us to then have code that filters out non-Players by
        // simply specifying the generic type "Player".
        //
        // The provided stream filters out the standard executeCommand method,
        // and checks to see if there is a second that makes
        // use of the generic parameter.
        Optional<Method> me = Arrays.stream(getClass().getMethods())
                .filter(x -> x.getName().equals("executeCommand") && x.getParameterTypes().length == 2
                        && x.getParameterTypes()[1].isAssignableFrom(CommandContext.class) && !x.getParameterTypes()[0].equals(CommandSource.class))
                .findFirst();

        // If there is a second executeCommand method, then we know that's the
        // type that we need and we can do our source
        // checks against it accordingly.
        if (me.isPresent()) {
            sourceType = (Class<T>) (me.get().getParameterTypes()[0]);
        } else {
            sourceType = (Class<T>) CommandSource.class;
        }

        this.commandPath = getSubcommandOf();
        RegisterCommand rc = this.getClass().getAnnotation(RegisterCommand.class);
        this.isRoot = rc != null && rc.subcommandOf().equals(AbstractCommand.class);
    }

    /**
     * The command will only load if this condition is true. Happens after injections.
     *
     * @return <code>true</code> if the command can be loaded.
     */
    public boolean canLoad() {
        return true;
    }

    private String getSubcommandOf() {
        StringBuilder sb = new StringBuilder();
        getSubcommandOf(this.getClass(), sb, false);
        return sb.toString();
    }

    private void getSubcommandOf(Class<? extends AbstractCommand> c, StringBuilder sb, boolean appendPeriod) {
        // Get subcommand alias, if any.
        RegisterCommand rc = c.getAnnotation(RegisterCommand.class);
        if (!Modifier.isAbstract( rc.subcommandOf().getModifiers()) && rc.subcommandOf() != this.getClass()) {
            getSubcommandOf(rc.subcommandOf(), sb, true);
        }

        sb.append(rc.value()[0]);
        if (appendPeriod) {
            sb.append(".");
        }
    }

    public final void setModuleCommands(Set<Class<? extends AbstractCommand<?>>> moduleCommands) {
        Preconditions.checkState(this.moduleCommands == null);
        this.moduleCommands = moduleCommands;
    }

    public final void setCommandBuilder(CommandBuilder builder) {
        this.builder = builder;
    }

    public final void postInit() {
        // Checks.
        Preconditions.checkNotNull(this.getAliases());
        Preconditions.checkArgument(this.getAliases().length > 0);

        // For these flags, we simply need to get whether the annotation was
        // declared. If they were not, we simply get back
        // a null - so the check is based around that.
        NoWarmup w = this.getClass().getAnnotation(NoWarmup.class);
        bypassWarmup = w != null;
        generateWarmupAnyway = !bypassWarmup || w.generateConfigEntry();

        bypassCooldown = this.getClass().getAnnotation(NoCooldown.class) != null;
        bypassCost = this.getClass().getAnnotation(NoCost.class) != null;

        // The Permissions annotation provides the backbone of the permissions
        // system for the commands.
        // The standard permission is based on the getAliases command,
        // specifically, the first argument in the
        // returned array, such that the permission it will generated if this
        // annotation is defined is:
        //
        // nucleus.(primaryalias).base
        //
        // Adding a "prefix" and/or "suffix" string will generate:
        //
        // nucleus.(prefix).(primaryalias).(suffix).base
        //
        // For warmup, cooldown and cost exemption, replace base with:
        //
        // exempt.(cooldown|warmup|cost)
        //
        // By default, the permission "nucleus.admin" also gets permission to
        // run and bypass all warmups, cooldowns and costs, but this can be
        // turned off in the annotation.
        permissions = plugin.getPermissionRegistry().getService(this.getClass());

        ConfigCommandAlias cca = this.getClass().getAnnotation(ConfigCommandAlias.class);
        if (this.commandPath == null || this.commandPath.isEmpty() || !this.commandPath.contains(".")) {
            configSection = "";
        } else {
            configSection = this.commandPath.replaceAll("\\.[^.]+$", ".");
        }

        configSection = configSection + (cca == null ? getAliases()[0].toLowerCase() : cca.value().toLowerCase());
        generateDefaults = cca == null || cca.generate();

        NotifyIfAFK n = this.getClass().getAnnotation(NotifyIfAFK.class);
        if (n != null) {
            this.afkArgs.addAll(Arrays.asList(n.value()));
        }

        permissionsToRegister().forEach((k, v) -> permissions.registerPermssion(k, v));
        permissionSuffixesToRegister().forEach((k, v) -> permissions.registerPermssionSuffix(k, v));

        requiresEconomy = this.getClass().isAnnotationPresent(RequiresEconomy.class);
        afterPostInit();
    }

    protected void afterPostInit() {
    }

    /**
     * Gets the aliases for the command. The first alias will be the primary
     * alias within NucleusPlugin.
     *
     * @return An array of aliases.
     */
    public String[] getAliases() {
        if (aliases == null) {
            RegisterCommand rc = getClass().getAnnotation(RegisterCommand.class);
            if (rc != null) {
                aliases = rc.value();
            }
        }

        return Arrays.copyOf(aliases, aliases.length);
    }

    /**
     * Gets any aliases that need to be forced.
     *
     * @return An array of aliases.
     */
    public String[] getRootCommandAliases() {
        if (forcedAliases == null) {
            RegisterCommand rc = getClass().getAnnotation(RegisterCommand.class);
            if (rc != null) {
                forcedAliases = rc.rootAliasRegister();
            }
        }

        return Arrays.copyOf(forcedAliases, forcedAliases.length);
    }

    /**
     * Functionally similar to
     * {@link CommandExecutor#execute(CommandSource, CommandContext)}, this
     * contains logic that actually executes the command.
     *
     * <p> Note that the {@link CommandResult} is important here. A success is
     * treated differently to a non-success! </p>
     *
     * @param src The executor of the command.
     * @param args The arguments for the command.
     * @return The {@link CommandResult}
     * @throws Exception If thrown, {@link TextMessageException#getText()} or
     *         {@link Exception#getMessage()} will be sent to the user.
     */
    public abstract CommandResult executeCommand(T src, CommandContext args) throws Exception;

    // -------------------------------------
    // Metadata
    // -------------------------------------

    public String getCommandPath() {
        return commandPath;
    }

    /**
     * Gets the description for the command.
     *
     * @return The description.
     */
    public String getDescription() {
        return getFromCommandKey("desc");
    }

    /**
     * Gets the description for the command.
     *
     * @return The description.
     */
    public String getExtendedDescription() {
        return getFromCommandKey("extended");
    }

    private String getFromCommandKey(String type) {
        String key = String.format("%s.%s", this.commandPath, type);
        try {
            return NucleusPlugin.getNucleus().getCommandMessageProvider().getMessageWithFormat(key);
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                plugin.getLogger().debug("Could not get command resource key " + key);
            }

            return "";
        }
    }

    /**
     * Contains extra permission suffixes that are to be registered in the
     * permission service.
     *
     * @return A map containing the extra permission suffixes to register.
     */
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return Maps.newHashMap();
    }

    /**
     * Contains extra permissions that are to be registered in the permission
     * service.
     *
     * @return A map containing the extra permissions to register.
     */
    protected Map<String, PermissionInformation> permissionsToRegister() {
        return Maps.newHashMap();
    }

    public final CommandPermissionHandler getPermissionHandler() {
        return permissions;
    }

    public final String getUsage(CommandSource source) {
        return "/" + getCommandPath().replaceAll("\\.", " ") + " " + getSpec().getUsage(source).toPlain().replaceAll("\\?\\|", "");
    }

    String getCommandConfigAlias() {
        if (configSection == null) {
            return getAliases()[0];
        }

        return configSection;
    }

    /**
     * Gets the arguments of the command.
     *
     * @return The arguments of the command.
     */
    public CommandElement[] getArguments() {
        return new CommandElement[]{};
    }

    /**
     * Returns a {@link CommandSpec} that allows this command to be registered.
     *
     * @return The {@link CommandSpec}
     */
    private CommandSpec createSpec() {
        Preconditions.checkState(permissions != null);
        RegisterCommand rc = getClass().getAnnotation(RegisterCommand.class);

        CommandSpec.Builder cb = CommandSpec.builder();
        if (rc == null || rc.hasExecutor()) {
            cb.executor(this).arguments(getArguments());
        }

        if (!permissions.isPassthrough()) {
            cb.permission(permissions.getBase());
        }

        String description = getDescription();
        if (!description.isEmpty()) {
            cb.description(Text.of(description));
        }

        String extended = getExtendedDescription();
        if (!extended.isEmpty()) {
            cb.description(Text.of(extended));
        }

        Map<List<String>, CommandCallable> m = createChildCommands();
        if (!m.isEmpty()) {
            cb.children(m);
        }

        cs = cb.build();
        return cs;
    }

    CommandSpec getSpec() {
        if (cs != null) {
            return cs;
        }

        try {
            return createSpec();
        } catch (Exception e) {
            return null;
        }
    }

    final boolean mergeDefaults() {
        return generateDefaults;
    }

    public CommentedConfigurationNode getDefaults() {
        CommentedConfigurationNode n = SimpleCommentedConfigurationNode.root();

        if (this.isRoot) {
            n.getNode("enabled").setComment(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("config.enabled")).setValue(true);
        }

        if (!bypassCooldown) {
            n.getNode("cooldown").setComment(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("config.cooldown")).setValue(0);
        }

        if (!bypassWarmup || generateWarmupAnyway) {
            n.getNode("warmup").setComment(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("config.warmup")).setValue(0);
        }

        if (!bypassCost) {
            n.getNode("cost").setComment(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("config.cost")).setValue(0);
        }

        for (String alias : this.getRootCommandAliases()) {
            n.getNode("aliases").getNode(alias).setValue(true);
        }

        return n;
    }

    // -------------------------------------
    // Command Execution
    // -------------------------------------

    /**
     * Runs any checks that need to occur before the warmup, cooldown and cost
     * checks.
     *
     * @param source The source of the command.
     * @param args The arguments.
     * @return Whether to continue or not.
     */
    protected ContinueMode preProcessChecks(T source, CommandContext args) {
        return ContinueMode.CONTINUE;
    }

    @Override
    @NonnullByDefault
    public final CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        // If the implementing class has defined a generic parameter, then check
        // the source type.
        if (!checkSourceType(source)) {
            return CommandResult.empty();
        }

        // Economy
        if (requiresEconomy && !plugin.getEconHelper().economyServiceExists()) {
            source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.economyrequired"));
            return CommandResult.empty();
        }

        // Cast as required.
        @SuppressWarnings("unchecked")
        T src = (T) source;

        try {
            ContinueMode mode = preProcessChecks(src, args);
            if (!mode.cont) {
                return mode.returnType;
            }
        } catch (Exception e) {
            // If it doesn't, just tell the user something went wrong.
            src.sendMessage(Text.builder().append(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.error")).build());

            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return CommandResult.empty();
        }

        if (src instanceof Player) {
            ContinueMode cm = runChecks((Player) src, args);

            if (!cm.cont) {
                return cm.returnType;
            }
        }

        // If we're running async...
        if (isAsync) {
            // Create an executor that runs the command async.
            plugin.getLogger().debug("Running " + this.getClass().getName() + " in async mode.");
            Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> startExecute(src, args));

            // Tell Sponge we're done.
            return CommandResult.success();
        }

        // Run the command sync.
        return startExecute(src, args);
    }

    private CommandResult startExecute(T src, CommandContext args) {
        CommandResult cr;
        try {
            // Checks to see if a target is AFK, based on the "NotifyIfAFK" argument.
            checkAfk(src, args);

            // Execute the command in the specific executor.
            cr = executeCommand(src, args);
        } catch (ReturnMessageException e) {
            Text t = e.getText();
            src.sendMessage((t == null) ? NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.error") : t);
            cr = CommandResult.empty();
        } catch (TextMessageException e) {
            // If the exception contains a text object, render it like so...
            Text t = e.getText();
            src.sendMessage((t == null) ? NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.error") : t);

            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            cr = CommandResult.empty();
        } catch (Exception e) {
            // If it doesn't, just tell the user something went wrong.
            src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.error"));

            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            cr = CommandResult.empty();
        }

        if (src instanceof Player) {
            // If the player is subject to cooling down, apply the cooldown.
            final Player p = (Player) src;

            if (cr.getSuccessCount().orElse(0) > 0) {
                setCooldown(p);
            } else {
                // For the tests, keep this here so we can skip the hard to test
                // code below.
                final double cost = getCost(p, args);
                if (cost > 0) {
                    Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> plugin.getEconHelper().depositInPlayer(p, cost));
                }
            }
        }

        return cr;
    }

    // -------------------------------------
    // Source Type
    // -------------------------------------
    private boolean checkSourceType(CommandSource source) {
        if (sourceType.equals(Player.class) && !(source instanceof Player)) {
            source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.playeronly"));
            return false;
        } else if (sourceType.equals(ConsoleSource.class) && !(source instanceof Player)) {
            source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.consoleonly"));
            return false;
        } else if (sourceType.equals(CommandBlockSource.class) && !(source instanceof CommandBlockSource)) {
            source.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("command.commandblockonly"));
            return false;
        }

        return true;
    }

    // -------------------------------------
    // Player Checks
    // -------------------------------------
    private ContinueMode runChecks(Player src, CommandContext args) {
        // Cooldown, cost, warmup.
        ContinueMode m = checkCooldown(src);
        if (!m.cont) {
            return m;
        }

        m = applyCost(src, args);
        if (!m.cont) {
            return m;
        }

        return setupWarmup(src, args);
    }

    /**
     * Gets the world properties from the specified argument.
     *
     * @param src The {@link CommandSource} executing the command.
     * @param argument The key of the argument to get the world properties from.
     * @param args The {@link CommandContext} with the data
     * @return An {@link Optional} containing the {@link WorldProperties}, if appropriate.
     */
    protected final Optional<WorldProperties> getWorldProperties(CommandSource src, String argument, CommandContext args) {
        Optional<WorldProperties> pr = args.getOne(argument);
        if (pr.isPresent()) {
            return pr;
        }

        // Actually, we just care about where we are.
        if (src instanceof Locatable) {
            return Optional.of(((Locatable) src).getWorld().getProperties());
        }

        return Optional.empty();
    }

    protected final WorldProperties getWorldPropertiesOrDefault(CommandSource src, String argument, CommandContext args) {
        Optional<WorldProperties> pr = getWorldProperties(src, argument, args);
        if (pr.isPresent()) {
            return pr.get();
        }

        src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("args.worldproperties.default"));
        return Sponge.getServer().getDefaultWorld().get();
    }

    protected final <U extends User> U getUserFromArgs(Class<U> clazz, CommandSource src, String argument, CommandContext args) throws ReturnMessageException {
        return getUserFromArgs(clazz, src, argument, args, "command.playeronly");
    }

    /**
     * Gets a {@link User} from the specified argument, or if one does not exist, attempts to use the
     * player currently running the command, if there is one.
     *
     * @param clazz The {@link Class} of the object we're expecting, which might be a {@link User} or {@link Player}
     * @param src The {@link CommandSource} running the command.
     * @param argument The name of the argument to check.
     * @param args The {@link CommandContext} that would contain the arguement result.
     * @param failKey The translation key to use if the method fails to produce a user.
     * @param <U> The type of object we're looking for.
     * @throws ReturnMessageException if no user was found.
     *
     * @return An object of type {@code clazz}.
     */
    protected final <U extends User> U getUserFromArgs(Class<U> clazz, CommandSource src, String argument, CommandContext args, String failKey) throws ReturnMessageException {
        Optional<U> opl = args.getOne(argument);
        if (opl.isPresent()) {
            return opl.get();
        } else if (clazz.isInstance(src)) {
            return clazz.cast(src);
        } else {
            throw new ReturnMessageException(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat(failKey));
        }
    }

    @Nonnull
    protected final WorldProperties getWorldFromUserOrArgs(CommandSource src, String argument, CommandContext args) throws ReturnMessageException {
        Optional<WorldProperties> owp = args.getOne(argument);
        if (owp.isPresent()) {
           return owp.get();
        } else {
            if (src instanceof Locatable) {
                return ((Locatable) src).getWorld().getProperties();
            } else {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.noworldconsole"));
            }
        }
    }

    @Nonnull
    protected final CatalogType getCatalogTypeFromHandOrArgs(CommandSource src, String argument, CommandContext args) throws ReturnMessageException {
        Optional<CatalogType> catalogTypeOptional = args.getOne(argument);
        CatalogType type;
        if (catalogTypeOptional.isPresent()) {
            return catalogTypeOptional.get();
        } else {
            // If player, get the item in hand, otherwise, we can't continue.
            if (src instanceof Player) {
                return Util.getTypeFromItemInHand((Player)src)
                    .orElseThrow(() -> new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.noneinhand")));
            }

            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.noitemconsole"));
        }
    }

    // -------------------------------------
    // Warmups
    // -------------------------------------
    protected int getWarmup(final Player src) {
        if (permissions.testWarmupExempt(src)) {
            return 0;
        }

        // Get the warmup time.
        return plugin.getCommandsConfig().getCommandNode(configSection).getNode("warmup").getInt();
    }

    @SuppressWarnings("unchecked")
    private ContinueMode setupWarmup(final Player src, CommandContext args) {
        if (bypassWarmup) {
            return ContinueMode.CONTINUE;
        }

        // Get the warmup time.
        int warmupTime = getWarmup(src);
        if (warmupTime <= 0) {
            return ContinueMode.CONTINUE;
        }

        // We create a task that executes the command at a later time. Because
        // we already know we have permission,
        // we can skip those checks.
        Task.Builder tb = Sponge.getScheduler().createTaskBuilder().delay(warmupTime, TimeUnit.SECONDS)
                .execute(new CostCancellableTask(plugin, src, getCost(src, args)) {

                    @Override
                    public void accept(Task task) {
                        src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("warmup.end"));
                        warmupService.removeWarmup(src.getUniqueId());
                        startExecute((T) src, args);
                    }
                }).name("Command Warmup - " + src.getName());

        // Run an async command async, of course!
        if (isAsync) {
            tb.async();
        }

        // Add the warmup to the service so we can cancel it if we need to.
        warmupService.addWarmup(src.getUniqueId(), tb.submit(plugin));

        // Tell the user we're warming up.
        src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("warmup.start", Util.getTimeStringFromSeconds(warmupTime)));

        WarmupConfig wc = cca.getNodeOrDefault().getWarmupConfig();
        if (wc.isOnMove() && wc.isOnCommand()) {
            src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("warmup.both"));
        } else if (wc.isOnMove()) {
            src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("warmup.onMove"));
        } else if (wc.isOnCommand()) {
            src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("warmup.onCommand"));
        }

        // Sponge should think the command was run successfully.
        return ContinueMode.STOP_SUCCESS;
    }

    // -------------------------------------
    // Cooldowns
    // -------------------------------------
    private ContinueMode checkCooldown(Player src) {
        // Remove any expired cooldowns.
        cleanCooldowns();

        // If they are still in there, then tell them they are still cooling
        // down.
        if (!bypassCooldown && !permissions.testCooldownExempt(src) && cooldownStore.containsKey(src.getUniqueId())) {
            Instant l = cooldownStore.get(src.getUniqueId());
            src.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("cooldown.message",
                    Util.getTimeStringFromSeconds(l.until(Instant.now(), ChronoUnit.SECONDS))));
            return ContinueMode.STOP;
        }

        return ContinueMode.CONTINUE;
    }

    private void setCooldown(Player src) {
        if (!permissions.testCooldownExempt(src)) {
            // Get the cooldown time.
            int cooldownTime = plugin.getCommandsConfig().getCommandNode(configSection).getNode("cooldown").getInt();
            if (cooldownTime > 0) {
                // If there is a cooldown, add the cooldown to the list, with
                // the end time as an Instant.
                cooldownStore.put(src.getUniqueId(), Instant.now().plus(cooldownTime, ChronoUnit.SECONDS));
            }
        }
    }

    protected void removeCooldown(UUID uuid) {
        cooldownStore.remove(uuid);
    }

    private void cleanCooldowns() {
        // If the cooldown end is in the past, remove it.
        cooldownStore.entrySet().removeIf(k -> k.getValue().isBefore(Instant.now()));
    }

    // -------------------------------------
    // Costs
    // -------------------------------------
    /**
     * Applies a cost to the user, if required.
     *
     * @param src The {@link CommandSource}
     * @param args The {@link CommandContext}
     * @return Whether to continue with the command.
     */
    private ContinueMode applyCost(Player src, CommandContext args) {
        double cost = getCost(src, args);
        if (cost == 0.) {
            return ContinueMode.CONTINUE;
        }

        if (!plugin.getEconHelper().withdrawFromPlayer(src, cost)) {
            return ContinueMode.STOP;
        }

        return ContinueMode.CONTINUE;
    }

    /**
     * Gets the cost for this command, or zero if the player does not have to
     * pay.
     *
     * @param src The {@link CommandSource}
     * @param args The {@link CommandContext}
     * @return The cost.
     */
    protected double getCost(Player src, @Nullable CommandContext args) {
        boolean noCost = args != null && args.<Boolean>getOne(NoCostArgument.NO_COST_ARGUMENT).orElse(false);

        // If the player or command itself is exempt, return a zero.
        if (bypassCost || noCost || permissions.testCostExempt(src)) {
            return 0.;
        }

        // Return the cost if positive, else, zero.
        double cost = plugin.getCommandsConfig().getCommandNode(configSection).getNode("cost").getDouble(0.);
        if (cost <= 0.) {
            return 0.;
        }

        return cost;
    }

    // -------------------------------------
    // AFK
    // -------------------------------------
    protected boolean isAfk(Player player) {
        if (afkHandler == null) {
            afkHandler = plugin.getInternalServiceManager().getService(AFKHandler.class);
        }

        return afkHandler.isPresent() && afkHandler.get().isAfk(player);
    }

    protected boolean alertOnAfk() {
        try {
            getAfkConfigAdapter();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return false;
        }

        return aca.isPresent() && aca.get().getNodeOrDefault().isAlertSenderOnAfk();
    }

    private void checkAfk(T src, CommandContext args) {
        try {
            // AFK checks can be done async and at the time.
            if (!afkArgs.isEmpty() && alertOnAfk()) {
                afkArgs.forEach(s ->
                        args.getAll(s).stream()
                                .filter(x -> Player.class.isAssignableFrom(x.getClass())).map(x -> (Player) x).filter(this::isAfk)
                                .forEach(p -> sendAfkMessage(src, p)));
            }
        } catch (Exception e) {
            // Swallow!
        }
    }

    private void getAfkConfigAdapter() throws NoModuleException, IncorrectAdapterTypeException {
        if (aca == null) {
            aca = Optional.of(plugin.getModuleContainer().getConfigAdapterForModule("afk", AFKConfigAdapter.class));
        }
    }

    protected void sendAfkMessage(CommandSource src, Player player) {
        try {
            getAfkConfigAdapter();
        } catch (NoModuleException | IncorrectAdapterTypeException e) {
            return;
        }

        aca.ifPresent(a -> {
            String onCommand = a.getNodeOrDefault().getMessages().getOnCommand();
            if (!onCommand.trim().isEmpty()) {
                src.sendMessage(plugin.getChatUtil().getPlayerMessageFromTemplate(onCommand, player, true));
            }
        });
    }

    // -------------------------------------
    // Child Commands
    // -------------------------------------
    private Map<List<String>, CommandCallable> createChildCommands() {
        Set<Class<? extends AbstractCommand<?>>> bases = null;
        if (this.moduleCommands != null) {
            bases = moduleCommands.stream().filter(x -> {
                RegisterCommand r = x.getAnnotation(RegisterCommand.class);
                // Only commands that are subcommands of this.
                return r != null && r.subcommandOf().equals(this.getClass());
            }).collect(Collectors.toSet());
        }

        Map<List<String>, CommandCallable> map = Maps.newHashMap();
        if (bases != null) {
            bases.forEach(cb -> {
                try {
                    builder.buildCommand(cb, false).ifPresent(x -> map.put(Arrays.asList(x.getAliases()), x.getSpec()));
                } catch (Exception e) {
                    plugin.getLogger().error(NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("command.child.notloaded", cb.getName()));

                    if (cca.getNodeOrDefault().isDebugmode()) {
                        e.printStackTrace();
                    }
                }
            });
        }

        map.put(Lists.newArrayList("?", "help"), usageCommand);
        return map;
    }

    public void setModuleName(String id, String module) {
        if (this.module == null) {
            this.moduleId = id;
            this.module = module;
        }
    }

    protected enum ContinueMode {
        /**
         * Continue executing the command.
         */
        CONTINUE(true, null),

        /**
         * Stop executing, but mark as success.
         */
        STOP_SUCCESS(false, CommandResult.success()),

        /**
         * Stop executing, mark as empty.
         */
        STOP(false, CommandResult.empty());

        final boolean cont;
        final CommandResult returnType;

        ContinueMode(boolean cont, CommandResult returnType) {
            this.cont = cont;
            this.returnType = returnType;
        }
    }

    private class UsageCommand implements CommandCallable {

        @Override
        @NonnullByDefault
        public CommandResult process(CommandSource source, String arguments) throws CommandException {
            if (!testPermission(source)) {
                source.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.nopermission"));
                return CommandResult.empty();
            }

            Nucleus plugin = Nucleus.getNucleus();
            AbstractCommand<?> parent = AbstractCommand.this;

            String command = getCommandPath().replaceAll("\\.", " ");

            // Header
            Text header = plugin.getMessageProvider().getTextMessageWithFormat("command.usage.header", command);

            List<Text> textMessages = Lists.newArrayList();

            if (parent.sourceType == Player.class) {
                textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.playeronly"));
            }

            if (parent.getClass().isAnnotationPresent(RequireMixinPlugin.class)) {
                textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.mixin"));
            }

            textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.module", module, moduleId));

            String desc = getDescription();
            if (!desc.isEmpty()) {
                textMessages.add(Text.EMPTY);
                textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.summary"));
                textMessages.add(Text.of(desc));
            }

            String ext = getExtendedDescription();
            if (!ext.isEmpty()) {
                textMessages.add(Text.EMPTY);
                textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.description"));
                String[] split = ext.split("(\\r|\\n|\\r\\n)");
                for (String s : split) {
                    textMessages.add(Text.of(s));
                }
            }

            textMessages.add(Text.EMPTY);
            textMessages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.usage.usage"));
            textMessages.add(Text.of(TextColors.WHITE, AbstractCommand.this.getUsage(source)));

            PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
            PaginationList.Builder builder = ps.builder().title(header).contents(textMessages);
            if (!(source instanceof Player)) {
                builder.linesPerPage(-1);
            }

            builder.sendTo(source);
            return CommandResult.success();
        }

        @Override
        @NonnullByDefault
        public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException {
            return Lists.newArrayList();
        }

        @Override
        @NonnullByDefault
        public boolean testPermission(CommandSource source) {
            return AbstractCommand.this.permissions.testBase(source);
        }

        @Override
        @NonnullByDefault
        public Optional<Text> getShortDescription(CommandSource source) {
            return Optional.empty();
        }

        @Override
        @NonnullByDefault
        public Optional<Text> getHelp(CommandSource source) {
            return Optional.empty();
        }

        @Override
        @NonnullByDefault
        public Text getUsage(CommandSource source) {
            return Text.EMPTY;
        }
    }
}
