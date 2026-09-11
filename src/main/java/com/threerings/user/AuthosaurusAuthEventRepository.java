package com.threerings.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;

/**
 * Provides read-only access to the authosaurus <code>auth_log</code> table, which lives in the
 * same database as the OOO user tables. The table is created and maintained entirely by
 * authosaurus; we never write to it and must not create it. See {@link AuthosaurusAuthEvent} for
 * the schema this mirrors and for what must be kept in sync with the authosaurus code.
 *
 * <p>Note that authosaurus expires rows after a retention window (180 days at the time of
 * writing), so events are only available for a while after they take place.</p>
 */
public class AuthosaurusAuthEventRepository extends SimpleRepository
{
    /**
     * Creates a repository that reads the auth log from the OOO user database.
     *
     * @param provider the database connection provider.
     */
    public AuthosaurusAuthEventRepository (ConnectionProvider provider)
    {
        super(provider, OOOUserRepository.USER_REPOSITORY_IDENT);
    }

    /**
     * Loads the event recording the creation of the specified account, which reports when the
     * account was created and the IP address it was created from.
     *
     * <p>We match on email address rather than account name because only the former is indexed,
     * and a lookup that finds nothing is by far the common case (every account that predates
     * authosaurus, plus every one whose creation has aged out of the log); because a single email
     * address can own several accounts, we match on the account name as well. The upshot is that
     * an account whose email address has changed since it was created will not be found, which we
     * accept rather than scan the table.</p>
     *
     * @param email the account's current email address, or null if it is unknown.
     * @param username the account name.
     * @return the account's creation event, or null if we have none: the account predates
     * authosaurus, was created by some other means (e.g. a thirdparty authenticator), its creation
     * event has aged out of the auth log, or its email address has since changed.
     */
    public AuthosaurusAuthEvent loadAccountCreation (String email, String username)
        throws PersistenceException
    {
        if (email == null) {
            return null;
        }
        return loadEvent(BY_EMAIL_AND_USERNAME, email, username,
                         AuthosaurusAuthEvent.CREATE_ACCOUNT_CONFIRM);
    }

    /**
     * Loads the oldest successful event matching the supplied query.
     *
     * @param query the query to run; it must select {@link #COLUMNS} in order.
     * @param params the values to bind to the query's parameters, in order.
     * @return the matching event, or null if there was none.
     */
    protected AuthosaurusAuthEvent loadEvent (final String query, final Object... params)
        throws PersistenceException
    {
        return execute(new Operation<AuthosaurusAuthEvent>() {
            public AuthosaurusAuthEvent invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(query);
                    for (int ii = 0; ii < params.length; ii++) {
                        stmt.setObject(ii + 1, params[ii]);
                    }
                    ResultSet rs = stmt.executeQuery();
                    return rs.next() ? decodeEvent(rs) : null;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Decodes the row at the current position of the supplied result set, which must have selected
     * {@link #COLUMNS} in order.
     *
     * @param rs the result set to decode.
     * @return the decoded event.
     */
    protected static AuthosaurusAuthEvent decodeEvent (ResultSet rs)
        throws SQLException
    {
        AuthosaurusAuthEvent event = new AuthosaurusAuthEvent();
        event.id = rs.getLong(1);
        event.siteId = rs.getInt(2);
        event.email = rs.getString(3);
        int userId = rs.getInt(4);
        event.userId = rs.wasNull() ? null : Integer.valueOf(userId);
        event.username = rs.getString(5);
        event.action = rs.getString(6);
        event.ipAddress = rs.getString(7);
        event.userAgent = rs.getString(8);
        boolean vpnDetected = rs.getBoolean(9);
        event.vpnDetected = rs.wasNull() ? null : Boolean.valueOf(vpnDetected);
        event.network = rs.getString(10);
        double staticIpScore = rs.getDouble(11);
        event.staticIpScore = rs.wasNull() ? null : Double.valueOf(staticIpScore);
        event.blockReason = rs.getString(12);
        event.success = rs.getBoolean(13);
        event.createdAt = rs.getTimestamp(14);
        return event;
    }

    /** The auth log columns, in the order {@link #decodeEvent} expects them. */
    protected static final String COLUMNS = "id, site_id, email, user_id, username, action, " +
        "ip_address, user_agent, vpn_detected, network, static_ip_score, block_reason, " +
        "success, created_at";

    /** Selects the oldest successful event of a given action for an email address and account
     * name. Uses the auth_log_email_created_idx index on the email column. */
    protected static final String BY_EMAIL_AND_USERNAME =
        "select " + COLUMNS + " from auth_log where email = ? and username = ? and action = ? " +
        "and success = true order by created_at asc, id asc limit 1";
}
