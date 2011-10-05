//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import static com.threerings.user.Log.log;

/**
 * Provides facilities for saving a user's Yohoho beta information to the
 * persistent beta info table in the database.
 */
public class YoBetaRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>yobetadb</code>.
     */
    public static final String YOBETA_DB_IDENT = "yobetadb";

    /**
     * Creates the yo beta repository.
     *
     * @param provider the database connection provider.
     */
    public YoBetaRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, YOBETA_DB_IDENT);

        // figure out whether or not the YOBETA table is in use on this system
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                _active = JDBCUtil.tableExists(conn, YOBETA_TABLE_NAME);
                if (!_active) {
                    log.info("No beta repository table. Disabling.");
                }
                return null;
            }
        });
    }

    /**
     * Inserts the supplied yo beta info for the specified user into the
     * database.
     */
    public void insertYoBetaInfo (final YoBetaInfo ybi)
        throws PersistenceException
    {
        if (!_active) {
            return;
        }
        insert(_table, ybi);
    }

    /**
     * Loads and returns the yo beta info for the specified user id from
     * the database, or <code>null</code> if no beta info for the user
     * exists.
     */
    public YoBetaInfo loadYoBetaInfo (final int userId)
        throws PersistenceException
    {
        if (!_active) {
            return null;
        }

        YoBetaInfo proto = new YoBetaInfo();
        proto.userId = userId;
        FieldMask mask = _table.getFieldMask();
        mask.setModified("userId");
        return loadByExample(_table, proto, mask);
    }

    /**
     * Deletes the yo beta info for the specified user id from the
     * database.
     */
    public void deleteYoBetaInfo (final int userId)
        throws PersistenceException
    {
        if (!_active) {
            return;
        }

        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "delete from " + YOBETA_TABLE_NAME + " " +
                        "where USER_ID = ?");
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();

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
        _table = new Table<YoBetaInfo>(
            YoBetaInfo.class, YOBETA_TABLE_NAME,
            YOBETA_TABLE_PRIMARY_KEY, true);
    }

    /** A wrapper that provides access to the yo beta table. */
    protected Table<YoBetaInfo> _table;

    /** Whether or not we are actually in use. If our table doesn't exist,
     * we quietly disable ourselves. */
    protected boolean _active;

    /** The name of the yobeta table. */
    protected static final String YOBETA_TABLE_NAME = "YOBETA";

    /** The primary key for the yobeta table. */
    protected static final String YOBETA_TABLE_PRIMARY_KEY = "USER_ID";
}
