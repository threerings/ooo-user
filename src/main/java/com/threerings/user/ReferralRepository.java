//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;

import static com.threerings.user.Log.log;

/**
 * Tracks the data needed for our personal referral system whereby one
 * user sends a referral link (or email) to their friend and we track the
 * original referring user if their referred friend actually registers
 * within some reasonable time frame (like a couple of weeks).
 */
public class ReferralRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>referraldb</code>.
     */
    public static final String REFERRAL_DB_IDENT = "referraldb";

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param provider the database connection provider.
     */
    public ReferralRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, REFERRAL_DB_IDENT);

        // figure out whether or not the referral system is in use
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                _active = JDBCUtil.tableExists(conn, "REFERRAL");
                if (!_active) {
                    log.info("No referral table. Disabling.");
                }
                return null;
            }
        });
    }

    /**
     * Records a new referral record in the system and returns the unique
     * identifier for said record.
     */
    public int recordReferral (int referrerId, String data)
        throws PersistenceException
    {
        if (!_active) {
            throw new PersistenceException("Referral repository is not available.");
        }

        final ReferralRecord record = new ReferralRecord();
        record.recorded = new Date(System.currentTimeMillis());
        record.referrerId = referrerId;
        record.data = data;
        record.referralId = insert(_rtable, record);

        checkPurge();
        return record.referralId;
    }

    /**
     * Looks up a referral record with the specified id, returning null if
     * no matching record could be found.
     */
    public ReferralRecord lookupReferral (int referralId)
        throws PersistenceException
    {
        return lookupReferralBy("where REFERRAL_ID = " + referralId);
    }

    /**
     * Looks up a referral record with the specified referring user id,
     * returning null if no matching record could be found.
     */
    public ReferralRecord lookupReferrer (int referrerId)
        throws PersistenceException
    {
        return lookupReferralBy("where REFERRER_ID = " + referrerId);
    }

    /** Looks up a referral record matching the specified query. */
    protected ReferralRecord lookupReferralBy (final String query)
        throws PersistenceException
    {
        if (!_active) {
            return null;
        }

        ReferralRecord record = load(_rtable, query);
        checkPurge();
        return record;
    }

    /**
     * Used to periodically purge old referral records from the
     * repository.
     */
    protected void checkPurge ()
    {
        long now = System.currentTimeMillis();
        if (now - _lastPurge > PURGE_INTERVAL) {
            _lastPurge = now;
            try {
                purgeStaleReferrals();
            } catch (PersistenceException pe) {
                log.warning("Error purging referrals: " + pe);
            }
        }
    }

    /**
     * Purges stale referral records from the repository.
     */
    protected void purgeStaleReferrals ()
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String pquery = "delete from REFERRAL " +
                    "where DATE_SUB(NOW(), INTERVAL 1 MONTH) > RECORDED;";
                Statement stmt = conn.createStatement();
                try {
                    stmt.executeUpdate(pquery);
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    @Override
    protected void createTables ()
    {
        _rtable = new Table<ReferralRecord>(ReferralRecord.class, "REFERRAL", "REFERRAL_ID", true);
    }

    /** The table used to new billing actions that have taken place. */
    protected Table<ReferralRecord> _rtable;

    /** If we don't detect our tables, we deactivate ourselves. */
    protected boolean _active;

    /** The last time we purged the repository of stale records. */
    protected long _lastPurge;

    /** We purge the repository every three hours of stale records. */
    protected static final long PURGE_INTERVAL = 3 * 60 * 60 * 1000L;
}
