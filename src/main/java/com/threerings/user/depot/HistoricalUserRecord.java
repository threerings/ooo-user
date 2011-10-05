//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Retains information about a historical user registration.
 */
@Entity(name="history")
public class HistoricalUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<HistoricalUserRecord> _R = HistoricalUserRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> USERNAME = colexp(_R, "username");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The user's assigned integer userid. */
    @Id
    public int userId;

    /** The user's chosen username. */
    @Column
    public String username;

    /** The date this record was created. */
    @Index
    public Date created;

    /** The affiliate site with which this user is associated. */
    @Index
    public int siteId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link HistoricalUserRecord}
     * with the supplied key values.
     */
    public static Key<HistoricalUserRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
