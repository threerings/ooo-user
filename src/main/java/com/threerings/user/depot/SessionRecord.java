//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;

/**
 * Maintains information on authenticated HTTP sessions.
 */
@Entity(name="sessions")
public class SessionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SessionRecord> _R = SessionRecord.class;
    public static final ColumnExp<String> AUTHCODE = colexp(_R, "authcode");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Date> EXPIRES = colexp(_R, "expires");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** A unique code that identifies this session. */
    @Id @Column(length=32)
    public String authcode;

    /** The id of the user authenticated in this session. */
    @Index(name="userid_index")
    public int userId;

    /** The date on which the session expires. */
    @Index(name="expires_index")
    public Date expires;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SessionRecord}
     * with the supplied key values.
     */
    public static Key<SessionRecord> getKey (String authcode)
    {
        return newKey(_R, authcode);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(AUTHCODE); }
    // AUTO-GENERATED: METHODS END
}
