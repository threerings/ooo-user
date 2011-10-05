//
// $Id$

package com.threerings.servlet;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.servlet.Site;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.util.ArrayUtil;

import com.threerings.servlet.persist.DomainRecord;
import com.threerings.servlet.persist.SiteIdentifierRepository;
import com.threerings.servlet.persist.SiteRecord;

/**
 * Accomplishes the process of site identification based on a mapping from domains (e.g.
 * samskivert.com) to site identifiers that is maintained in a depot database table.
 *
 * <p> There are two tables, one that maps domains to site identifiers and another that maps site
 * identifiers to site strings. These are both loaded at construct time and refreshed periodically
 * in the course of normal operation.
 *
 * <p> Note that any of the calls to identify, lookup or enumerate site information can result in
 * the sites table being refreshed from the database which will take relatively much longer than
 * the simple hashtable lookup that the operations normally require. However, this happens only
 * once every 15 minutes and the circumstances in which the site identifier are normally used can
 * generally accomodate the extra 100 milliseconds or so that it is likely to take to reload the
 * (tiny) sites and domains tables from the database.
 */
public class DepotSiteIdentifier
    implements SiteIdentifier
{
    /**
     * Constructs a debot site identifier.
     */
    public DepotSiteIdentifier (PersistenceContext pctx)
    {
        this(pctx, DEFAULT_SITE_ID);
    }

    /**
     * Creates an identifier that will load data from the supplied connection provider and which
     * will use the supplied default site id instead of {@link #DEFAULT_SITE_ID}.
     */
    public DepotSiteIdentifier (PersistenceContext pctx, int defaultSiteId)
    {
        this(pctx, defaultSiteId, DEFAULT_SITE_STRING);
    }

    /**
     * Creates an identifier that will load data from the supplied connection provider and which
     * will use the supplied default site id instead of {@link #DEFAULT_SITE_ID} and the supplied
     * default site string instead of {@link #DEFAULT_SITE_STRING}.
     */
    public DepotSiteIdentifier (
            PersistenceContext pctx, int defaultSiteId, String defaultSiteString)
    {
        _repo = new SiteIdentifierRepository(pctx);
        refreshSiteData();
        _defaultSiteId = defaultSiteId;
        _defaultSiteString = defaultSiteString;
    }

    // documentation inherited
    public int identifySite (HttpServletRequest req)
    {
        checkReloadSites();
        String serverName = req.getServerName();

        // scan for the mapping that matches the specified domain
        int msize = _mappings.size();
        for (int i = 0; i < msize; i++) {
            SiteMapping mapping = _mappings.get(i);
            if (serverName.endsWith(mapping.domain)) {
                return mapping.siteId;
            }
        }

        // if we matched nothing, return the default id
        return _defaultSiteId;
    }

    // documentation inherited
    public String getSiteString (int siteId)
    {
        checkReloadSites();
        Site site = _sitesById.get(siteId);
        if (site == null) {
            site = _sitesById.get(_defaultSiteId);
        }
        return (site == null) ? _defaultSiteString : site.siteString;
    }

    // documentation inherited
    public int getSiteId (String siteString)
    {
        checkReloadSites();
        Site site = _sitesByString.get(siteString);
        return (site == null) ? _defaultSiteId : site.siteId;
    }

    // documentation inherited from interface
    public Iterator<Site> enumerateSites ()
    {
        checkReloadSites();
        return _sitesById.values().iterator();
    }

    /**
     * Insert a new site into the site table and into this mapping.
     */
    public Site insertNewSite (String siteString)
    {
        return insertNewSite(siteString, 0);
    }

    /**
     * Insert a new site into the site table and into this mapping.
     */
    public Site insertNewSite (String siteString, int siteId)
    {
        if (_sitesByString.containsKey(siteString) ||
                (siteId > 0 && _sitesById.containsKey(siteId))) {
            return null;
        }

        // add it to the db
        Site site = _repo.insertNewSite(siteString, siteId);

        // add it to our two mapping tables, taking care to avoid causing enumerateSites() to choke
        Map<String, Site> newStrings = Maps.newHashMap(_sitesByString);
        Map<Integer, Site> newIds = Maps.newHashMap(_sitesById);
        newIds.put(site.siteId, site);
        newStrings.put(site.siteString, site);
        _sitesByString = newStrings;
        _sitesById = newIds;

        return site;
    }

    /**
     * Insert a new domain into the domain table and into this mapping.
     */
    public void insertNewDomain (String domain, int siteId)
    {
        // scan for the mapping that matches the specified domain
        int msize = _mappings.size();
        for (int i = 0; i < msize; i++) {
            SiteMapping mapping = _mappings.get(i);
            if (mapping.domain.equals(domain)) {
                return;
            }
        }

        // add it to the db
        _repo.insertNewDomain(domain, siteId);

        // add it to our two mapping table, taking care to avoid causing enumerateSites() to choke
        List<SiteMapping> mappings = Lists.newArrayList();
        mappings.addAll(_mappings);
        mappings.add(new SiteMapping(siteId, domain));

        Collections.sort(mappings, SiteMapping.BY_SPECIFICITY);
        _mappings = mappings;
    }

    /**
     * Checks to see if we should reload our sites information from the sites table.
     */
    protected void checkReloadSites ()
    {
        long now = System.currentTimeMillis();
        boolean reload = false;
        synchronized (this) {
            reload = (now - _lastReload > RELOAD_INTERVAL);
            if (reload) {
                _lastReload = now;
            }
        }
        if (reload) {
            refreshSiteData();
        }
    }

    /**
     * Refreshes the cached site information.
     */
    public void refreshSiteData ()
    {
        List<SiteRecord> siteRecords = _repo.loadSites();
        Map<Integer,Site> sites = Maps.newHashMap();
        Map<String,Site> strings = Maps.newHashMap();
        for (SiteRecord record : siteRecords) {
            Site site = record.toSite();
            sites.put(record.siteId, site);
            strings.put(record.siteString, site);
        }
        _sitesById = sites;
        _sitesByString = strings;

        List<DomainRecord> domainRecords = _repo.loadDomains();
        List<SiteMapping> mappings = Lists.newArrayList();
        for (DomainRecord record : domainRecords) {
            mappings.add(new SiteMapping(record.siteId, record.domain));
        }

        Collections.sort(mappings, SiteMapping.BY_SPECIFICITY);
        _mappings = mappings;
    }

    /**
     * Used to track domain to site identifier mappings.
     */
    protected static class SiteMapping
    {
        /**
         * Sorts site mappings from most specific (www.yahoo.com) to least specific (yahoo.com).
         */
        public static final Comparator<SiteMapping> BY_SPECIFICITY = new Comparator<SiteMapping>() {
            public int compare (SiteMapping one, SiteMapping two) {
                return one._rdomain.compareTo(two._rdomain);
            }
        };

        /** The domain to match. */
        public String domain;

        /** The site identifier for the associated domain. */
        public int siteId;

        public SiteMapping (int siteId, String domain) {
            this.siteId = siteId;
            this.domain = domain;
            byte[] bytes = domain.getBytes();
            ArrayUtil.reverse(bytes);
            _rdomain = new String(bytes);
        }

        @Override // from Object
        public String toString () {
            return "[" + domain + " => " + siteId + "]";
        }

        protected String _rdomain;
    }

    /** The repository through which we load up site identifier information. */
    protected SiteIdentifierRepository _repo;

    /** The site id to return if we cannot identify the site from our table data. */
    protected int _defaultSiteId;

    /** The site string to return if we cannot identify the site from our table data. */
    protected String _defaultSiteString;

    /** The list of domain to site identifier mappings ordered from most specific domain to least
     * specific. */
    protected volatile List<SiteMapping> _mappings = Lists.newArrayList();

    /** The mapping from integer site identifiers to string site identifiers. */
    protected volatile Map<Integer, Site> _sitesById = Maps.newHashMap();

    /** The mapping from string site identifiers to integer site identifiers. */
    protected volatile Map<String, Site> _sitesByString = Maps.newHashMap();

    /** Used to periodically reload our site data. */
    protected long _lastReload;

    /** Reload our site data every 15 minutes. */
    protected static final long RELOAD_INTERVAL = 15 * 60 * 1000L;
}

