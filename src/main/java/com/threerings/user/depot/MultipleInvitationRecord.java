//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * A multiple invitation record.
 */
@Entity
public class MultipleInvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MultipleInvitationRecord> _R = MultipleInvitationRecord.class;
    public static final ColumnExp<Integer> INVITATION_ID = colexp(_R, "invitationId");
    public static final ColumnExp<String> INVITATION = colexp(_R, "invitation");
    public static final ColumnExp<Integer> MAX_INVITATIONS = colexp(_R, "maxInvitations");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The invitation id. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    public int invitationId;

    /** The invitation code. */
    @Column(length=32, unique=true)
    public String invitation;

    /** The max number of accounts that can be created with this invitation. */
    public int maxInvitations;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MultipleInvitationRecord}
     * with the supplied key values.
     */
    public static Key<MultipleInvitationRecord> getKey (int invitationId)
    {
        return newKey(_R, invitationId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(INVITATION_ID); }
    // AUTO-GENERATED: METHODS END
}
