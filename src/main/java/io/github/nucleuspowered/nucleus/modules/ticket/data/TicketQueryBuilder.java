package io.github.nucleuspowered.nucleus.modules.ticket.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.query.NucleusTicketQuery;
import io.github.nucleuspowered.nucleus.api.query.QueryComparator;

import java.util.HashMap;
import java.util.Map;

public class TicketQueryBuilder implements NucleusTicketQuery.Builder {
    private Map<String, Map<Integer, Object>> queries = Maps.newHashMap(); //The value is a map of the parameter index for the prepared statement and the value to replace.
    private NucleusTicketQuery.Column column;
    private QueryComparator comparator;
    private Map<Integer, Object> values = Maps.newHashMap();

    public TicketQueryBuilder() {
        reset();
    }

    @Override
    public NucleusTicketQuery.Builder column(NucleusTicketQuery.Column column) {
        Preconditions.checkState(TicketQuery.TicketColumnProperties.getColumnProperties(column) != null,
                "The column " + column.toString() + " does not have a valid set of column properties.");

        this.column = column;
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder comparator(QueryComparator comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder value(Object value) {
        Preconditions.checkState(column != null,
                "You must first declare the column you are querying before declaring a value to filter by.");
        Preconditions.checkState(comparator != null,
                "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
        Preconditions.checkState(values.size() < comparator.getExpectedValueCount(),
                "This query already has the maximum number of assigned values.");
        Preconditions.checkState(TicketQuery.TicketColumnProperties.getColumnProperties(column).getJavaType().isAssignableFrom(value.getClass()),
                "The value is not of the expected type, the column " + column.toString() + " expects a query parameter of type " +
                        TicketQuery.TicketColumnProperties.getColumnProperties(column).getJavaType().getSimpleName());

        replaceValue(values.size() + 1, value); //Add one to the size because the parameter index starts at 1, not 0.
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder replaceValue(int valueIndex, Object value) {
        Preconditions.checkState(column != null,
                "You must first declare the column you are querying before declaring a value to filter by.");
        Preconditions.checkState(comparator != null,
                "You must first declare the comparator you are using to filter by before declaring a value to use with the filter.");
        Preconditions.checkState(valueIndex > 0,
                "The value index must be greater than 0.");
        Preconditions.checkState(valueIndex <= comparator.getExpectedValueCount(),
                "The provided value index is greater than the maximum possible number of values.");
        Preconditions.checkState(TicketQuery.TicketColumnProperties.getColumnProperties(column).getJavaType().isAssignableFrom(value.getClass()),
                "The value is not of the expected type, the column " + column.toString() + " expects a query parameter of type " +
                        TicketQuery.TicketColumnProperties.getColumnProperties(column).getJavaType().getSimpleName());

        values.put(valueIndex, value);
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder completeFilter() {
        Preconditions.checkState(column != null,
                "The column to query must be defined.");
        Preconditions.checkState(comparator != null,
                "The comparator to use in the query must be defined.");
        Preconditions.checkState(values.size() == comparator.getExpectedValueCount(),
                "The comparator expects " + comparator.getExpectedValueCount() + " value(s) but " +
                        values.size() + " value(s) were provided.");

        switch (comparator) {
            case BETWEEN:
                queries.put(TicketQuery.TicketColumnProperties.getColumnProperties(column).getColumnName() + " " + comparator.getComparator() + " ? AND ?", ImmutableMap.copyOf(values)); //Since this will be used to create a prepared statement use a token instead of the value.
                break;
            default:
                queries.put(TicketQuery.TicketColumnProperties.getColumnProperties(column).getColumnName() + " " + comparator.getComparator() + " ?", ImmutableMap.copyOf(values)); //Since this will be used to create a prepared statement use a token instead of the value.
                break;
        }

        this.column = null;
        this.comparator = null;
        values.clear();
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder filter(NucleusTicketQuery.Column column, QueryComparator comparator, Object... values) {
        column(column);
        comparator(comparator);
        for (Object value : values) {
            value(value);
        }
        completeFilter();

        return this;
    }

    @Override
    public Map<String, Map<Integer, Object>> getQueries() {
        return queries;
    }

    @Override
    public NucleusTicketQuery build() {
        return new TicketQuery(this);
    }

    @Override
    public NucleusTicketQuery.Builder from(NucleusTicketQuery value) {
        this.queries = new HashMap<>(value.getQueries());
        return this;
    }

    @Override
    public NucleusTicketQuery.Builder reset() {
        queries.clear();
        column = null;
        comparator = null;
        values.clear();
        return this;
    }
}
