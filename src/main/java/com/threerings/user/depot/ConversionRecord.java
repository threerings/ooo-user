//
// $Id$

package com.threerings.user.depot;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.user.OOOUser;

/**
 * Emulates {@link ConversionRecord} for the Depot.
 */
@Entity(name="CONVERSION")
public class ConversionRecord extends PersistentRecord
{
    /** Indicates that an account was created. */
    public static final int CREATED = 1;

    /** Indicates that an account that was not currently paying us started
     * to.  This won't be true before 2005/03/01 where we tried (more or
     * less) to save everytime someone gave us money. */
    public static final int SUBSCRIPTION_START = 2;

    /** Indicates that a subscription was canceled or lapsed from use. */
    public static final int SUBSCRIPTION_ENDED = 3;

    // AUTO-GENERATED: FIELDS START
    public static final Class<ConversionRecord> _R = ConversionRecord.class;
    public static final ColumnExp<Timestamp> RECORDED = colexp(_R, "recorded");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    public static final ColumnExp<Short> ACTION = colexp(_R, "action");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The timestamp on which the action was taken. */
    @Id @Column(name="RECORDED")
    public Timestamp recorded;

    /** The unique identifier of the user that took an action. */
    @Column(name="USER_ID")
    public int userId;

    /**
     * The partner with whom the user is associated.  You should always
     * use the accessor method to access this value in order to correctly
     * deal with personal referrals.
     */
    @Column(name="SITE_ID")
    public int siteId;

    /** The identifier indicating the action taken. */
    @Column(name="ACTION")
    public short action;

    /**
     * Returns the site id associated with this record, properly mapping
     * negative ids (personal referrals) into a single category.
     */
    public int getSiteId ()
    {
        return (siteId <= 0) ? OOOUser.REFERRAL_SITE_ID : siteId;
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    public boolean equals (ConversionRecord cr)
    {
        return (cr.userId == userId && cr.siteId == siteId &&
                cr.action == action && cr.recorded.equals(recorded));
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ConversionRecord}
     * with the supplied key values.
     */
    public static Key<ConversionRecord> getKey (Timestamp recorded)
    {
        return newKey(_R, recorded);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORDED); }
    // AUTO-GENERATED: METHODS END
}
