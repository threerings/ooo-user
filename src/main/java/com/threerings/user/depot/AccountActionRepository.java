//
// $Id$

package com.threerings.user.depot;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.util.StringUtil;

import com.threerings.user.AccountAction;

import static com.threerings.user.Log.log;

/**
 * Provides access to the account actions records.
 */
@Singleton
public class AccountActionRepository extends DepotRepository
{
    /**
     * The database identifier used when establishing a database connection.
     * This value being <code>actiondb</code>.
     */
    public static final String ACTION_DB_IDENT = "actiondb";

    @Inject public AccountActionRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    public AccountActionRepository (ConnectionProvider conprov)
    {
        super(new PersistenceContext(ACTION_DB_IDENT, conprov, null));
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
    {
        // TODO: Figure out how to do this in one query
        List<Integer> processedIds = from(ProcessedActionRecord.class)
            .where(ProcessedActionRecord.SERVER.eq(server))
            .select(ProcessedActionRecord.ACTION_ID);
        List<AccountActionRecord> actions = findAll(AccountActionRecord.class,
                new Where(Ops.not(AccountActionRecord.ACTION_ID.in(processedIds))));

        List<AccountAction> list = Lists.newArrayListWithCapacity(actions.size());
        for (AccountActionRecord record : actions) {
            list.add(record.toAccountAction());
        }

        if (StringUtil.isBlank(server)) {
            return list;
        }

        // if this is the first time this method is called, register ourselves
        // with the action system
        long now = System.currentTimeMillis();
        if (_lastActionPrune == 0L) {
            _lastActionPrune = now;
            registerActionServer(server);

        } else if (now - _lastActionPrune > ACTION_PRUNE_INTERVAL) {
            _lastActionPrune = now;
            pruneActions();
        }
        return list;
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, int action)
    {
        addAction(accountName, null, action, "");
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, String data, int action)
    {
        addAction(accountName, data, action, "");
    }

    /**
     * Adds a new action to the repository.
     */
    public void addAction (String accountName, int action, String server)
    {
        addAction(accountName, null, action, server);
    }

    /**
     * Adds a new action to the repository, with the specified server being marked as already
     * having processed the action.
     */
    public void addAction (String accountName, String data, int action, final String server)
    {
        AccountActionRecord record = new AccountActionRecord();
        record.accountName = accountName;
        record.data = data;
        record.action = action;
        record.entered = new Timestamp(System.currentTimeMillis());
        insert(record);

        // note that it was processed by this server if appropriate
        if (!StringUtil.isBlank(server)) {
            noteProcessed(record.actionId, server);
        }
    }

    /**
     * Updates the single specified action to reflect that the specified server has processed it.
     */
    public void updateAction (AccountAction action, String server)
    {
        updateActions(Collections.singletonList(action), server);
    }

    /**
     * Batch update a list of actions.
     */
    public void updateActions (List<AccountAction> actions, String server)
    {
        for (int ii = 0, ll = actions.size(); ii < ll; ii++) {
            noteProcessed(actions.get(ii).actionId, server);
        }
    }

    /**
     * Deletes an action from the repository.
     */
    public void deleteAction (AccountAction action)
    {
        delete(AccountActionRecord.fromAccountAction(action));
        deleteAll(ProcessedActionRecord.class,
                new Where(ProcessedActionRecord.ACTION_ID.eq(action.actionId)));
    }

    /**
     * Notes that the specified server has processed the specified action.
     */
    public void noteProcessed (int actionId, String server)
    {
        ProcessedActionRecord record = new ProcessedActionRecord();
        record.actionId = actionId;
        record.server = server;
        insert(record);
    }

    /**
     * Prunes actions that have been processed by all registered servers to keep the actions table
     * small. This method need not be called by hand as it is called automatically by
     * {@link #getActions(String,int)}, but no more frequently than once an hour.
     */
    public void pruneActions ()
    {
        Set<String> servers = loadActionServers();
        Map<Integer, Set<String>> procmap = Maps.newHashMap();
        Set<Integer> actids = Sets.newHashSet();

        // determine which servers have processed which actions
        for (ProcessedActionRecord record : findAll(ProcessedActionRecord.class)) {
            Set<String> procset = procmap.get(record.actionId);
            if (procset == null) {
                procmap.put(record.actionId, procset = Sets.newHashSet());
            }
            procset.add(record.server);
        }

        // determine which of those have been fully processed
        for (Map.Entry<Integer, Set<String>> entry : procmap.entrySet()) {
            if (entry.getValue().containsAll(servers)) {
                actids.add(entry.getKey());
            }
        }

        // now wipe out the actions (and processed entries for all actions that have been fully
        // processed
        if (actids.size() > 0) {
            deleteAll(AccountActionRecord.class,
                    new Where(AccountActionRecord.ACTION_ID.in(actids)));
            deleteAll(ProcessedActionRecord.class,
                    new Where(ProcessedActionRecord.ACTION_ID.in(actids)));
        }
    }

    /**
     * Registers a server with the action system. This method is called automatically the first
     * time {@link #getActions(String)} is called.
     */
    protected void registerActionServer (String server)
    {
        // see if we're already registered
        Set<String> set = loadActionServers();
        if (set.contains(server)) {
            return;
        }

        ActionServerRecord record = new ActionServerRecord();
        record.server = server;
        insert(record);

        log.info("Registered action server", "server", server);
    }

    /**
     * Loads up the set of action servers.
     */
    protected Set<String> loadActionServers ()
    {
        Set<String> set = Sets.newHashSet();
        for (ActionServerRecord record : findAll(ActionServerRecord.class)) {
            set.add(record.server);
        }
        return set;
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(AccountActionRecord.class);
        classes.add(ActionServerRecord.class);
        classes.add(ProcessedActionRecord.class);
    }

    /** Used by {@link #getActions(String)}. */
    protected long _lastActionPrune;

    /** We automatically prune the actions table once an hour. */
    protected static final long ACTION_PRUNE_INTERVAL = 60 * 60 * 1000L;
}
