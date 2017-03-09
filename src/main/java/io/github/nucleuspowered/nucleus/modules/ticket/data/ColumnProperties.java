/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.data;

import io.github.nucleuspowered.nucleus.api.query.QueryColumnProperties;

import java.sql.SQLType;

public class ColumnProperties implements QueryColumnProperties {
    private final String name;
    private final SQLType sqlType;
    private final Class javaType;

    public ColumnProperties(String name, SQLType sqlType, Class javaType) {
        this.name = name;
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    @Override
    public String getColumnName() {
        return name;
    }

    @Override
    public SQLType getSqlType() {
        return sqlType;
    }

    @Override
    public Class getJavaType() {
        return javaType;
    }
}
