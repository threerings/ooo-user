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
 * A server that processes actions.
 */
@Entity(name="ACTION_SERVERS")
public class ActionServerRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ActionServerRecord> _R = ActionServerRecord.class;
    public static final ColumnExp<String> SERVER = colexp(_R, "server");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The server name. */
    @Id @Column(name="SERVER")
    public String server;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ActionServerRecord}
     * with the supplied key values.
     */
    public static Key<ActionServerRecord> getKey (String server)
    {
        return newKey(_R, server);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SERVER); }
    // AUTO-GENERATED: METHODS END
}
