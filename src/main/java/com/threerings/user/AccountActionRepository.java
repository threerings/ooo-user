//
// $Id$

package com.threerings.user;

import static com.threerings.user.Log.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

/**
 * Provides access to the account actions table.
 *
 * @deprecated Use com.threerings.user.depot.AccountActionRepository instead.
 */
@Deprecated
public class AccountActionRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database connection.
     * This value being <code>actiondb</code>.
     */
    public static final String ACTION_DB_IDENT = "actiondb";

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param provider the database connection provider.
     */
    public AccountActionRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, ACTION_DB_IDENT);

        // figure out whether or not we should be disabled
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                _active = JDBCUtil.tableExists(conn, "ACCOUNT_ACTIONS");
                if (!_active) {
                    log.info("No actions table. Disabling.");
                }
                return null;
            }
        });
    }

    /**
     * Return the list of actions that have not yet been processed by the specified server.
     * Returns all actions if no server name is specified.
     *
     * <em>Note:</em> this method has two side effects: the first time it is called, it will
     * register this server as a action participant (assuming server is non-null) and
     * subsequently, it will call {@link #pruneActions} if it has not done so within the last
     * hour.
     */
    public List<AccountAction> getActions (String server)
        throws PersistenceException
    {
        return getActions(server, Integer.MAX_VALUE);
    }

    /**
     * Return the list of actions that have not yet been processed by the specified server.
     * Returns all actions if no server name is specified.
     *
     * @param maxActions the maximum number of actions to return.
     *
     * <em>Note:</em> this method has two side effects: the first time it is called, it will
     * register this server as a action participant (assuming server is non-null) and
     * subsequently, it will call {@link #pruneActions} if it has not done so within the last
     * hour.
     */
    public List<AccountAction> getActions (String server, int maxActions)
        throws PersistenceException
    {
        if (!_active) {
            return new ArrayList<AccountAction>();
        }

        // look ma, subselects
        String where = "where ACTION_ID NOT IN " +
            "(select ACTION_ID from PROCESSED_ACTIONS " +
            "where SERVER = '" + server + "') limit " + maxActions;
        List<AccountAction> list = loadAll(_atable, where);

        // unjigger the account names
        for (AccountAction action : list) {
            action.accountName = JDBCUtil.unjigger(action.accountName);
        }

        // no fooling around if we are acting in general
        if (StringUtil.isBlank(server)) {
            return list;
        }

        // if this is the first time this method is called, register ourselves
        // with the action system
        long now = System.currentTimeMillis();
        if (_lastActionPrune == 0L) {
            _lastActionPrune = now;
            try {
                registerActionServer(server);
            } catch (PersistenceException pe) {
                log.warning("Failure registering server", "server", server, pe);
            }

        } else if (now - _lastActionPrune > ACTION_PRUNE_INTERVAL) {
            _lastActionPrune = now;
            try {
                pruneActions();
            } catch (PersistenceException pe) {
                log.warning("Failure auto-pruning actions", "server", server, pe);
            }
        }

        return list;
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, int action)
        throws PersistenceException
    {
        addAction(accountName, null, action, "");
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, String data, int action)
        throws PersistenceException
    {
        addAction(accountName, data, action, "");
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, int action, String server)
        throws PersistenceException
    {
        addAction(accountName, null, action, server);
    }

    /**
     * Adds a new action to the repository, with the specified server being marked as already
     * having processed the action.
     */
    public void addAction (String accountName, String data, int action, final String server)
        throws PersistenceException
    {
        if (!_active) {
            log.info("Dropping account action",
                "name", accountName, "data", data, "action", action, "server", server);
            return;
        }

        // create and insert the account action
        final AccountAction aact = new AccountAction();
        aact.accountName = JDBCUtil.jigger(accountName);
        aact.data = data;
        aact.action = action;
        aact.entered = new Timestamp(System.currentTimeMillis());
        aact.actionId = insert(_atable, aact);

        // note that it was processed by this server if appropriate
        if (!StringUtil.isBlank(server)) {
            noteProcessed(aact.actionId, server);
        }
    }

    /**
     * Updates the single specified action to reflect that the specified server has processed it.
     */
    public void updateAction (AccountAction action, String server)
        throws PersistenceException
    {
        updateActions(Collections.singletonList(action), server);
    }

    /**
     * Batch update a list of actions.
     */
    public void updateActions (List<AccountAction> actions, String server)
        throws PersistenceException
    {
        for (int ii = 0, ll = actions.size(); ii < ll; ii++) {
            noteProcessed(actions.get(ii).actionId, server);
        }
    }

    /**
     * Deletes an action from the repository.
     */
    public void deleteAction (final AccountAction action)
        throws PersistenceException
    {
        // delete the action
        delete(_atable, action);

        // and any records indicating it has been processed
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    stmt.executeUpdate("delete from PROCESSED_ACTIONS " +
                                       "where ACTION_ID = " + action.actionId);
                } finally {
                    stmt.close();
                }
                return null;
            }
        });
    }

    /**
     * Notes that the specified server has processed the sepecified action.
     */
    public void noteProcessed (final int actionId, final String server)
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    String query = "insert into PROCESSED_ACTIONS " +
                        "values(" + actionId + ", '" + server + "')";
                    stmt.executeUpdate(query);
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Prunes actions that have been processed by all registered servers to keep the actions table
     * small. This method need not be called by hand as it is called automatically by
     * {@link #getActions(String,int)}, but no more frequently than once an hour.
     */
    public void pruneActions ()
        throws PersistenceException
    {
        final Set<String> servers = loadActionServers();
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Map<Integer,Set<String>> procmap = Maps.newHashMap();
                ArrayIntSet actids = new ArrayIntSet();
                Statement stmt = conn.createStatement();
                try {
                    // determine which servers have processed which actions
                    ResultSet rs = stmt.executeQuery(
                        "select ACTION_ID, SERVER from PROCESSED_ACTIONS");
                    while (rs.next()) {
                        Integer actionId = (Integer)rs.getObject(1);
                        Set<String> procset = procmap.get(actionId);
                        if (procset == null) {
                            procmap.put(actionId, procset = Sets.newHashSet());
                        }
                        procset.add(rs.getString(2));
                    }

                    // determine which of those have been fully processed
                    for (Map.Entry<Integer,Set<String>> entry : procmap.entrySet()) {
                        Integer actionId = entry.getKey();
                        Set<String> procset = entry.getValue();
                        if (procset.containsAll(servers)) {
                            actids.add(actionId.intValue());
                        }
                    }

                    // now wipe out the actions (and processed entries) for
                    // all actions that have been fully processed
                    if (actids.size() > 0) {
                        String where = "where ACTION_ID in " +
                            "(" + Joiner.on(",").join(actids) + ")";
                        stmt.executeUpdate("delete from ACCOUNT_ACTIONS " + where);
                        stmt.executeUpdate("delete from PROCESSED_ACTIONS " + where);
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Registers a server with the action system. This method is called automatically the first
     * time {@link #getActions(String)} is called.
     */
    protected void registerActionServer (final String server)
        throws PersistenceException
    {
        // see if we're already registered
        Set<String> set = loadActionServers();
        if (set.contains(server)) {
            return;
        }

        // if not, insert ourselves into the action table
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "insert into ACTION_SERVERS (SERVER) values (?)";
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, server);
                    stmt.executeUpdate();

                    return null;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        log.info("Registered action server '" + server + "'.");
    }

    /**
     * Loads up the set of action servers.
     */
    protected Set<String> loadActionServers ()
        throws PersistenceException
    {
        return execute(new Operation<Set<String>>() {
            public Set<String> invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Set<String> set = Sets.newHashSet();
                String query = "select * from ACTION_SERVERS";
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        set.add(rs.getString(1));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return set;
            }
        });
    }

    @Override
    protected void createTables ()
    {
        _atable = new Table<AccountAction>(AccountAction.class, TABLE, "ACTION_ID", true);
    }

    /** The table used to note actions that have taken place. */
    protected Table<AccountAction> _atable;

    /** Whether or not we're actually being used. */
    protected boolean _active;

    /** Used by {@link #getActions(String)}. */
    protected long _lastActionPrune;

    /** We automatically prune the actions table once an hour. */
    protected static final long ACTION_PRUNE_INTERVAL = 60 * 60 * 1000L;

    /** The name of the account actions table. */
    protected static final String TABLE = "ACCOUNT_ACTIONS";
}
