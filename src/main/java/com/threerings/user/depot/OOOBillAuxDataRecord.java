//
// $Id$

package com.threerings.user.depot;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.user.OOOBillAuxData;

/**
 * Emulates {@link OOOBillAuxData} for the Depot.
 */
@Entity(name="BILLAUXDATA")
public class OOOBillAuxDataRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OOOBillAuxDataRecord> _R = OOOBillAuxDataRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Timestamp> FIRST_COIN_BUY = colexp(_R, "firstCoinBuy");
    public static final ColumnExp<Timestamp> LATEST_COIN_BUY = colexp(_R, "latestCoinBuy");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The user's unique identifier. */
    @Id @Column(name="USER_ID")
    public int userId;

    /** The first time the user bought coins. */
    @Column(name="FIRST_COIN_BUY", nullable=true, defaultValue="NULL")
    public Timestamp firstCoinBuy;

    /** The most recent time the user bought coins. */
    @Column(name="LATEST_COIN_BUY", nullable=true, defaultValue="NULL")
    public Timestamp latestCoinBuy;

    /**
     * Creates a OOOBillAuxDataRecord from OOOBillAuxData.
     */
    public static OOOBillAuxDataRecord fromOOOBillAuxData (OOOBillAuxData aux)
    {
        OOOBillAuxDataRecord record = new OOOBillAuxDataRecord();
        record.userId = aux.userId;
        record.firstCoinBuy = aux.firstCoinBuy;
        record.latestCoinBuy = aux.latestCoinBuy;
        return record;
    }

    /**
     * Returns a OOOBillAuxData version of this record.
     */
    public OOOBillAuxData toOOOBillAuxData ()
    {
        OOOBillAuxData aux = new OOOBillAuxData();
        aux.userId = userId;
        aux.firstCoinBuy = firstCoinBuy;
        aux.latestCoinBuy = latestCoinBuy;
        return aux;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link OOOBillAuxDataRecord}
     * with the supplied key values.
     */
    public static Key<OOOBillAuxDataRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
