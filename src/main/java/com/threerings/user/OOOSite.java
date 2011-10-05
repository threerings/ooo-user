//
// $Id$

package com.threerings.user;

import com.samskivert.servlet.Site;

/**
 * Used to identify OOO sites.
 */
public class OOOSite extends Site
{
    /** The domain at which this site is found, e.g. puzzlepirates.com */
    public final String domain;

    public OOOSite (int siteId, String siteString, String domain)
    {
        super(siteId, siteString);
        this.domain = domain;
    }
}
