//
// $Id$

/*
 * Copyright (C) 2007-2010 Three Rings Design, Inc.
 * Copyright 1999,2004 The Apache Software Foundation.
 */

package com.threerings.user;

/**
 * Utilities for dealing with affiliate tagging.
 */
public class AffiliateUtils {

    /** Request parameter containing integer affiliate site id. */
    public static final String AFFILIATE_ID_PARAMETER = "affiliate";
    
    /** before the days of affiliate, there was from, 
     * which also supports referrals from in-game users */
    public static final String FROM_PARAMETER = "from";

    /** Request parameter containing sub-affiliate tag; an arbitrary integer usable
     * by the partner to distinguish different placements on their site. */
    public static final String AFFILIATE_TAG_PARAMETER = "tag";

    /** Affiliate cookie name. 
     *  this cookie is also used for legacy personal referrals */
    public static final String AFFILIATE_ID_COOKIE = "site_id";
 
    /** Affiliate arbitrary tag cookie name. */
    public static final String AFFILIATE_TAG_COOKIE = "affiliate_tag";
    
    /** The name of the session attribute used to hold the affiliate id */
    public static final String AFFILIATE_ID_ATTRIBUTE = AffiliateUtils.class.getName() + ".AffiliateId";

    /** The name of the session attribute used to hold the affiliate tag */
    public static final String AFFILIATE_TAG_ATTRIBUTE = AffiliateUtils.class.getName() + ".AffiliateTag";
    
}
