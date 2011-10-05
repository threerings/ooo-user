//
// $Id$

package com.threerings.user;

import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

/**
 * Representation of a AccountAction record from the database.
 */
public class AccountAction
{
    /** An action constant indicating the first time a user becomes a
     * subscriber, and have also never before bought coins or time. */
    public static final int INITIAL_SUBSCRIPTION = 0;

    /** An action constant indicating that some server has modified the
     * coins count for a particular user. */
    public static final int COINS_UPDATED = 1;

    /** An action constant indicating that the user, whom at some point in
     *  the past used to be a subscriber, has become a subscriber again. */
    public static final int REPEAT_SUBSCRIPTION = 2;

    /** An action constant indicating that the user purchased coins for the
     * first time ever, and have never before subscribed or bought time. */
    public static final int INITIAL_COIN_PURCHASE = 3;

    /** An action constant indicating that the specified account has
     * expired and any player resources should be expired. The 'data'
     * field is currently filled in with the yohoho accountId for the
     * expired account. */
    public static final int ACCOUNT_EXPIRED = 4;

    /** An action constant indicating that the specified account has been
     * deleted in the external account system. The 'data' field is
     * optionally filled in with the "disabled" username which the server
     * can use to simply disable the account rather than deleting it. */
    public static final int ACCOUNT_DELETED = 5;

    /** An action unconnected with any single account, but rather a system-wide
     * signal to all servers that they should do whatever "daily" actions
     * they would like to do at the present time. */
    public static final int DO_DAILY_ACTIONS = 6;

    /** An action constant indicating that the user purchased time for the
     * first time ever, and has never before subscribed or bought coins. */
    public static final int INITIAL_TIME_PURCHASE = 7;

    /** Indicates that a reward has been activated for the account. */
    public static final int REWARD_ACTIVATED = 8;

    /** Placeholder for application-defined action constants. */
    public static final int FIRST_APPLICATION_ACTION = 100;

    /** Unique action identifier. */
    public int actionId;

    /** The unique identifier for the account. */
    public String accountName;

    /** The action that took place. */
    public int action;

    /** Data that is interpreted depending on the action type. */
    public String data;

    /** When the action took place. */
    public Timestamp entered;

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
