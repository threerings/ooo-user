//
// $Id$

package com.threerings.user.tools;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.Username;
import com.samskivert.util.Config;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserRepository;

/**
 * Provides command line inspection and processing of account actions.
 */
public class UserTool
{
    public static final String PROPS_NAME = System.getProperty("ooo.propfile", "threerings");

    public static void main (String[] args)
    {
        if (args.length == 0) {
            failWithUsage();
        }

        Config config = new Config(PROPS_NAME);
        try {
            OOOUserRepository repo = new OOOUserRepository(
                new StaticConnectionProvider(config.getSubProperties("db")));

            switch (args[0]) {
            case "prune":
                repo.pruneUsers(PRUNE_DAYS);
                break;

            case "create":
                createUser(repo, args);
                break;

            case "make_admin":
                makeAdmin(repo, args);
                break;

            default:
                System.err.println("Unknown command: " + args[0]);
                failWithUsage();
                break;
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected static void failWithUsage () {
        for (String usage : USAGE) {
            System.err.println(usage);
        }
        System.exit(255);
    }

    protected static void createUser(OOOUserRepository repo, String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Invalid 'create' args. Usage:");
            System.err.println(USAGE[2]);
            System.exit(255);
        }
        Username username = new Username(args[1]);
        Password password = Password.makeFromClear(args[2]);
        int id = repo.createUser(username, password, args[3], OOOUser.DEFAULT_SITE_ID, 0);
        System.out.println("Created user '" + username + "' with id " + id);
    }

    protected static void makeAdmin(OOOUserRepository repo, String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Invalid 'make_admin' args. Usage:");
            System.err.println(USAGE[3]);
            System.exit(255);
        }

        String username = args[1];
        OOOUser user = repo.loadUser(username, false);
        if (user.holdsToken(OOOUser.ADMIN)) {
            System.err.println("User " + username + " (id: " + user.userId + ") already admin.");
            System.exit(255);
        }
        user.addToken(OOOUser.ADMIN);
        repo.updateUser(user);
        System.out.println("Marked user " + username + " (id: " + user.userId + ") as admin.");
    }

    protected static final String[] USAGE = {
        "Usage: UserTool [prune|create]",
        "  prune - prunes expired users",
        "  create username email password - create a user account",
        "  make_admin username - marks a user account as admin"
    };

    /** Users that have been purged from all other games for 30 days are toast. */
    protected static final int PRUNE_DAYS = 30;
}
