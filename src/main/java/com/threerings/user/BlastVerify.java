//
// $Id$

package com.threerings.user;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.StringTokenizer;

import com.samskivert.net.HttpPostUtil;
import com.samskivert.util.Logger;
import com.samskivert.util.ServiceWaiter;
import com.samskivert.util.StringUtil;

/**
 * Utility methods to verify that a user is actually a Shockwave Gameblast
 * member.
 *
 * See GameBlastRemoteAPI.htm in the docs.
 */
public class BlastVerify
{
    /** Return status code indicating that the account verified. */
    public static final byte VERIFIED = 0;

    /** Return status code indicating invalid account. */
    public static final byte INVALID_ACCOUNT = 1;

    /** Return status code indicating an expired account. */
    public static final byte EXPIRED = 2;

    /** Return status code indicating that there were technical difficulties
     * and the blastiness of the user cannot be determined at this time. */
    public static final byte RETRY = 3;

    /** Returns status code indicating invalid account. */
    public static final byte INVALID_PASSWORD = 4;

    /**
     * Verify the username/password to ensure that it's an active shockwave
     * blast user.
     *
     * @return true if the account is in good standing, false if it doesn
     */
    public static byte verifyBlastUser (String username, String password)
    {
        String request = "member_name=" + StringUtil.encode(username) +
                "&password=" + StringUtil.encode(password);

        String response;
        try {
            response = HttpPostUtil.httpPost(LOGIN_URL, request, TIMEOUT);

        } catch (ServiceWaiter.TimeoutException te) {
            return RETRY;

        } catch (IOException ioe) {
            log.warning("Error communicating with blast", ioe);
            return RETRY;
        }

        // we just do a very simplistic parsing of the response
        StringTokenizer st = new StringTokenizer(response);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.startsWith("Error_Code")) {
                StringTokenizer st2 = new StringTokenizer(s, " =\"");
                if (st2.countTokens() == 2) {
                    st2.nextToken();
                    String code = st2.nextToken();
                    try {
                        int result = Integer.parseInt(code);
                        switch (result) {
                        case 1: return VERIFIED;

                        case 2: return INVALID_PASSWORD;

                        case 3: return EXPIRED;

                        case 4: return INVALID_ACCOUNT;

                        default: return RETRY;
                        }

                    } catch (NumberFormatException nfe) {
                        // handled below
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        log.warning("Unable to parse response from blast [resp=\"" + response + "\"].");
        return RETRY;
    }

    /** How long we wait while trying to verify. */
    protected static final int TIMEOUT = 30;

    /** The URL for username/password verification. */
    protected static URL LOGIN_URL;

    /** Used for logging. */
    protected static final Logger log = Logger.getLogger(BlastVerify.class);

    static {
        try {
            LOGIN_URL = new URL(
                // "http://gameblastbeta.shockwave.com" +   // testing URL
                "https://transactor.shockwave.com" +         // real URL
                "/servlet/LoginRemote");

        } catch (MalformedURLException mue) {
            log.warning("Bad URL specification", mue);
        }
    }
}
