//
// $Id$

package com.threerings.user.depot;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains a mapping from an arbitrary text tag to an integer identifier.
 */
@Entity(name="afftags")
public class AffiliateTagRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AffiliateTagRecord> _R = AffiliateTagRecord.class;
    public static final ColumnExp<Integer> TAG_ID = colexp(_R, "tagId");
    public static final ColumnExp<String> TAG = colexp(_R, "tag");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** Value indicating that the affiliate did not provide a tag **/
    public static final int NO_TAG = 0;

    /** The automatically generated tag id. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    public int tagId;

    /** The arbitrary text tag. */
    @Column(unique=true)
    public String tag;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AffiliateTagRecord}
     * with the supplied key values.
     */
    public static Key<AffiliateTagRecord> getKey (int tagId)
    {
        return newKey(_R, tagId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TAG_ID); }
    // AUTO-GENERATED: METHODS END
}
