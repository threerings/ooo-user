//
// $Id$

package com.threerings.user;

import java.sql.Date;

/**
 * Used to load detailed information about a user from the OOO user
 * database tables.
 */
public class DetailedUser
{
    /** The user's assigned integer userid. */
    public int userId;

    /** The user's chosen username. */
    public String username;

    /** The date this record was created. */
    public Date created;

    /** The user's email address. */
    public String email;

    /** The user's birthday. */
    public Date birthday;

    /** The user's gender. */
    public byte gender;

    /** The missive provided by the user during registration. */
    public String missive;
}
