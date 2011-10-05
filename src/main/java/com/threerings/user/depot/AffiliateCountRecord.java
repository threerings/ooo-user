//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.ColumnExp;

/**
 * The number of users referred by an affiliate created on a particular day.
 */
@Computed(shadowOf=HistoricalUserRecord.class)
public class AffiliateCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AffiliateCountRecord> _R = AffiliateCountRecord.class;
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The day that these users were created */
    public Date created;

    /** The number of users. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
