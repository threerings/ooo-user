//
// $Id$

package com.threerings.servlet.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.servlet.Site;

/**
 * Depot implements of a site identifier repository.
 */
@Singleton
public class SiteIdentifierRepository extends DepotRepository
{
    /**
     * Creates the repository.
     */
    @Inject public SiteIdentifierRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the sites.
     */
    public List<SiteRecord> loadSites ()
    {
        return findAll(SiteRecord.class);
    }

    /**
     * Loads the domains.
     */
    public List<DomainRecord> loadDomains ()
    {
        return findAll(DomainRecord.class);
    }

    /**
     * Adds a new site.
     */
    public Site insertNewSite (String siteString)
    {
        return insertNewSite(siteString, 0);
    }

    /**
     * Adds a new site.
     */
    public Site insertNewSite (String siteString, int siteId)
    {
        SiteRecord record = new SiteRecord();
        record.siteString = siteString;
        record.siteId = siteId;
        insert(record);
        return record.toSite();
    }

    /**
     * Adds a new domain.
     */
    public void insertNewDomain (String domain, int siteId)
    {
        DomainRecord record = new DomainRecord();
        record.domain = domain;
        record.siteId = siteId;
        insert(record);
    }

    @Override // documentation inherited
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(DomainRecord.class);
        classes.add(SiteRecord.class);
    }
}
