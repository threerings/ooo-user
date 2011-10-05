//
// $Id$

package com.threerings.user;

import java.sql.Date;

/**
 * Records information on a particular referral.
 */
public class ReferralRecord
{
    /** A unique identifier for this referral record. */
    public int referralId;

    /** The date on which the referral was made (used to expire old
     * unutilized referrals). */
    public Date recorded;

    /** The OOO user id of the referring user. */
    public int referrerId;

    /** Data associated with this referral record. */
    public String data;
}
