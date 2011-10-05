//
// $Id$

package com.threerings.user;

import static com.threerings.user.Log.log;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Cursor;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

/**
 * Maintains persistent reward information.
 */
public class RewardRepository extends JORARepository
{
    /**
     * Creates the repository with the specified connection provider.
     *
     * @exception PersistenceException thrown if an error occurs communicating with the underlying
     * persistence facilities.
     */
    public RewardRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, OOOUserRepository.USER_REPOSITORY_IDENT);
    }

    /**
     * Creates a new RewardInfo record in the database.  <code>info</code> should have the
     * <code>description</code>, <code>data</code> and <code>expiration</code> filled in.
     */
    public void createReward (RewardInfo info)
        throws PersistenceException
    {
        info.maxEligibleId = getMaxUserId();
        store(_infoTable, info);
    }

    /**
     * Immediately expires a reward by setting the expiration to the current date then making a
     * call to <code>purgeExpiredRewards</code>.
     */
    public void expireReward (int rewardId)
        throws PersistenceException
    {
        RewardInfo info = new RewardInfo();
        info.rewardId = rewardId;
        info.expiration = new Date(System.currentTimeMillis());
        updateField(_infoTable, info, "expiration");
        purgeExpiredRewards();
    }

    /**
     * Attempt to activate the reward for an account.  If this account has not already activated
     * the reward, a new RewardRecord will be created.  Returns true if a new RewardRecord is
     * created, false otherwise.
     */
    public boolean activateReward (int rewardId, String account)
        throws PersistenceException
    {
        return activateReward(rewardId, account, null);
    }

    public boolean activateReward (int rewardId, String account, String param)
        throws PersistenceException
    {
        final RewardRecord rr = new RewardRecord();
        rr.rewardId = rewardId;
        rr.account = account;
        rr.param = param == null ? "" : param;
        // try to insert it; catching duplicate row exceptions and reporting that the reward is
        // already activated
        return executeUpdate(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                try {
                    _rewardsTable.insert(conn, rr);
                    return true;
                } catch (SQLException sqe) {
                    if (liaison.isDuplicateRowException(sqe)) {
                        return false;
                    } else {
                        throw sqe;
                    }
                }
            }
        });
    }

    /**
     * Activates monthly rewards of the specified ID for the account between start and end.  If they
     *  already have monthly rewards for this ID, will add new ones only at appropriate monthly
     *  intervals from the existing ones.
     */
    public boolean activateMonthlyRewards (final int rewardId, final String account,
        final java.util.Date start, final java.util.Date end)
        throws PersistenceException
    {
        // Setup an example to find relevant rewards.
        final RewardRecord example = new RewardRecord();
        example.rewardId = rewardId;
        example.account = account;
        final FieldMask mask = _rewardsTable.getFieldMask();
        mask.setModified("rewardId");
        mask.setModified("account");

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return executeUpdate(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                // Find the most recent reward this account already has of this type.
                java.util.Date latest = null;
                Cursor<RewardRecord> matches =
                    _rewardsTable.queryByExample(conn, example, mask);
                RewardRecord rec;
                while ((rec = matches.next()) != null) {
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
                    final RewardRecord rr = new RewardRecord();
                    rr.rewardId = rewardId;
                    rr.account = account;
                    rr.param = dateFormat.format(startCal.getTime());
                    _rewardsTable.insert(conn, rr);
                    startCal.add(Calendar.MONTH, 1);
                }

                return true;
            }
        });

    }

    /**
     * Deactivates all rewards of the specified ID and account during the time window.
     *  Returns the number of rewards deleted.
     */
    public int deactivateMonthlyRewards (final int rewardId, final String account,
        final java.util.Date start, final java.util.Date end)
        throws PersistenceException
    {
        return executeUpdate(new Operation<Integer>() {
            public Integer invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String delQuery = "delete from REWARD_RECORDS where REWARD_ID = ? and ACCOUNT = ?" +
                    " and PARAM >= ? and PARAM <= ? and REDEEMER_IDENT is null";

                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String startStr = dateFormat.format(start);
                String endStr = dateFormat.format(end);

                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(delQuery);
                    stmt.setInt(1, rewardId);
                    stmt.setString(2, account);
                    stmt.setString(3, startStr);
                    stmt.setString(4, endStr);
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Returns all currently active rewards.
     */
    public List<RewardInfo> loadActiveRewards ()
        throws PersistenceException
    {
        return loadAll(_infoTable, "where EXPIRATION > NOW()");
    }

    /**
     * Returns all rewards.
     */
    public List<RewardInfo> loadRewards ()
        throws PersistenceException
    {
        return loadAll(_infoTable, "order by REWARD_ID desc");
    }

    /**
     * Returns all <code>RewardRecord</code>s that match the given account name and whose eligible
     * date is in the past. The returned list will be sorted by <code>rewardId</code>.
     */
    public List<RewardRecord> loadActivatedRewards (String account)
        throws PersistenceException
    {
        String condition = "where ACCOUNT = " + JDBCUtil.escape(account) +
            " order by REWARD_ID";
        return loadAll(_rewardsTable, condition);
    }

    /**
     * Returns all <code>RewardRecord</code>s that match either the account or redeemer identifier.
     * The returned list will be sorted by <code>rewardId</code>.
     */
    public List<RewardRecord> loadActivatedRewards (String account, String redeemerIdent)
        throws PersistenceException
    {
        String condition = "where ACCOUNT = " + JDBCUtil.escape(account) +
            " or REDEEMER_IDENT = " + JDBCUtil.escape(redeemerIdent) +
            " order by REWARD_ID, PARAM";
        return loadAll(_rewardsTable, condition);
    }

    /**
     * Returns all <code>RewardRecord</code>s for a specified <code>rewardId</code> that match
     * either the account or redeemer identifier.
     */
    public List<RewardRecord> loadActivatedReward (
        String account, String redeemerIdent, int rewardId)
        throws PersistenceException
    {
        String condition = "where REWARD_ID = " + rewardId +
            " and (ACCOUNT = " + JDBCUtil.escape(account) +
            " or REDEEMER_IDENT = " + JDBCUtil.escape(redeemerIdent) + ")";
        return loadAll(_rewardsTable, condition);
    }

    /**
     * Returns all <code>RewardRecord</code>s for a specified <code>rewardId</code> and
     *  <code>param</code> that match either the account or redeemer identifier.
     */
    public List<RewardRecord> loadActivatedReward (
        String account, String redeemerIdent, int rewardId, String param)
        throws PersistenceException
    {
        String paramStr = (param == null) ? "PARAM is NULL" : "PARAM = " + JDBCUtil.escape(param);
        String condition = "where REWARD_ID = " + rewardId +
            " and " + paramStr +
            " and (ACCOUNT = " + JDBCUtil.escape(account) +
            " or REDEEMER_IDENT = " + JDBCUtil.escape(redeemerIdent) + ")";
        return loadAll(_rewardsTable, condition);
    }

    /**
     * Updates the supplied RewardRecord with the indicated <code>redeemerIdent</code> and store it
     * to the database.
     */
    public boolean redeemReward (RewardRecord record, String redeemerIdent)
        throws PersistenceException
    {
        String update = "update " + _rewardsTable.getName() + " set REDEEMER_IDENT = " +
            JDBCUtil.escape(redeemerIdent) + " where REDEEMER_IDENT is NULL and REWARD_ID = " +
            record.rewardId + " and ACCOUNT = " + JDBCUtil.escape(record.account) +
            " and PARAM = " + JDBCUtil.escape(record.param);
        return update(update) > 0;
    }

    /**
     * Summarizes the reward data and purges expired rewards.
     */
    public void purgeExpiredRewards ()
        throws PersistenceException
    {
        executeUpdate(new Operation<Object> () {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                summarizeAndUpdate(conn, "", "ACTIVATIONS");
                summarizeAndUpdate(conn, "and REDEEMER_IDENT is NOT NULL", "REDEMPTIONS");
                return null;
            }
        });

        update("delete from REWARD_RECORDS using REWARD_RECORDS, REWARD_INFO " +
               "where REWARD_INFO.REWARD_ID = REWARD_RECORDS.REWARD_ID " +
               "and REWARD_INFO.EXPIRATION <= NOW()");
    }

    /**
     * Summarizes the reward data and stores it in the reward info records.
     */
    protected void summarizeAndUpdate (Connection conn, String condition, String column)
        throws PersistenceException, SQLException
    {
        String activatedQuery = "select count(*), REWARD_INFO.REWARD_ID " +
            "from REWARD_RECORDS, REWARD_INFO " +
            "where REWARD_INFO.REWARD_ID = REWARD_RECORDS.REWARD_ID " + condition +
            " group by REWARD_ID";
        String activatedUpdate = "update REWARD_INFO set " + column + " = ? where REWARD_ID = ?";
        PreparedStatement ustmt = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(activatedQuery);
            ustmt = conn.prepareStatement(activatedUpdate);
            while (rs.next()) {
                ustmt.setInt(1, rs.getInt(1));
                ustmt.setInt(2, rs.getInt(2));
                ustmt.executeUpdate();
            }
        } finally {
            JDBCUtil.close(ustmt);
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Returns the max userid currently in use.
     */
    protected int getMaxUserId ()
        throws PersistenceException
    {
        return execute(new Operation<Integer> () {
            public Integer invoke (Connection conn, DatabaseLiaison liaison)
                throws PersistenceException, SQLException
            {
                String query = "select MAX(userId) from users";
                Statement stmt = null;
                int maxUserId = 0;

                try {
                    stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    if (rs.next()) {
                        maxUserId = rs.getInt(1);
                    } else {
                        log.warning("No users found!?");
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }

                return maxUserId;
            }
        });
    }

    @Override
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        String[] rewardInfoTable = {
            "REWARD_ID INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY",
            "DESCRIPTION VARCHAR(255) NOT NULL",
            "COUPON_CODE VARCHAR(255)",
            "DATA VARCHAR(255) NOT NULL",
            "EXPIRATION DATE NOT NULL",
            "MAX_ELIGIBLE_ID INTEGER NOT NULL",
            "ACTIVATIONS INTEGER NOT NULL",
            "REDEMPTIONS INTEGER NOT NULL"
        };
        JDBCUtil.createTableIfMissing(conn, "REWARD_INFO", rewardInfoTable, "");

        // TEMP: (2006-12-22) add COUPON_CODE column
        JDBCUtil.addColumn(conn, "REWARD_INFO", "COUPON_CODE", "VARCHAR(255)", "DESCRIPTION");
        // END TEMP

        String[] rewardsTable = {
            "REWARD_ID INTEGER NOT NULL",
            "ACCOUNT VARCHAR(255)",
            "REDEEMER_IDENT VARCHAR(64)",
            "PARAM VARCHAR(255)",
            "PRIMARY KEY (REWARD_ID, ACCOUNT, PARAM)",
            "INDEX (ACCOUNT)",
            "INDEX (REDEEMER_IDENT)"
        };
        JDBCUtil.createTableIfMissing(conn, "REWARD_RECORDS", rewardsTable, "");

        // Add a parameter column. This allows a particular reward to be granted more than once, and
        // pass specific details to the reward handler.
        if (!JDBCUtil.tableContainsColumn(conn, "REWARD_RECORDS", "PARAM")) {
            // unfortunately, we need to recreate the primary key
            JDBCUtil.dropPrimaryKey(conn, "REWARD_RECORDS");

            // add generic parameter column, and include
            JDBCUtil.addColumn(conn, "REWARD_RECORDS", "PARAM", "VARCHAR(255), " +
                "ADD PRIMARY KEY (REWARD_ID, ACCOUNT, PARAM)", null);
        }
    }

    @Override
    protected void createTables ()
    {
        _infoTable = new Table<RewardInfo>(
            RewardInfo.class, "REWARD_INFO", REWARD_INFO_PRIMARY_KEY, true);
        _rewardsTable = new Table<RewardRecord>(
            RewardRecord.class, "REWARD_RECORDS", REWARD_RECORDS_PRIMARY_KEYS, true);
    }

    /** The primary key for the reward info table. */
    protected static final String REWARD_INFO_PRIMARY_KEY = "REWARD_ID";

    /** The primary keys for the rewards table. */
    protected static final String[] REWARD_RECORDS_PRIMARY_KEYS = {
        "REWARD_ID", "ACCOUNT", "PARAM"
    };

    /** The table used to store reward information. */
    protected Table<RewardInfo> _infoTable;

    /** The table used to store activated rewards. */
    protected Table<RewardRecord> _rewardsTable;
}
