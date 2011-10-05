//
// $Id$

package com.threerings.user.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.net.MailUtil;
import com.samskivert.servlet.user.User;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Config;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserRepository;

/**
 * A script for activating (or deactivating) the tester flag on users.
 */
public class ActivateTesters
{
    public static void main (String[] args)
    {
        if (args.length == 0 ||
            !(args[0].equals("activate") || args[0].equals("clear")) ||
            (args[0].equals("activate") && args.length < 3)) {
            System.err.println(USAGE);
            System.exit(255);
        }

        Config config = new Config("threerings");
        try {
            OOOUserRepository urepo = new OOOUserRepository(
                new StaticConnectionProvider(config.getSubProperties("db")));

            if (args[0].equals("clear")) {
                clearTesters(urepo);
            } else if (args[0].equals("activate")) {
                activateTesters(urepo, Integer.parseInt(args[1]), args[2]);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected static void activateTesters (
        OOOUserRepository urepo, int count, String emailFile)
        throws Exception
    {
        // load up the emails and randomize them
        BufferedReader bin = new BufferedReader(new FileReader(emailFile));
        List<String> elist = Lists.newArrayList();
        String email;
        int rejected = 0;
        while ((email = bin.readLine()) != null) {
            if (!MailUtil.isValidAddress(email)) {
                // System.err.println("Rejecting invalid address: " + email);
                rejected++;
                continue;
            }
            elist.add(email);
        }
        if (rejected > 0) {
            System.err.println("Rejected " + rejected +
                               " invalid addresses, accepted " +
                               elist.size() + " valid addresses.");
        }
        String[] emails = elist.toArray(new String[elist.size()]);
        ArrayUtil.shuffle(emails);

        int activated = 0, offset = 0;
        while (activated < count && offset < emails.length) {
            // grab the next N addresses from the list and see if any of them
            // have an account that we can activate as a tester
            StringBuilder where = new StringBuilder();
            for (int ii = offset, ll = Math.min(emails.length, offset+CHUNK);
                 ii < ll; ii++) {
                if (where.length() > 0) {
                    where.append(", ");
                }
                where.append("'").append(emails[ii]).append("'");
            }
            List<User> users = urepo.lookupUsersWhere("email in (" + where + ")");

            // filter out users that have multiple accounts; cheeky bastards
            HashSet<String> seen = Sets.newHashSet(), filter = Sets.newHashSet();
            for (User ruser : users) {
                OOOUser user = (OOOUser)ruser;
                if (seen.contains(user.email)) {
                    filter.add(user.email);
                } else {
                    seen.add(user.email);
                }
            }
            for (Iterator<User> iter = users.iterator(); iter.hasNext(); ) {
                OOOUser user = (OOOUser)iter.next();
                if (filter.contains(user.email)) {
                    iter.remove();
                }
            }

            // now mark anyone that made it through the guantlet as a tester
            for (Iterator<User> iter = users.iterator(); iter.hasNext(); ) {
                OOOUser user = (OOOUser)iter.next();
                if (user.holdsToken(OOOUser.TESTER)) {
                    continue;
                }
                user.addToken(OOOUser.TESTER);
                System.out.println(user.username + " " + user.email);
                urepo.updateUser(user);
                activated++;
            }
            offset += CHUNK;
        }

        System.err.println("Activated " + activated + " accounts.");
    }

    protected static void clearTesters (OOOUserRepository urepo)
        throws Exception
    {
        List<User> users = urepo.lookupUsersWhere("HEX(tokens) like '%04%'");
        System.err.println("Clearing " + users.size() + " testers.");
        for (int ii = 0, ll = users.size(); ii < ll; ii++) {
            OOOUser user = (OOOUser)users.get(ii);
            user.removeToken(OOOUser.TESTER);
            urepo.updateUser(user);
        }
    }

    protected static final String USAGE =
        "Usage: ActivateTesters [activate count emails.txt | clear]";

    protected static final int CHUNK = 25;
}
