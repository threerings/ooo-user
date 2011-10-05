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
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * A reward being offered.
 */
@Entity(name="REWARD_INFO")
public class RewardInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RewardInfoRecord> _R = RewardInfoRecord.class;
    public static final ColumnExp<Integer> REWARD_ID = colexp(_R, "rewardId");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<String> COUPON_CODE = colexp(_R, "couponCode");
    public static final ColumnExp<String> DATA = colexp(_R, "data");
    public static final ColumnExp<Date> EXPIRATION = colexp(_R, "expiration");
    public static final ColumnExp<Integer> MAX_ELIGIBLE_ID = colexp(_R, "maxEligibleId");
    public static final ColumnExp<Integer> ACTIVATIONS = colexp(_R, "activations");
    public static final ColumnExp<Integer> REDEMPTIONS = colexp(_R, "redemptions");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** Unique reward id. */
    @Id @Column(name="REWARD_ID") @GeneratedValue(strategy=GenerationType.AUTO)
    public int rewardId;

    /** Reward description. */
    @Column(name="DESCRIPTION")
    public String description;

    /** A coupon code required to activate this reward or null. */
    @Column(name="COUPON_CODE", nullable=true)
    public String couponCode;

    /** Game specific reward data. */
    @Column(name="DATA")
    public String data;

    /** Date when the reward expires. */
    @Column(name="EXPIRATION")
    public Date expiration;

    /** Max userid that can redeem this reward. */
    @Column(name="MAX_ELIGIBLE_ID")
    public int maxEligibleId;

    /** Number of activated rewards. */
    @Column(name="ACTIVATIONS")
    public int activations;

    /** Number of claimed rewards. */
    @Column(name="REDEMPTIONS")
    public int redemptions;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RewardInfoRecord}
     * with the supplied key values.
     */
    public static Key<RewardInfoRecord> getKey (int rewardId)
    {
        return newKey(_R, rewardId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(REWARD_ID); }
    // AUTO-GENERATED: METHODS END

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
