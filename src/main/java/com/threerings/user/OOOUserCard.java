//
// $Id$

package com.threerings.user;

import com.google.common.base.Function;

/**
 * Identifying information and flags for a {@link OOOUser} object. This is for use when querying
 * a potentially large number of users meeting some criteria and only each user's name and flags
 * are required.
 */
public class OOOUserCard
{
    /** Converts a card to a username. */
    public static Function<OOOUserCard, String> TO_USERNAME = new Function<OOOUserCard, String>() {
        public String apply (OOOUserCard card) {
            return card.username;
        }
    };

    /**
     * Creates a new user card with the given field values.
     */
    public OOOUserCard (int userid, String username, int flags)
    {
        this.userid = userid;
        this.username = username;
        this.flags = flags;
    }

    /**
     * Creates a new user card for deserialization.
     */
    public OOOUserCard ()
    {
    }

    /** The unique id of the user. */
    public int userid;

    /** The name of the user. */
    public String username;

    /** The {@link OOOUser#flags} of the user. */
    public int flags;
}
