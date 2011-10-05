//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maps a multiple invitation record to a user.
 */
@Entity
public class UserInvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<UserInvitationRecord> _R = UserInvitationRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> INVITATION_ID = colexp(_R, "invitationId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The user id. */
    @Id
    public int userId;

    /** The invitation id. */
    @Index
    public int invitationId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link UserInvitationRecord}
     * with the supplied key values.
     */
    public static Key<UserInvitationRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
