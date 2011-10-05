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

import com.threerings.user.ValidateRecord;

/**
 * Emulates {@link ValidateRecord} for the Depot.
 */
@Entity(name="penders")
public class ValidateDepotRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ValidateDepotRecord> _R = ValidateDepotRecord.class;
    public static final ColumnExp<String> SECRET = colexp(_R, "secret");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Boolean> PERSIST = colexp(_R, "persist");
    public static final ColumnExp<Date> INSERTED = colexp(_R, "inserted");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The secret key. */
    @Id @Column(length=32)
    public String secret;

    /** The user id. */
    public int userId;

    /** The persist? */
    public boolean persist;

    /** The date inserted. */
    public Date inserted;

    /**
     * Creates a ValidateDepotRecord from a ValidateRecord.
     */
    public static ValidateDepotRecord fromValidateRecord (ValidateRecord vr)
    {
        ValidateDepotRecord record = new ValidateDepotRecord();
        record.secret = vr.secret;
        record.userId = vr.userId;
        record.persist = vr.persist;
        record.inserted = vr.inserted;
        return record;
    }

    /**
     * Returns a ValidateRecord version of this record.
     */
    public ValidateRecord toValidateRecord ()
    {
        ValidateRecord record = new ValidateRecord();
        record.secret = secret;
        record.userId = userId;
        record.persist = persist;
        record.inserted = inserted;
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ValidateDepotRecord}
     * with the supplied key values.
     */
    public static Key<ValidateDepotRecord> getKey (String secret)
    {
        return newKey(_R, secret);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SECRET); }
    // AUTO-GENERATED: METHODS END
}
