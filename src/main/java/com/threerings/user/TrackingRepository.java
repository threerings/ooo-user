//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.SimpleRepository;

/**
 * Manages database access for doing our own quick and dirty tracking ala clientstep.
 */
public class TrackingRepository extends SimpleRepository
{
    /** Name of the tracking table. */
    public static final String TRACKING_TABLE = "TRACKING";

    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>trackingdb</code>.
     */
    public static final String TRACKING_DB_IDENT = "trackingdb";

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param provider the database connection provider.
     */
    public TrackingRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, TRACKING_DB_IDENT);
    }

    /**
     * Records a tracking event to the database.
     */
    public void addTrackingEvent (final String event, final String description,
        final int accountId, final int siteId)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement("insert into " + TRACKING_TABLE +
                        " (TIME, EVENT, DESCRIPTION, ACCOUNT_ID, SITE_ID)" +
                        " values (NOW(), ?, ?, ?, ?)");
                    stmt.setString(1, event);
                    stmt.setString(2, description);
                    stmt.setInt(3, accountId);
                    stmt.setInt(4, siteId);

                    JDBCUtil.checkedUpdate(stmt, 1);
                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }

    @Override
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        String[] TrackingTable = {
            "TIME DATETIME NOT NULL",
            "EVENT VARCHAR(255)",
            "DESCRIPTION VARCHAR(255)",
            "ACCOUNT_ID INTEGER",
            "SITE_ID INTEGER",
        };
        JDBCUtil.createTableIfMissing(conn, TRACKING_TABLE, TrackingTable, "");

        if (!JDBCUtil.tableContainsColumn(conn, TRACKING_TABLE, "ACCOUNT_ID")) {
            JDBCUtil.addColumn(conn, TRACKING_TABLE, "ACCOUNT_ID", "INTEGER", "DESCRIPTION");
            JDBCUtil.addColumn(conn, TRACKING_TABLE, "SITE_ID", "INTEGER", "ACCOUNT_ID");
        }
    }
}
