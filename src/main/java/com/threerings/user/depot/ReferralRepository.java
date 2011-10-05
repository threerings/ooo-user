//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.util.Calendars;

/**
 * Tracks the data needed for our personal referral system whereby one
 * user sends a referral link (or email) to their friend and we track the
 * original referring user if their referred friend actually registers
 * within some reasonable time frame (like a couple of weeks).
 */
@Singleton
public class ReferralRepository extends DepotRepository
{
    @Inject public ReferralRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Records a new referral record in the system and returns the unique
     * identifier for said record.
     */
    public int recordReferral (int referrerId, String data)
    {
        final ReferralRecord record = new ReferralRecord();
        record.recorded = new Date(System.currentTimeMillis());
        record.referrerId = referrerId;
        record.data = data;
        insert(record);

        checkPurge();
        return record.referralId;
    }

    /**
     * Looks up a referral record with the specified id, returning null if
     * no matching record could be found.
     */
    public ReferralRecord lookupReferral (int referralId)
    {
        return load(ReferralRecord.getKey(referralId));
    }

    /**
     * Looks up a referral record with the specified referring user id,
     * returning null if no matching record could be found.
     */
    public ReferralRecord lookupReferrer (int referrerId)
    {
        return load(ReferralRecord.class, new Where(ReferralRecord.REFERRER_ID, referrerId));
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
            purgeStaleReferrals();
        }
    }

    /**
     * Purges stale referral records from the repository.
     */
    protected void purgeStaleReferrals ()
    {
        deleteAll(ReferralRecord.class, new Where(ReferralRecord.RECORDED.lessThan(
                        Calendars.now().zeroTime().addMonths(-1).toSQLDate())));
    }

    @Override // documentation inherited
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ReferralRecord.class);
    }

    /** The last time we purged the repository of stale records. */
    protected long _lastPurge;

    /** We purge the repository every three hours of stale records. */
    protected static final long PURGE_INTERVAL = 3 * 60 * 60 * 1000L;
}
