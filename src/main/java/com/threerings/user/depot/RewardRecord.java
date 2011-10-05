//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * A persistent reward.
 */
@Entity(name="REWARD_RECORDS")
public class RewardRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RewardRecord> _R = RewardRecord.class;
    public static final ColumnExp<Integer> REWARD_ID = colexp(_R, "rewardId");
    public static final ColumnExp<String> ACCOUNT = colexp(_R, "account");
    public static final ColumnExp<String> REDEEMER_IDENT = colexp(_R, "redeemerIdent");
    public static final ColumnExp<String> PARAM = colexp(_R, "param");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** RewardInfo reward id. */
    @Id @Column(name="REWARD_ID")
    public int rewardId;

    /** Account name. */
    @Id @Index(name="ixAccount") @Column(name="ACCOUNT", nullable=true)
    public String account;

    /** Unique identification value. */
    @Index(name="ixRedeemerIdent") @Column(name="REDEEMER_IDENT", length=64, nullable=true)
    public String redeemerIdent;

    /**
     * A generic parameter string that can tell the reward handler more specifically when and what
     * item, etc. to hand out for this instance of the reward.
     */
    @Id @Column(name="PARAM", nullable=true)
    public String param;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RewardRecord}
     * with the supplied key values.
     */
    public static Key<RewardRecord> getKey (int rewardId, String account, String param)
    {
        return newKey(_R, rewardId, account, param);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(REWARD_ID, ACCOUNT, PARAM); }
    // AUTO-GENERATED: METHODS END
}
