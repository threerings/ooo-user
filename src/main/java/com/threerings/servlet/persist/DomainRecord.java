//
// $Id$

package com.threerings.servlet.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.servlet.SiteIdentifier;

/**
 * Represents a domain mapping known to a {@link SiteIdentifier}.
 */
@Entity(name="domains")
public class DomainRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DomainRecord> _R = DomainRecord.class;
    public static final ColumnExp<String> DOMAIN = colexp(_R, "domain");
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The domain. */
    @Id @Column(length=128)
    public String domain;

    /** The site identifier. */
    public int siteId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DomainRecord}
     * with the supplied key values.
     */
    public static Key<DomainRecord> getKey (String domain)
    {
        return newKey(_R, domain);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(DOMAIN); }
    // AUTO-GENERATED: METHODS END
}
