//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.user.OOOAuxData;

/**
 * Emulates {@link OOOAuxData} for the Depot.
 */
@Entity(name="AUXDATA")
public class OOOAuxDataRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OOOAuxDataRecord> _R = OOOAuxDataRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Date> BIRTHDAY = colexp(_R, "birthday");
    public static final ColumnExp<Byte> GENDER = colexp(_R, "gender");
    public static final ColumnExp<String> MISSIVE = colexp(_R, "missive");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The user's unique identifier. */
    @Id @Column(name="USER_ID")
    public int userId;

    /** The user's birthday. */
    @Column(name="BIRTHDAY", defaultValue="'1900-01-01'")
    public Date birthday;

    /** The user's gender. */
    @Column(name="GENDER", defaultValue="0")
    public byte gender;

    /** The user's personal missive to us. */
    @Column(name="MISSIVE")
    public String missive;

    /**
     * Creates a OOOAuxDataRecord from a OOOAuxData.
     */
    public static OOOAuxDataRecord fromOOOAuxData (OOOAuxData aux)
    {
        OOOAuxDataRecord record = new OOOAuxDataRecord();
        record.userId = aux.userId;
        record.birthday = aux.birthday;
        record.gender = aux.gender;
        record.missive = aux.missive;
        return record;
    }

    /**
     * Returns a OOOAuxData version of this record.
     */
    public OOOAuxData toOOOAuxData ()
    {
        OOOAuxData aux = new OOOAuxData();
        aux.userId = userId;
        aux.birthday = birthday;
        aux.gender = gender;
        aux.missive = missive;
        return aux;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link OOOAuxDataRecord}
     * with the supplied key values.
     */
    public static Key<OOOAuxDataRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
