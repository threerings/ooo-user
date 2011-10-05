//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

/**
 * Used to determine the max userId.
 */
@Computed
public class MaxUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MaxUserRecord> _R = MaxUserRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    // AUTO-GENERATED: FIELDS END

    /** The user id. */
    @Computed
    public int userId;
}
