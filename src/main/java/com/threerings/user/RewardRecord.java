//
// $Id$

package com.threerings.user;

import com.samskivert.util.StringUtil;

/**
 * An object representation of the persistent rewards stored in the {@link RewardRepository} REWARDS
 * table.
 */
public class RewardRecord
{
    /** RewardInfo reward id. */
    public int rewardId;

    /** Account name. */
    public String account;

    /** Unique identification value. */
    public String redeemerIdent;

    /**
     * A generic parameter string that can tell the reward handler more specifically when and what
     * item, etc. to hand out for this instance of the reward.
     */
    public String param;

    /**
     * Constructs a blank RewardRecord for unserialization from the repository.
     */
    public RewardRecord ()
    {
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
