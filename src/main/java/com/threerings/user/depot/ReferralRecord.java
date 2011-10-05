//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records information on a particular referral.
 */
@Entity(name="REFERRAL")
public class ReferralRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ReferralRecord> _R = ReferralRecord.class;
    public static final ColumnExp<Integer> REFERRAL_ID = colexp(_R, "referralId");
    public static final ColumnExp<Date> RECORDED = colexp(_R, "recorded");
    public static final ColumnExp<Integer> REFERRER_ID = colexp(_R, "referrerId");
    public static final ColumnExp<String> DATA = colexp(_R, "data");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The refferal's assigned integer id. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO) @Column(name="REFERRAL_ID")
    public int referralId;

    /** The date the referral was made. */
    @Column(name="RECORDED", defaultValue="'1900-01-01'") @Index
    public Date recorded;

    /** The user id of the referring user. */
    @Column(name="REFERRER_ID", defaultValue="0") @Index
    public int referrerId;

    /** Data associated with this referral record. */
    @Column(name="DATA")
    public String data;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ReferralRecord}
     * with the supplied key values.
     */
    public static Key<ReferralRecord> getKey (int referralId)
    {
        return newKey(_R, referralId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(REFERRAL_ID); }
    // AUTO-GENERATED: METHODS END
}
