//
// $Id: RewardRepository.java 3547 2011-03-03 04:40:51Z jamie $

package com.threerings.user.depot;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.ConnectionProvider;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.user.OOOUserRepository;

import static com.threerings.user.Log.log;

/**
 * Maintains persistent reward information.
 */
@Singleton
public class RewardRepository extends DepotRepository
{
    @Inject public RewardRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates the repository with the specified connection provider.
     *
     */
    public RewardRepository (ConnectionProvider provider)
    {
        super(new PersistenceContext(OOOUserRepository.USER_REPOSITORY_IDENT, provider, null));
    }

    /**
     * Creates a new RewardInfo record in the database.  <code>info</code> should have the
     * <code>description</code>, <code>data</code> and <code>expiration</code> filled in.
     */
    public void createReward (RewardInfoRecord info)
    {
        //info.maxEligibleId = getMaxUserId();
        store(info);
    }

    /**
     * Immediately expires a reward by setting the expiration to the current date then making a
     * call to <code>purgeExpiredRewards</code>.
     */
    public void expireReward (int rewardId)
    {
        updatePartial(RewardInfoRecord.getKey(rewardId), RewardInfoRecord.EXPIRATION,
                new Date(System.currentTimeMillis()));
        purgeExpiredRewards();
    }

    /**
     * Attempt to activate the reward for an account.  If this account has not already activated
     * the reward, a new RewardRecord will be created.  Returns true if a new RewardRecord is
     * created, false otherwise.
     */
    public boolean activateReward (int rewardId, String account)
    {
        return activateReward(rewardId, account, null);
    }

    public boolean activateReward (int rewardId, String account, String param)
    {
        RewardRecord rr = new RewardRecord();
        rr.rewardId = rewardId;
        rr.account = account;
        rr.param = param == null ? "" : param;
        try {
            insert(rr);
        } catch (DuplicateKeyException e) {
            return false;
        }
        return true;
    }

    /**
     * Activates monthly rewards of the specified ID for the account between start and end.  If they
     *  already have monthly rewards for this ID, will add new ones only at appropriate monthly
     *  intervals from the existing ones.
     */
    public boolean activateMonthlyRewards (
            int rewardId, String account, java.util.Date start, java.util.Date end)
    {
        // Find the most recent reward this account already has of this type.
        java.util.Date latest = null;
        List<RewardRecord> matches = findAll(RewardRecord.class,
                new Where(Ops.and(RewardRecord.REWARD_ID.eq(rewardId),
                    RewardRecord.ACCOUNT.eq(account))));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Find the most recent reward this account already has of this type.
        for (RewardRecord rec : matches) {
            if (rec.param == null) {
                log.warning("Missing monthly reward param: ", "rewardId", rewardId,
                    "account", account);
                continue;
            }
            try {
                java.util.Date thisDate = dateFormat.parse(rec.param);
                if (latest == null || thisDate.after(latest)) {
                    latest = thisDate;
                }
            } catch (ParseException pe) {
                log.warning("Bogus reward param: ", "rewardId", rewardId,
                    "account", account, "param", rec.param);
                continue;
            }
        }

        // We use the maximum of our provided start time and a month-after-latest
        //  as the first time we'll input a reward.  Note that this can result
        //  in no rewards being given.
        Calendar startCal = Calendar.getInstance();
        if (latest != null) {
            startCal.setTime(latest);
            startCal.add(Calendar.MONTH, 1);
            if (startCal.getTime().before(start)) {
                startCal.setTime(start);
            }
        } else {
            startCal.setTime(start);
        }

        // Add in any appropriate rewards between the modified start and end.
        while (startCal.getTime().before(end)) {
            RewardRecord rr = new RewardRecord();
            rr.rewardId = rewardId;
            rr.account = account;
            rr.param = dateFormat.format(startCal.getTime());
            insert(rr);
            startCal.add(Calendar.MONTH, 1);
        }

        return true;
    }

    /**
     * Deactivates all rewards of the specified ID and account during the time window.
     *  Returns the number of rewards deleted.
     */
    public int deactivateMonthlyRewards (
        int rewardId, String account, java.util.Date start, java.util.Date end)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startStr = dateFormat.format(start);
        String endStr = dateFormat.format(end);

        return deleteAll(RewardRecord.class,
                new Where(Ops.and(
                        RewardRecord.REWARD_ID.eq(rewardId),
                        RewardRecord.ACCOUNT.eq(account),
                        RewardRecord.PARAM.greaterEq(startStr),
                        RewardRecord.PARAM.lessEq(endStr),
                        RewardRecord.REDEEMER_IDENT.isNull())));
    }

    /**
     * Returns all currently active rewards.
     */
    public List<RewardInfoRecord> loadActiveRewards ()
    {
        return findAll(RewardInfoRecord.class,
                new Where(RewardInfoRecord.EXPIRATION.greaterThan(
                        new Timestamp(System.currentTimeMillis()))));
    }

    /**
     * Returns all rewards.
     */
    public List<RewardInfoRecord> loadRewards ()
    {
        return findAll(RewardInfoRecord.class, OrderBy.descending(RewardInfoRecord.REWARD_ID));
    }

    /**
     * Return just the ids of all activated rewards for the specified account.
     */
    public List<Integer> loadActivatedRewardIds (String account)
    {
        return from(RewardRecord.class)
            .where(RewardRecord.ACCOUNT.eq(account))
            .select(RewardRecord.REWARD_ID);
    }

    /**
     * Returns all <code>RewardRecord</code>s that match the given account name and whose eligible
     * date is in the past. The returned list will be sorted by <code>rewardId</code>.
     */
    public List<RewardRecord> loadActivatedRewards (String account)
    {
        return findAll(RewardRecord.class,
                new Where(RewardRecord.ACCOUNT.eq(account)),
                OrderBy.ascending(RewardRecord.REWARD_ID));
    }

    /**
     * Load the redeemable reward records.
     */
    public List<RewardRecord> loadRedeemableRewards (String account)
    {
        return from(RewardRecord.class)
            .where(RewardRecord.ACCOUNT.eq(account), RewardRecord.REDEEMER_IDENT.isNull())
            .select();
    }

    /**
     * Returns all <code>RewardRecord</code>s that match either the account or redeemer identifier.
     * The returned list will be sorted by <code>rewardId</code>.
     */
    public List<RewardRecord> loadActivatedRewards (String account, String redeemerIdent)
    {
        return findAll(RewardRecord.class,
                new Where(Ops.or(RewardRecord.ACCOUNT.eq(account),
                        RewardRecord.REDEEMER_IDENT.eq(redeemerIdent))),
                OrderBy.ascending(RewardRecord.REWARD_ID).thenAscending(RewardRecord.PARAM));
    }

    /**
     * Returns all <code>RewardRecord</code>s for a specified <code>rewardId</code> that match
     * either the account or redeemer identifier.
     */
    public List<RewardRecord> loadActivatedReward (
        String account, String redeemerIdent, int rewardId)
    {
        return findAll(RewardRecord.class,
                new Where(Ops.and(RewardRecord.REWARD_ID.eq(rewardId),
                        Ops.or(RewardRecord.ACCOUNT.eq(account),
                            RewardRecord.REDEEMER_IDENT.eq(redeemerIdent)))));
    }

    /**
     * Returns all <code>RewardRecord</code>s for a specified <code>rewardId</code> and
     *  <code>param</code> that match either the account or redeemer identifier.
     */
    public List<RewardRecord> loadActivatedReward (
        String account, String redeemerIdent, int rewardId, String param)
    {
        SQLExpression<?> paramExp = param == null ?
            RewardRecord.PARAM.isNull() : RewardRecord.PARAM.eq(param);
        return findAll(RewardRecord.class,
                new Where(Ops.and(
                        RewardRecord.REWARD_ID.eq(rewardId),
                        paramExp,
                        Ops.or(
                            RewardRecord.ACCOUNT.eq(account),
                            RewardRecord.REDEEMER_IDENT.eq(redeemerIdent)))));
    }

    /**
     * Updates the supplied RewardRecord with the indicated <code>redeemerIdent</code> and store it
     * to the database.
     */
    public void redeemReward (RewardRecord record, String redeemerIdent)
    {
        record.redeemerIdent = redeemerIdent;
        update(record, RewardRecord.REDEEMER_IDENT);
    }

    /**
     * Summarizes the reward data and purges expired rewards.
     */
    public void purgeExpiredRewards ()
    {
        summarizeAndUpdate(null, RewardInfoRecord.ACTIVATIONS);
        summarizeAndUpdate(
                Ops.not(RewardRecord.REDEEMER_IDENT.isNull()), RewardInfoRecord.REDEMPTIONS);
        deleteAll(RewardRecord.class,
            KeySet.newKeySet(RewardRecord.class, findAllKeys(RewardRecord.class, true,
            new FromOverride(RewardRecord.class, RewardInfoRecord.class),
            new Where(Ops.and(
                    RewardRecord.REWARD_ID.eq(RewardInfoRecord.REWARD_ID),
                    RewardInfoRecord.EXPIRATION.lessEq(new Timestamp(System.currentTimeMillis()))
                )))));
    }

    /**
     * Summarizes the reward data and stores it in the reward info records.
     */
    protected void summarizeAndUpdate (SQLExpression<?> condition, ColumnExp<?> column)
    {
        SQLExpression<?> where = RewardInfoRecord.REWARD_ID.eq(RewardRecord.REWARD_ID);
        if (condition != null) {
            where = Ops.and(where, condition);
        }
        List<RewardCountRecord> counts = findAll(RewardCountRecord.class,
                new FromOverride(RewardInfoRecord.class, RewardRecord.class),
                new Where(where),
                new GroupBy(RewardInfoRecord.REWARD_ID));
        for (RewardCountRecord record : counts) {
            updatePartial(RewardInfoRecord.getKey(record.rewardId), column, record.count);
        }
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(RewardRecord.class);
        classes.add(RewardInfoRecord.class);
    }
}
