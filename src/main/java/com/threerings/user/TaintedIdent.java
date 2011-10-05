//
// $Id$

package com.threerings.user;

/**
 * Represents a row in the TAINTED_IDENTS table.
 */
public class TaintedIdent
{
    /** A 'unique' id for a specific machine we have seen. */
    public String machIdent;

    /** Blank constructor for the unserialization buisness. */
    public TaintedIdent ()
    {
    }

    @Override
    public String toString ()
    {
        return machIdent;
    }
}
