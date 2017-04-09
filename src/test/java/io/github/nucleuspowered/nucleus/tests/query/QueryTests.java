/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.query;

import io.github.nucleuspowered.nucleus.api.filter.FilterComparator;
import io.github.nucleuspowered.nucleus.api.filter.NucleusTicketFilter;
import io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketQuery;
import io.github.nucleuspowered.nucleus.modules.ticket.filter.sql.TicketQueryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

public class QueryTests {

    @Test
    public void ticketQueryTest() {
        Instant now = Instant.now();
        Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS);
        Set<NucleusTicketFilter> filters = new TicketQueryBuilder()
                .filter(NucleusTicketFilter.Property.ID, FilterComparator.EQUALS, 8)
                .filter(NucleusTicketFilter.Property.CREATION_DATE, FilterComparator.BETWEEN, lastWeek, now)
                .build(); //Filter that gets all tickets with ID 8 created in the last week. (Just a test, I know it'll only ever match for one ticket at most as the ID is unique).

        TicketQuery query = TicketQuery.fromFilters(filters);

        String queryString = query.createPlainQueryString();
        Map<Integer, Object> parameters = query.createParameterReplacementList();

        Assert.assertTrue(StringUtils.countMatches(queryString, "?") == 3); //Checks there are 3 parameters in need of replacement in the filter string.
        Assert.assertTrue(parameters.size() == 3); //Checks there are 3 replacement parameters.
        Assert.assertTrue(((int) parameters.get(1)) == 8 && parameters.get(2).equals(lastWeek) && parameters.get(3).equals(now)); //Checks the replacement parameters are in the correct order
    }
}
