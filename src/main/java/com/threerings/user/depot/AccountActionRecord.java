//
// $Id$

package com.threerings.user.depot;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.user.AccountAction;

/**
 * Emulates {@link AccountAction} for the Depot.
 */
@Entity(name="ACCOUNT_ACTIONS")
public class AccountActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AccountActionRecord> _R = AccountActionRecord.class;
    public static final ColumnExp<Integer> ACTION_ID = colexp(_R, "actionId");
    public static final ColumnExp<String> ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp<Integer> ACTION = colexp(_R, "action");
    public static final ColumnExp<String> DATA = colexp(_R, "data");
    public static final ColumnExp<Timestamp> ENTERED = colexp(_R, "entered");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The action id. */
    @Id @Column(name="ACTION_ID") @GeneratedValue(strategy=GenerationType.AUTO)
    public int actionId;

    /** The account name. */
    @Column(name="ACCOUNT_NAME")
    public String accountName;

    /** The action that took place. */
    @Column(name="ACTION")
    public int action;

    /** Data that is interpreted depending on the action type. */
    @Column(name="DATA", length=65536, nullable=true)
    public String data;

    /** When the action took place. */
    @Column(name="ENTERED")
    public Timestamp entered;

    /**
     * Creates an AccountActionRecord from a AccountAction.
     */
    public static AccountActionRecord fromAccountAction (AccountAction aa)
    {
        AccountActionRecord record = new AccountActionRecord();
        record.actionId = aa.actionId;
        record.accountName = aa.accountName;
        record.action = aa.action;
        record.data = aa.data;
        record.entered = aa.entered;
        return record;
    }

    /**
     * Returns an AccountAction version of this record.
     */
    public AccountAction toAccountAction ()
    {
        AccountAction aa = new AccountAction();
        aa.actionId = actionId;
        aa.accountName = accountName;
        aa.action = action;
        aa.data = data;
        aa.entered = entered;
        return aa;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AccountActionRecord}
     * with the supplied key values.
     */
    public static Key<AccountActionRecord> getKey (int actionId)
    {
        return newKey(_R, actionId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ACTION_ID); }
    // AUTO-GENERATED: METHODS END
}
