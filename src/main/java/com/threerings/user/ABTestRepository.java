//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.SimpleRepository;
import com.samskivert.util.IntTuple;

/**
 * Manages database access for figuring out if we're doing an AB test of the website, and if so,
 * keeping track of how many have gone each way.
 */
public class ABTestRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>abtestdb</code>.
     */
    public static final String ABTEST_DB_IDENT = "abtestdb";

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param provider the database connection provider.
     */
    public ABTestRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, ABTEST_DB_IDENT);
    }

    /**
     * Looks up whether or not we're doing an AB test, and what the remaining desired number
     * of people for each bucket is.
     *
     * Returns null if there's no test running.
     */
    public IntTuple getABTestCounts ()
        throws PersistenceException
    {
        return execute(new Operation<IntTuple>() {
            public IntTuple invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = 
                        stmt.executeQuery("select REMAINING_A, REMAINING_B from AB_TEST");
                    while (rs.next()) {
                        int a = rs.getInt(1);
                        int b = rs.getInt(2);

                        if (a != 0 && b != 0) {
                            return new IntTuple(a,b);
                        }
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    public void decrementABTest (final boolean groupA)
        throws PersistenceException
    {
        final IntTuple counts = getABTestCounts();

        if (counts == null) {
            // Not running a test? Ignore it
            return;
        }

        if (counts.left == -1) {
            // Can set to -1/-1 to show we want it to go on indefinitely
            return;
        }

        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "update AB_TEST set REMAINING_" + (groupA ? "A" : "B") + 
                    "=" + (Math.max(0, (groupA ? counts.left : counts.right) - 1)));

                try {
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
        String[] abTestTable = {
            "REMAINING_A INTEGER",
            "REMAINING_B INTEGER",
        };
        JDBCUtil.createTableIfMissing(conn, "AB_TEST", abTestTable, "");
    }
}
