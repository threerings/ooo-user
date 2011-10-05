//
// $Id$

package com.threerings.user;

import java.sql.Date;

/**
 * Retains information about a historical user registration.
 */
public class HistoricalUser
{
    /** The user's assigned integer userid. */
    public int userId;

    /** The user's chosen username. */
    public String username;

    /** The date this record was created. */
    public Date created;

    /** The affiliate site with which this user is associated. */
    public int siteId;
}
