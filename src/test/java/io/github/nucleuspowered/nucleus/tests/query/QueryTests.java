/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.query;

import io.github.nucleuspowered.nucleus.api.query.QueryComparator;
import io.github.nucleuspowered.nucleus.api.query.NucleusTicketQuery;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class QueryTests {

    @Test
    public void ticketQueryTest() {
        Instant now = Instant.now();
        Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS);
        NucleusTicketQuery query = NucleusTicketQuery.builder()
                .filter(NucleusTicketQuery.Column.ID, QueryComparator.EQUALS, 8)
                .filter(NucleusTicketQuery.Column.CREATION_DATE, QueryComparator.BETWEEN, lastWeek, now)
                .build(); //Filter that gets all tickets with ID 8 created in the last week. (Just a test, I know it'll only ever match for one ticket at most as the ID is unique).

        String queryString = query.createPlainQueryString();
        Map<Integer, Object> parameters = query.createParameterReplacementList();

        Assert.assertTrue(StringUtils.countMatches(queryString, "?") == 3); //Checks there are 3 parameters in need of replacement in the query string.
        Assert.assertTrue(parameters.size() == 3); //Checks there are 3 replacement parameters.
        Assert.assertTrue(((int) parameters.get(1)) == 8 && parameters.get(2).equals(lastWeek) && parameters.get(3).equals(now)); //Checks the replacement parameters are in the correct order
    }
}
