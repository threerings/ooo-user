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
 * Actions that have been processed by a server.
 */
@Entity(name="PROCESSED_ACTIONS")
public class ProcessedActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ProcessedActionRecord> _R = ProcessedActionRecord.class;
    public static final ColumnExp<Integer> ACTION_ID = colexp(_R, "actionId");
    public static final ColumnExp<String> SERVER = colexp(_R, "server");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The action id. */
    @Id @Column(name="ACTION_ID")
    public int actionId;

    /** The name of the server that has processed the action. */
    @Id @Column(name="SERVER")
    public String server;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ProcessedActionRecord}
     * with the supplied key values.
     */
    public static Key<ProcessedActionRecord> getKey (int actionId, String server)
    {
        return newKey(_R, actionId, server);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ACTION_ID, SERVER); }
    // AUTO-GENERATED: METHODS END
}
