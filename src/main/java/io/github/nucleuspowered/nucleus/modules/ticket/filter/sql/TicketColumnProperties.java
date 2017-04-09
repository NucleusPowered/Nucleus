/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.filter.sql;

import java.sql.SQLType;

/**
 * A set of properties possessed by a database column.
 */
public class TicketColumnProperties {
    private final String name;
    private final SQLType sqlType;
    private final Class javaType;

    public TicketColumnProperties(String name, SQLType sqlType, Class javaType) {
        this.name = name;
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    /**
     * The name of the column in the database table.
     *
     * @return The column name.
     */
    public String getColumnName() {
        return name;
    }

    /**
     * The {@link SQLType} of the column in the database.
     * @return the sql type.
     */
    public SQLType getSqlType() {
        return sqlType;
    }

    /**
     * The corresponding class to this columns {@link SQLType}.
     * @return The java type.
     */
    public Class getJavaType() {
        return javaType;
    }
}