//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * An invitation record.
 */
@Entity
public class InvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<InvitationRecord> _R = InvitationRecord.class;
    public static final ColumnExp<String> INVITATION = colexp(_R, "invitation");
    public static final ColumnExp<String> EMAIL = colexp(_R, "email");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> INVITER_USER_ID = colexp(_R, "inviterUserId");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Date> SENT = colexp(_R, "sent");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 4;

    /** The invitation key. */
    @Id @Column(length=32)
    public String invitation;

    /** The e-mail address this invitation was sent to. */
    public String email;

    /** The account activated with this invitation. */
    public int userId;

    /** The account used to generate the invitation. */
    @Index
    public int inviterUserId;

    /** The date this invitation was created. */
    public Date created;

    /** The date the last e-mail was sent. */
    public Date sent;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link InvitationRecord}
     * with the supplied key values.
     */
    public static Key<InvitationRecord> getKey (String invitation)
    {
        return newKey(_R, invitation);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(INVITATION); }
    // AUTO-GENERATED: METHODS END
}
