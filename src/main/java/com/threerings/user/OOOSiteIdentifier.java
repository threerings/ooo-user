//
// $Id$

package com.threerings.user;

import java.util.Iterator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.Site;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.util.StringUtil;

import static com.threerings.user.Log.log;

/**
 * Handles the identification of OOO sites without requiring us to maintain a domains everywhere we
 * use our webapps. It still uses the "sites" table for affiliate handling, so we can set up
 * affiliates using the register webapp and whatnot, but it uses a static mapping (defined in
 * OOOUser) for our domains and site ids.
 */
public class OOOSiteIdentifier extends JDBCTableSiteIdentifier
{
    /** A request property that can be used to override the domain-based site identification. See
     * {@link HttpServletRequest#setAttribute}. */
    public static final String SITE_ID_OVERRIDE_KEY = "SiteIdentifierOverride";

    /** A cookie that can be provided with a request to override the domain-based site
     * identification. This is superceded by {@link #SITE_ID_OVERRIDE_KEY}. */
    public static final String SITE_COOKIE = "site";

    public OOOSiteIdentifier (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }

    @Override // from JDBCTableSiteIdentifier
    public int identifySite (HttpServletRequest req)
    {
        // look for request parameter, accept it as an override and store for the session
        String requestParam = req.getParameter(SITE_ID_OVERRIDE_KEY);
        if (requestParam != null) {
            try {
                Integer siteId = Integer.parseInt(requestParam);
                req.getSession().setAttribute(SITE_ID_OVERRIDE_KEY, siteId);
                return siteId;
            } catch (Exception e) {
                log.warning("Received invalid site override param", "site", requestParam);
                // fall through to other methods
            }
        }

        // check for override in the request attributes and store in the session
        Integer attributeOverride = (Integer)req.getAttribute(SITE_ID_OVERRIDE_KEY);
        if (attributeOverride != null) {
            req.getSession().setAttribute(SITE_ID_OVERRIDE_KEY, attributeOverride);
            return attributeOverride;
        }

        // check whether our site id was overridden during this session
        Integer override = (Integer)req.getSession().getAttribute(SITE_ID_OVERRIDE_KEY);
        if (override != null) {
            return override;
        }

        // check for a site cookie
        String sitecookie = CookieUtil.getCookieValue(req, SITE_COOKIE);
        if (!StringUtil.isBlank(sitecookie)) {
            try {
                return Integer.parseInt(sitecookie);
            } catch (Exception e) {
                log.warning("Received invalid site cookie", "site", sitecookie);
                // fall through to the domain parsing
            }
        }

        // otherwise we just use a static mapping
        String serverName = req.getServerName();
        for (OOOSite site : OOOUser.SITES) {
            if (serverName.endsWith(site.domain)) {
                return site.siteId;
            }
        }
        return super.identifySite(req);
    }

    @Override // from JDBCTableSiteIdentifier
    public String getSiteString (int siteId)
    {
        for (OOOSite site : OOOUser.SITES) {
            if (site.siteId == siteId) {
                return site.siteString;
            }
        }
        return super.getSiteString(siteId);
    }

    @Override // from JDBCTableSiteIdentifier
    public int getSiteId (String siteString)
    {
        for (OOOSite site : OOOUser.SITES) {
            if (site.siteString.equals(siteString)) {
                return site.siteId;
            }
        }
        return super.getSiteId(siteString);
    }

    @Override // from JDBCTableSiteIdentifier
    public Iterator<Site> enumerateSites ()
    {
        List<Site> sites = Lists.newArrayList(super.enumerateSites());
        for (OOOSite site : OOOUser.SITES) {
            if (!_sitesById.containsKey(site.siteId)) {
                sites.add(site);
            }
        }
        return sites.iterator();
    }
}
