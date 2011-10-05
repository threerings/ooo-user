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
 * Represents a row in the BANNED_IDENTS table, Depot version.
 */
@Entity(name="BANNED_IDENTS")
public class BannedIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BannedIdentRecord> _R = BannedIdentRecord.class;
    public static final ColumnExp<String> MACH_IDENT = colexp(_R, "machIdent");
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** a 'unique' id for the specific machine we have seen. */
    @Id @Column(name="MACH_IDENT")
    public String machIdent;

    /** The site if which this machine is banned from. */
    @Id @Column(name="SITE_ID")
    public int siteId;

    /** Blank constructor for the unserialization business. */
    public BannedIdentRecord ()
    {
    }

    /** A constructor that populates this record. */
    public BannedIdentRecord (String machIdent, int siteId)
    {
        this.machIdent = machIdent;
        this.siteId = siteId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BannedIdentRecord}
     * with the supplied key values.
     */
    public static Key<BannedIdentRecord> getKey (String machIdent, int siteId)
    {
        return newKey(_R, machIdent, siteId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MACH_IDENT, SITE_ID); }
    // AUTO-GENERATED: METHODS END
}
