//
// $Id$

package com.threerings.user;

/**
 * Represents a row in the USER_IDENT table mapping a user to all their
 * machine identifieres.
 */
public class UserIdent
{
    /** The id of the user in question. */
    public int userId;

    /** A 'unique' id for a specific machine we have seen the user come from. */
    public String machIdent;

    /** Construct a blank record for unserialization purposes. */
    public UserIdent ()
    {
    }

    @Override
    public String toString ()
    {
        return "[id=" + userId + ", ident=" + machIdent + "]";
    }
}
