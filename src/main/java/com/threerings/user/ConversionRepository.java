//
// $Id$

package com.threerings.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.Calendars;

import static com.threerings.user.Log.log;

/**
 * Provides an interface to the conversion repository. A service that
 * makes use of the OOO user management and billing system can make use of
 * this conversion service to track conversion related information for
 * their users.
 */
public class ConversionRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>conversiondb</code>.
     */
    public static final String CONVERSION_DB_IDENT = "conversiondb";

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param provider the database connection provider.
     */
    public ConversionRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, CONVERSION_DB_IDENT);

        // figure out whether or not the forums are in use on this system
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                _active = JDBCUtil.tableExists(conn, "CONVERSION");
                if (!_active) {
                    log.info("No conversion table. Disabling.");
                }
                return null;
            }
        });

        // TEMP CODE To update the repository to using a full integer for
        // siteId, as personal referrer ids are -userIds which can be
        // quite large~
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                if (!_active) {
                    return null;
                }
                if (JDBCUtil.getColumnType(conn, "CONVERSION", "SITE_ID") == Types.SMALLINT) {
                    JDBCUtil.changeColumn(conn, "CONVERSION", "SITE_ID",
                                          "SITE_ID INTEGER NOT NULL");
                }
                return null;
            }
        });
    }

    /**
     * Records an action in the conversion table.
     */
    public void recordAction (int userId, int siteId, int action)
        throws PersistenceException
    {
        recordAction(userId, siteId, action, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Records an action in the conversion table.
     */
    public void recordAction (int userId, int siteId, int action, Timestamp recorded)
        throws PersistenceException
    {
        if (!_active) {
            return;
        }

        ConversionRecord record = new ConversionRecord();
        record.userId = userId;
        record.siteId = siteId;
        record.action = (short)action;
        record.recorded = recorded;
        insert(_ctable, record);
    }

    /**
     * Get the subscriber info for a given date broken up by site.  Key 0
     * references a total for all sites.
     */
    public IntIntMap getSiteSubscribers (java.util.Date date)
    {
        checkRecomputeSubInfo();
        return _subscribers.get(date);
    }

    /**
     * Return the total number of subscribers on then given date.
     */
    public int getTotalSubscribers (java.util.Date date)
    {
        checkRecomputeSubInfo();
        return _subscribers.get(date).get(0);
    }

    /**
     * Returns all of our subscription info.  Date -> IntIntMap,
     * IntIntMap: SiteId -> Subscribers.  SiteId 0 is a total of all sites.
     */
    public Map<Date, IntIntMap> getSubscriptionInfo ()
    {
        checkRecomputeSubInfo();
        return _subscribers;
    }

    /**
     * Recompute our subscription info if it is older than the cache time.
     */
    protected void checkRecomputeSubInfo ()
    {
        if (_subTime + SUB_CACHE_TIME < System.currentTimeMillis()) {
            try {
                computeSubscriptionInfo();
            } catch (PersistenceException pe) {
                log.warning("Failed to compute our subscription info: " + pe);
            }
        }
    }

    /**
     * Build up all the subscriber data over time and affiliate.
     */
    protected void computeSubscriptionInfo ()
        throws PersistenceException
    {
        // get all the raw data from the db, ordered by date
        List<ConversionRecord> data = loadAll(_ctable, "where ACTION in (" +
            ConversionRecord.SUBSCRIPTION_START + ", " +
            ConversionRecord.SUBSCRIPTION_ENDED + ") order by RECORDED");

        java.util.Date recent = null;

        ArrayIntSet subs = new ArrayIntSet();
        HashIntMap<ArrayIntSet> siteSubs = new HashIntMap<ArrayIntSet>();

        // these are used to do the right thing if we get both a subscribe
        // and unsubscribe action in the same day, but in the wrong order
        ArrayIntSet subexcept = new ArrayIntSet();
        ArrayIntSet unsubexcept = new ArrayIntSet();

        Iterator<ConversionRecord> itr = data.iterator();
        while (itr.hasNext()) {
            ConversionRecord cr = itr.next();

            // we only care about the date, not the time, at which the
            // entry was recorded since we are hashing by day
            java.util.Date day = Calendars.at(cr.recorded).zeroTime().toDate();

            // if the date changes, store the data for the most recent day
            if (recent == null) {
                recent = day;
            } else if (!recent.equals(day)) {
                subexcept.clear();
                unsubexcept.clear();
                addSubscriptionEntry(recent, subs, siteSubs);
                recent = day;
            }

            // make sure we have a sitewise set of subscribers
            if (!siteSubs.containsKey(cr.getSiteId())) {
                siteSubs.put(cr.getSiteId(), new ArrayIntSet());
            }
            ArrayIntSet siteMap = siteSubs.get(cr.getSiteId());

            // update our data
            if (cr.action == ConversionRecord.SUBSCRIPTION_START) {
                // handle not-subscribed -> unsubscribe -> subscribe
                if (!checkExcept(unsubexcept, subexcept,
                                 subs.contains(cr.userId), cr)) {
                    subs.add(cr.userId);
                    siteMap.add(cr.userId);
                }

            } else if (cr.action == ConversionRecord.SUBSCRIPTION_ENDED) {
                // handle is-subscribed -> subscribe -> unsubscribe
                if (!checkExcept(subexcept, unsubexcept,
                                 !subs.contains(cr.userId), cr)) {
                    subs.remove(cr.userId);
                    siteMap.remove(cr.userId);
                }
            }
        }

        // add in the incomplete current day info
        addSubscriptionEntry(Calendars.now().zeroTime().toDate(), subs, siteSubs);

        // and mark when we finished
        _subTime = System.currentTimeMillis();
    }

    protected boolean checkExcept (ArrayIntSet expect, ArrayIntSet surprise,
                                   boolean currentStatus, ConversionRecord cr)
    {
        // first check to see if he's in the opposite exception set; in
        // which case we clear him out and ignore this action
        if (expect.remove(cr.userId)) {
//             Log.info("Processed exception [rec=" + cr +
//                      ", status=" + currentStatus + "].");
            return true;
        }

        // next, check to see if he's already in the expected state, in which
        // case we put him into the exception set
        return currentStatus;
    }

    /**
     * Add an entry to the _subscriptions data for the given day.
     */
    protected void addSubscriptionEntry (java.util.Date day, ArrayIntSet subs,
                                         HashIntMap<ArrayIntSet> siteSubs)
    {
        IntIntMap daysubs = new IntIntMap();
        for (Map.Entry<Integer,ArrayIntSet> entry : siteSubs.entrySet()) {
            ArrayIntSet set = entry.getValue();
            int key = entry.getKey().intValue();
            daysubs.put(key, set.size());
        }
        daysubs.put(0, subs.size());
        _subscribers.put(day, daysubs);
    }

    @Override
    protected void createTables ()
    {
        _ctable = new Table<ConversionRecord>(
            // this isn't the real primary key, but we never load conversion
            // records individually
            ConversionRecord.class, "CONVERSION", "RECORDED", true);
    }

    /**
     * Contains subscription info about our users over time. Keys are
     * dates and the values are IntIntMaps mapping siteId -> subsrivers
     * with 0 holding the total for all sites.
     */
    protected Map<Date, IntIntMap> _subscribers = Maps.newHashMap();

    /** The time at which we last computed our subscriber info. */
    protected long _subTime;

    /** The time in millis to cache our subscriber info before recomputing. */
    protected long SUB_CACHE_TIME = 1l * 60l * 60l * 1000l;

    /** If we don't detect our tables, we deactivate ourselves. */
    protected boolean _active;

    /** The table used to new billing actions that have taken place. */
    protected Table<ConversionRecord> _ctable;

    /** The date (3/17/2005 in ms) after which we should complain about
        wacky conversion data.  */
    protected static long FIXED_DATE = 1111090429312L;
}
