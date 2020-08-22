/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.docgen;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Objects;

@ConfigSerializable
public class PermissionDoc {

    public PermissionDoc() { }

    public PermissionDoc(String permission, String description, String defaultLevel, String module) {
        this.permission = permission;
        this.description = description;
        this.defaultLevel = defaultLevel;
        this.module = module;
    }

    @Setting
    private String permission;

    @Setting
    private String description;

    @Setting
    private String defaultLevel;

    @Setting
    private String module;

    public String getPermission() {
        return this.permission;
    }

    public PermissionDoc setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public PermissionDoc setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getModule() {
        return this.module;
    }

    public PermissionDoc setModule(String module) {
        this.module = module;
        return this;
    }

    public String getDefaultLevel() {
        return this.defaultLevel;
    }

    public PermissionDoc setDefaultLevel(String defaultLevel) {
        this.defaultLevel = defaultLevel;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PermissionDoc that = (PermissionDoc) o;
        return Objects.equals(this.permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.permission);
    }

}
