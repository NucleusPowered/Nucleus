/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.docgen;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public class CommandDoc {

    @Setting
    private String commandName;

    @Setting
    private String aliases;

    @Setting
    private String rootAliases;

    @Setting
    private String defaultLevel;

    @Setting
    private String usageString;

    @Setting
    private String oneLineDescription;

    @Setting
    private String extendedDescription;

    @Setting
    private String module;

    @Setting
    private String permissionbase;

    @Setting
    private boolean warmup;

    @Setting
    private boolean cooldown;

    @Setting
    private boolean cost;

    @Setting
    private List<String> essentialsEquivalents;

    @Setting
    private Boolean isExactEssEquiv = null;

    @Setting
    private String essNotes;

    @Setting
    private List<PermissionDoc> permissions;

    @Setting private String simpleUsage;

    @Setting private String subcommands;

    @Setting private String context;

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getAliases() {
        return this.aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public void setDefaultLevel(String defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    public void setUsageString(String usageString) {
        this.usageString = usageString;
    }

    public void setOneLineDescription(String oneLineDescription) {
        this.oneLineDescription = oneLineDescription;
    }

    public void setExtendedDescription(String extendedDescription) {
        this.extendedDescription = extendedDescription;
    }

    public String getModule() {
        return this.module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setPermissionbase(String permissionbase) {
        this.permissionbase = permissionbase.replaceAll("\\.base", "");
    }

    public boolean isWarmup() {
        return this.warmup;
    }

    public void setWarmup(boolean warmup) {
        this.warmup = warmup;
    }

    public boolean isCooldown() {
        return this.cooldown;
    }

    public void setCooldown(boolean cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isCost() {
        return this.cost;
    }

    public void setCost(boolean cost) {
        this.cost = cost;
    }

    public List<PermissionDoc> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(List<PermissionDoc> permissions) {
        this.permissions = permissions;
    }

    public void setRootAliases(String rootAliases) {
        this.rootAliases = rootAliases;
    }

    public String getSubcommands() {
        return this.subcommands;
    }

    public void setSubcommands(String subcommands) {
        this.subcommands = subcommands;
    }

    public void setEssentialsEquivalents(List<String> essentialsEquivalents) {
        this.essentialsEquivalents = essentialsEquivalents;
    }

    public void setExactEssEquiv(Boolean exactEssEquiv) {
        this.isExactEssEquiv = exactEssEquiv;
    }

    public void setEssNotes(String essNotes) {
        this.essNotes = essNotes;
    }

    public void setSimpleUsage(String simpleUsage) {
        this.simpleUsage = simpleUsage;
    }

    public String getContext() {
        return this.context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
