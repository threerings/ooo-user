//
// $Id$

package com.threerings.user.tools;

import java.util.List;

import com.samskivert.util.Config;

import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.user.AccountAction;
import com.threerings.user.depot.AccountActionRepository;

/**
 * Provides command line inspection and processing of account actions.
 */
public class AccountActionTool
{
    public static void main (String[] args)
    {
        if (args.length == 0) {
            System.err.println(USAGE);
            System.exit(255);
        }

        Config config = new Config("threerings");
        try {
            AccountActionRepository repo = new AccountActionRepository(
                new StaticConnectionProvider(config.getSubProperties("db")));

            if (args[0].equals("list")) {
                while (true) {
                    String server = (args.length > 1) ? args[1] : "";
                    List<AccountAction> actions = repo.getActions(server, 999);
                    for (int ii = 0; ii < actions.size(); ii++) {
                        System.out.println(actions.get(ii));
                    }
                    try { Thread.sleep(1000L); } catch (Exception e) {}
                }

            } else if (args[0].equals("prune")) {
                repo.pruneActions();

            } else if (args[0].equals("add")) {
                if (args.length != 3) {
                    System.err.println(USAGE);
                    System.exit(255);
                }
                repo.addAction(args[1], Integer.parseInt(args[2]));

            } else if (args[0].equals("process")) {
                if (args.length != 3) {
                    System.err.println(USAGE);
                    System.exit(255);
                }
                repo.noteProcessed(Integer.parseInt(args[1]), args[2]);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected static final String USAGE =
        "Usage: AccountActionTool [prune|list|list server|" +
        "add account_name action_type|process action_id server]";
}
