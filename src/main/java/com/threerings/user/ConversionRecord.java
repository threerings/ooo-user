//
// $Id$

package com.threerings.user;

import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

/**
 * Tracks conversion related actions taken by our users.
 */
public class ConversionRecord
{
    /** Indicates that an account was created. */
    public static final int CREATED = 1;

    /** Indicates that an account that was not currently paying us started
     * to.  This won't be true before 2005/03/01 where we tried (more or
     * less) to save everytime someone gave us money. */
    public static final int SUBSCRIPTION_START = 2;

    /** Indicates that a subscription was canceled or lapsed from use. */
    public static final int SUBSCRIPTION_ENDED = 3;

    /** The unique identifier of the user that took an action. */
    public int userId;

    /**
     * The partner with whom the user is associated.  You should always
     * use the accessor method to access this value in order to correctly
     * deal with personal referrals.
     */
    public int siteId;

    /** The identifier indicating the action taken. */
    public short action;

    /** The date on which the action was taken. */
    public Timestamp recorded;

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
}
