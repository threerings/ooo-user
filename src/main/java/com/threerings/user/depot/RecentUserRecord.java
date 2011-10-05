//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

/**
 * Used to summarize recent registrants from the {@link HistoricalUserRecord} table.
 */
@Computed(shadowOf=HistoricalUserRecord.class)
public class RecentUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RecentUserRecord> _R = RecentUserRecord.class;
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Integer> ENTRIES = colexp(_R, "entries");
    // AUTO-GENERATED: FIELDS END

    /** The created date. */
    public Date created;

    /** The number of users created on the date. */
    @Computed(fieldDefinition="count(*)")
    public int entries;
}
