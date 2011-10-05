//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.io.PersistenceException;
    
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;

/**
 * Stores gameblast aux data for the X users that actually need it, where
 * X is a small and annoying number.
 */
public class GameBlastAuxRepository extends JORARepository
{
    /**
     * Constructs a new GameBlastAuxRepository.
     */
    public GameBlastAuxRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, OOOUserRepository.USER_REPOSITORY_IDENT);
    }

    @Override
    protected void createTables ()
    {
        // nada
    }

    /**
     * @return the username/password for the specified userId, or null
     * if none exists.
     */
    public String[] getAuxData (final int userId)
        throws PersistenceException
    {
        return execute(new Operation<String[]>() {
            public String[] invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                Statement stmt = conn.createStatement();
                try {
                    String query = "select LOGIN, PASSWORD from " +
                        "GAMEBLAST_AUX where USER_ID = " + userId;

                    ResultSet rs = stmt.executeQuery(query);
                    if (rs.next()) {
                        return new String[] { rs.getString(1),
                                              rs.getString(2) };
                    }
                    return null; // didn't find
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Ensure that the specified gameblast login is is not present or if it is,
     * only belongs to the specified user.
     */
    public boolean ensureLoginUnique (final String login, final int userId)
        throws PersistenceException
    {
        Boolean boolval = execute(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement("select USER_ID from " +
                        "GAMEBLAST_AUX where LOGIN=?");
                    stmt.setString(1, login);
                    ResultSet rs = stmt.executeQuery();

                    // return true if there are no matches or if the first
                    // one is us. (There should never be two, anyway).
                    return Boolean.valueOf(
                        !rs.next() || (rs.getInt(1) == userId));
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return boolval.booleanValue();
    }

    /**
     * Save the data for the specified user.
     */
    public void saveAuxData (final int userId,
                             final String login, final String password)
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                PreparedStatement stmt = null;
                try {
                    // try updating first
                    stmt = conn.prepareStatement(
                        "update GAMEBLAST_AUX set " + 
                        "LOGIN=?, PASSWORD=? where USER_ID=?");
                    stmt.setString(1, login);
                    stmt.setString(2, password);
                    stmt.setInt(3, userId);
                    if (stmt.executeUpdate() != 1) {
                        // insert a new one
                        PreparedStatement stmt2 = null;
                        try {
                            stmt2 = conn.prepareStatement(
                                "insert into GAMEBLAST_AUX " +
                                "(USER_ID, LOGIN, PASSWORD) values (?, ?, ?)");
                            stmt2.setInt(1, userId);
                            stmt2.setString(2, login);
                            stmt2.setString(3, password);
                            JDBCUtil.checkedUpdate(stmt2, 1);
                        } finally {
                            JDBCUtil.close(stmt2);
                        }
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Remove the gameblast mapping for the specified user.
     */
    public void removeAuxData (final int userId)
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                Statement stmt = conn.createStatement();
                try {
                    String query = "delete from GAMEBLAST_AUX where USER_ID=" +
                        userId;
                    stmt.executeUpdate(query);
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }
}
