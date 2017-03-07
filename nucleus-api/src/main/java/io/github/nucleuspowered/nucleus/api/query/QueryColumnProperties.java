/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.query;

import java.sql.SQLType;

/**
 * A set of properties possessed by a database column.
 */
public interface QueryColumnProperties {

    /**
     * The name of the column in the database table..
     *
     * @return The column name.
     */
    String getColumnName();

    /**
     * The {@link SQLType} of the column in the database.
     * @return the sql type.
     */
    SQLType getSqlType();

    /**
     * The corresponding class to this columns {@link SQLType}.
     * @return The java type.
     */
    Class getJavaType();
}
