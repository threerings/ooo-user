//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

/**
 * Used to summarize rewards.
 */
@Computed(shadowOf=RewardInfoRecord.class)
public class RewardCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RewardCountRecord> _R = RewardCountRecord.class;
    public static final ColumnExp<Integer> REWARD_ID = colexp(_R, "rewardId");
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The reward id. */
    public int rewardId;

    /** The number of matching records. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
