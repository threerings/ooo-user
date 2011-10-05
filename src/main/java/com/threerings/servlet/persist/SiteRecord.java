//
// $Id$

package com.threerings.servlet.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;

import com.samskivert.servlet.Site;
import com.samskivert.servlet.SiteIdentifier;

/**
 * Represents a site mapping known to a {@link SiteIdentifier}.
 */
@Entity(name="sites")
public class SiteRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SiteRecord> _R = SiteRecord.class;
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    public static final ColumnExp<String> SITE_STRING = colexp(_R, "siteString");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The site's unique identifier. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    public int siteId;

    /** The site's human readable identifier (i.e., "monkeybutter"). */
    @Column(length=24)
    public String siteString;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SiteRecord}
     * with the supplied key values.
     */
    public static Key<SiteRecord> getKey (int siteId)
    {
        return newKey(_R, siteId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SITE_ID); }
    // AUTO-GENERATED: METHODS END

    /**
     * Creates a Site from this record.
     */
    public Site toSite ()
    {
        Site site = new Site();
        site.siteId = siteId;
        site.siteString = siteString;
        return site;
    }
}
