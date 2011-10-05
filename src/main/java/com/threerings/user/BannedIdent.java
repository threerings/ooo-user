//
// $Id$

package com.threerings.user;

/**
 * Represents a row in the BANNED_IDENTS table.
 */
public class BannedIdent
{
    /** A 'unique' id for a specific machine we have seen. */
    public String machIdent;

    /** The site id which this machine is banned from. */
    public int siteId;

    @Override
    public String toString ()
    {
        return "(" + siteId + ") " + machIdent;
    }
}
