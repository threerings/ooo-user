//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.user.ExternalAuther;

/**
 * Maintains information on a mapping from an external authentication source to a OOOUser.
 */
public class ExternalAuthRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ExternalAuthRecord> _R = ExternalAuthRecord.class;
    public static final ColumnExp<ExternalAuther> AUTHER = colexp(_R, "auther");
    public static final ColumnExp<String> EXTERNAL_ID = colexp(_R, "externalId");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> SESSION_KEY = colexp(_R, "sessionKey");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The auther that maintains the external user id. */
    @Id public ExternalAuther auther;

    /** The external user identifier. */
    @Id public String externalId;

    /** The id of the OOOUser account associated with the specified external account. */
    @Index(name="ixUserId")
    public int userId;

    /** The most recent session key provided by the external site, for use in making API requests
     * to said site based on our most recently active session. */
    @Column(nullable=true, length=1024)
    public String sessionKey;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ExternalAuthRecord}
     * with the supplied key values.
     */
    public static Key<ExternalAuthRecord> getKey (ExternalAuther auther, String externalId)
    {
        return newKey(_R, auther, externalId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(AUTHER, EXTERNAL_ID); }
    // AUTO-GENERATED: METHODS END
}
