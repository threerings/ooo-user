//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.user.DetailedUser;

/**
 * A computed persistent entity that loads detailed user information.
 */
@Computed
@Entity
public class DetailedUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DetailedUserRecord> _R = DetailedUserRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> USERNAME = colexp(_R, "username");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<String> EMAIL = colexp(_R, "email");
    public static final ColumnExp<Date> BIRTHDAY = colexp(_R, "birthday");
    public static final ColumnExp<Byte> GENDER = colexp(_R, "gender");
    public static final ColumnExp<String> MISSIVE = colexp(_R, "missive");
    // AUTO-GENERATED: FIELDS END

    /** The user's assigned userid. */
    @Id @Computed(shadowOf=OOOUserRecord.class)
    public int userId;

    /** The user's chosen username. */
    @Computed(shadowOf=OOOUserRecord.class)
    public String username;

    /** The date this record was created. */
    @Computed(shadowOf=OOOUserRecord.class)
    public Date created;

    /** The user's email address. */
    @Computed(shadowOf=OOOUserRecord.class)
    public String email;

    /** The user's birthday. */
    @Column(name="BIRTHDAY") @Computed(shadowOf=OOOAuxDataRecord.class)
    public Date birthday;

    /** The user's gender. */
    @Column(name="GENDER") @Computed(shadowOf=OOOAuxDataRecord.class)
    public byte gender;

    /** The user's personal missive to us. */
    @Column(name="MISSIVE") @Computed(shadowOf=OOOAuxDataRecord.class)
    public String missive;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DetailedUserRecord}
     * with the supplied key values.
     */
    public static Key<DetailedUserRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END

    /**
     * Creates a DetailedUser from the record.
     */
    public DetailedUser toDetailedUser ()
    {
        DetailedUser user = new DetailedUser();
        user.userId = userId;
        user.username = username;
        user.created = created;
        user.email = email;
        user.birthday = birthday;
        user.gender = gender;
        user.missive = missive;
        return user;
    }
}
