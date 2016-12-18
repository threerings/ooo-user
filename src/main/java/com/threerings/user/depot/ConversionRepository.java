//
// $Id$

package com.threerings.user.depot;

import java.sql.Timestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.ConnectionProvider;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.util.Calendars;
import com.samskivert.util.IntIntMap;

/**
 * Provides an interface to the conversion repository. A service that
 * makes use of the OOO user management and billing system can make use of
 * this conversion service to track conversion related information for
 * their users.
 */
@Singleton
public class ConversionRepository extends DepotRepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>conversiondb</code>.
     */
    public static final String CONVERSION_DB_IDENT = "conversiondb";

    @Inject public ConversionRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates the repository and prepares it for operation.
     *
     * @param conprov the database connection provider.
     */
    public ConversionRepository (ConnectionProvider conprov)
    {
        super(new PersistenceContext(CONVERSION_DB_IDENT, conprov, null));
    }

    /**
     * Records an action in the conversion table.
     */
    public void recordAction (int userId, int siteId, int action)
    {
        recordAction(userId, siteId, action, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Records an action in the conversion table.
     */
    public void recordAction (int userId, int siteId, int action, Timestamp recorded)
    {
        ConversionRecord record = new ConversionRecord();
        record.userId = userId;
        record.siteId = siteId;
        record.action = (short)action;
        record.recorded = recorded;
        insert(record);
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
     * Returns all of our subscription info. {@code Date -> SiteId -> Subscribers}. SiteId 0 is a
     * total of all sites.
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
            computeSubscriptionInfo();
        }
    }

    /**
     * Build up all the subscriber data over time and affiliate.
     */
    protected void computeSubscriptionInfo ()
    {
        // get all the raw data from the db, ordered by date
        List<ConversionRecord> data = findAll(ConversionRecord.class,
                new Where(ConversionRecord.ACTION.in(ConversionRecord.SUBSCRIPTION_START,
                        ConversionRecord.SUBSCRIPTION_ENDED)),
                OrderBy.ascending(ConversionRecord.RECORDED));

        java.util.Date recent = null;

        Set<Integer> subs = Sets.newHashSet();
        Map<Integer, Set<Integer>> siteSubs = Maps.newHashMap();

        // these are used to do the right thing if we get both a subscribe
        // and unsubscribe action in the same day, but in the wrong order
        Set<Integer> subexcept = Sets.newHashSet();
        Set<Integer> unsubexcept = Sets.newHashSet();

        for (ConversionRecord cr : data) {
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
                siteSubs.put(cr.getSiteId(), Sets.<Integer>newHashSet());
            }
            Set<Integer> siteMap = siteSubs.get(cr.getSiteId());

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

    protected boolean checkExcept (
        Set<Integer> expect, Set<Integer> surprise, boolean currentStatus, ConversionRecord cr)
    {
        // first check to see if he's in the opposite exception set; in
        // which case we clear him out and ignore this action
        if (expect.remove(cr.userId)) {
            return true;
        }

        // next, check to see if he's already in the expected state, in which
        // case we put him into the exception set
        return currentStatus;
    }

    /**
     * Add an entry to the _subscriptions data for the given day.
     */
    protected void addSubscriptionEntry (
        java.util.Date day, Set<Integer> subs, Map<Integer, Set<Integer>> siteSubs)
    {
        IntIntMap daysubs = new IntIntMap();
        for (Map.Entry<Integer, Set<Integer>> entry : siteSubs.entrySet()) {
            Set<Integer> set = entry.getValue();
            int key = entry.getKey().intValue();
            daysubs.put(key, set.size());
        }
        daysubs.put(0, subs.size());
        _subscribers.put(day, daysubs);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ConversionRecord.class);
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
}
