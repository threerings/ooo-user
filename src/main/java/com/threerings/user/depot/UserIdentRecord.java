//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Extends the basic samskivert user record with special Three Rings business.
 */
@Entity(name="USER_IDENTS")
public class UserIdentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<UserIdentRecord> _R = UserIdentRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> MACH_IDENT = colexp(_R, "machIdent");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 4;

    /** The id of the user in question. */
    @Id @Column(name="USER_ID")
    public int userId;

    /** A 'unique' id for a specific machine we have seen the user come from. */
    @Id @Column(name="MACH_IDENT") @Index(name="ixMachIdent")
    public String machIdent;

    public UserIdentRecord ()
    {
        super();
    }

    public UserIdentRecord (int userId, String machIdent)
    {
        super();
        this.userId = userId;
        this.machIdent = machIdent;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link UserIdentRecord}
     * with the supplied key values.
     */
    public static Key<UserIdentRecord> getKey (int userId, String machIdent)
    {
        return newKey(_R, userId, machIdent);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, MACH_IDENT); }
    // AUTO-GENERATED: METHODS END
}
