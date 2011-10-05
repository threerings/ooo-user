//
// $Id$

package com.threerings.user;

import java.sql.Date;

import com.samskivert.util.StringUtil;

/**
 * An object representation of the persistent reward info stored in the {@link RewardRepository}
 * REWARD_INFO table.
 */
public class RewardInfo
{
    /** Unique reward id. */
    public int rewardId;

    /** Reward description. */
    public String description;

    /** A coupon code required to activate this reward or null. */
    public String couponCode;

    /** Game specific reward data. */
    public String data;

    /** Date when the reward expires. */
    public Date expiration;

    /** Max userid that can redeem this reward. */
    public int maxEligibleId;

    /** Number of activated rewards. */
    public int activations;

    /** Number of claimed rewards. */
    public int redemptions;

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
