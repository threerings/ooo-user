//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Represents a row in the TAINTED_IDENTS table, Depot version.
 */
@Entity(name="TAINTED_IDENTS")
public class TaintedIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TaintedIdentRecord> _R = TaintedIdentRecord.class;
    public static final ColumnExp<String> MACH_IDENT = colexp(_R, "machIdent");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** A 'unique' id for a specific machine we have seen. */
    @Id @Column(name="MACH_IDENT")
    public String machIdent;

    /** Blank constructor for the unserialization business. */
    public TaintedIdentRecord ()
    {
    }

    /** A constructor that populates this record. */
    public TaintedIdentRecord (String machIdent)
    {
        this.machIdent = machIdent;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TaintedIdentRecord}
     * with the supplied key values.
     */
    public static Key<TaintedIdentRecord> getKey (String machIdent)
    {
        return newKey(_R, machIdent);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MACH_IDENT); }
    // AUTO-GENERATED: METHODS END
}
