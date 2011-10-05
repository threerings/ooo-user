//
// $Id$

package com.threerings.user;

import java.sql.Date;

/**
 * Maintains information on users that have not yet validated their
 * account.
 */
public class ValidateRecord
{
    public String secret;

    public int userId;

    public boolean persist;

    public Date inserted;
}
