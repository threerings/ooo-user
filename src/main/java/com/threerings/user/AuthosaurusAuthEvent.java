package com.threerings.user;

import java.sql.Timestamp;

/**
 * A single row of the authosaurus <code>auth_log</code> table: one account-creation or login
 * event, recording the IP address used and, where checked, whether that address looked like a
 * VPN/proxy/hosting connection.
 *
 * <p><b>This is a hand-maintained copy of a schema owned by another project.</b> The table is
 * created and written solely by authosaurus (a TypeScript project, so we cannot share code with
 * it); we only ever read from it. The authoritative definitions live in the authosaurus repository
 * in <code>src/db/schema-mysql.ts</code> and <code>src/db/schema-pg.ts</code> (with the migrations
 * that created the table under <code>drizzle/</code>), and the events are written by
 * <code>src/auth-log.ts</code>. Any change to the columns or action values there must be mirrored
 * here and in {@link AuthosaurusAuthEventRepository}.</p>
 */
public class AuthosaurusAuthEvent
{
    /** Action recorded when a create-account link is requested. */
    public static final String CREATE_ACCOUNT_REQUEST = "create-account-request";

    /** Action recorded when a create-account link is followed and the account is created. This is
     * the event that tells us when and from where an account came into being. */
    public static final String CREATE_ACCOUNT_CONFIRM = "create-account-confirm";

    /** Action recorded when a login link is requested. */
    public static final String LOGIN_REQUEST = "login-request";

    /** Action recorded when a login link is followed and a session is established. */
    public static final String LOGIN_CONFIRM = "login-confirm";

    /** A unique identifier for this event. */
    public long id;

    /** The OOO site id (see {@link OOOUser}) of the site on which the event took place. Note that
     * multiple sites can share a user database, so this is not constant within the table. */
    public int siteId;

    /** The email address that requested the action. */
    public String email;

    /** The OOO user id of the account involved, or null if it was not (yet) known. */
    public Integer userId;

    /** The account name involved, or null if it was not (yet) known. Null for all events recorded
     * before authosaurus started logging the account name. */
    public String username;

    /** What took place: one of {@link #CREATE_ACCOUNT_REQUEST}, {@link #CREATE_ACCOUNT_CONFIRM},
     * {@link #LOGIN_REQUEST} or {@link #LOGIN_CONFIRM}. */
    public String action;

    /** The IP address from which the request came. */
    public String ipAddress;

    /** The user agent that made the request, or null if it sent none. */
    public String userAgent;

    /** Whether the IP address was flagged as an anonymizing network, or null if it was not
     * checked (authosaurus skips the lookup when it has no credentials configured). */
    public Boolean vpnDetected;

    /** Comma separated labels for whichever anonymizing traits were flagged (e.g. "vpn,hosting"),
     * or null if nothing was flagged or no check was made. */
    public String network;

    /** MaxMind's likelihood (0-99) that the address is static rather than dynamically assigned,
     * or null if unknown. */
    public Double staticIpScore;

    /** Why the action was refused, if it was, or null if it was not refused for a reason
     * authosaurus categorizes (currently "anonymizing_network" or "disposable_email"). */
    public String blockReason;

    /** Whether the action succeeded. */
    public boolean success;

    /** When the event took place. */
    public Timestamp createdAt;

    @Override
    public String toString ()
    {
        return "[id=" + id + ", email=" + email + ", username=" + username + ", action=" + action +
            ", ip=" + ipAddress + ", success=" + success + ", created=" + createdAt + "]";
    }
}
