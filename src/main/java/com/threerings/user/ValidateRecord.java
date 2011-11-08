//
// $Id$

package com.threerings.user;

import java.sql.Date;
import java.util.Random;

import com.samskivert.util.StringUtil;

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

    /**
     * Create a new ValidateRecord for the specified userId.
     */
    public static ValidateRecord create (int userId, boolean persist)
    {
        ValidateRecord rec = new ValidateRecord();
        rec.userId = userId;
        rec.persist = persist;
        long now = System.currentTimeMillis();
        long secret;
        do {
            secret = rand.nextLong();
        } while (secret == Long.MIN_VALUE); // MIN_VALUE doesn't "abs" nicely
        // guarantee that secret cannot collide with another user by prepending the userId
        rec.secret = Integer.toString(userId, 16) +
            StringUtil.prepad(Integer.toString((int) (now & 0xFFFF), 16), 4, '0') +
            StringUtil.prepad(Long.toString(Math.abs(secret), 16), 16, '0');
        rec.inserted = new Date(now);
        return rec;
    }

    /** Generates random numbers for validate records. */
    protected static final Random rand = new Random();
}
