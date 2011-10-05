//
// $Id$

package com.threerings.user;

import java.sql.Date;

import com.samskivert.util.StringUtil;

/**
 * Auxiliary information relating to our registered users.
 */
public class OOOAuxData
{
    /** The user's unique identifier. */
    public int userId;

    /** The user's birthday. */
    public Date birthday;

    /** The user's gender. */
    public byte gender;

    /** The user's personal missive to us. */
    public String missive;

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
