//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.ColumnExp;

@Computed(shadowOf=HistoricalUserRecord.class)
public class AffiliateUserCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AffiliateUserCountRecord> _R = AffiliateUserCountRecord.class;
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The affiliate site ID. */
    public int siteId;

    /** The number of users. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
