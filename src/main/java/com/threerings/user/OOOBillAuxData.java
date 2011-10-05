//
// $Id$

package com.threerings.user;

import java.sql.Timestamp;

/**
 * Contains additional information on users that have paid us in one form or
 * another.
 */
public class OOOBillAuxData
{
    /** The user's unique identifier. */
    public int userId;
    
    /** The first time the user bought coins **/
    public Timestamp firstCoinBuy;

    /** The most recent time the user bought coins **/
    public Timestamp latestCoinBuy;
}
