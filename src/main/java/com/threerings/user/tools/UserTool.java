//
// $Id$

package com.threerings.user.tools;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.Config;

import com.threerings.user.OOOUserRepository;

/**
 * Provides command line inspection and processing of account actions.
 */
public class UserTool
{
    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println(USAGE);
            System.exit(255);
        }

        Config config = new Config("threerings");
        try {
            OOOUserRepository repo = new OOOUserRepository(
                new StaticConnectionProvider(config.getSubProperties("db")));

            if (args[0].equals("prune")) {
                repo.pruneUsers(PRUNE_DAYS);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected static final String USAGE = "Usage: UserTool [prune]";

    /** Users that have been purged from all other games for 30 days are toast. */
    protected static final int PRUNE_DAYS = 30;
}
