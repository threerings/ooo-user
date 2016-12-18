//
// $Id$

/*
 * Copyright (C) 2007-2010 Three Rings Design, Inc.
 * Copyright 1999,2004 The Apache Software Foundation.
 */

package com.threerings.user;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.util.Config;

/**
 * Encapsulates multiple repositories, and offers public access to all of them.
 */
public class RepositoryManager {

    /** The repo manager will be stored in context under this attribute name */
    public static final String ATTRIBUTE_NAME = RepositoryManager.class.getName();

    /** Our User Manager */
    public OOOUserManager userRepository;

    /** Our referral repository. */
    public ReferralRepository referralRepository;

    /** Our site identification repository. */
    public SiteIdentifier siteRepository;

    /** For logging tracking events */
    public TrackingRepository trackingRepository;

    /**
     * Initialize a new set of repositories.
     */
    public RepositoryManager (Config config)
        throws PersistenceException
    {
        // create a static connection provider
        StaticConnectionProvider conn = new StaticConnectionProvider(config.getSubProperties("db"));

        /* create the referral repository  */
        referralRepository = new ReferralRepository(conn);

        /* create a repository of users */
        userRepository = new OOOUserManager(config.getSubProperties("oooauth"), conn);

        /* create a repository of sites */
        siteRepository = new JDBCTableSiteIdentifier(conn);

        /* create a repository for tracking */
        trackingRepository = new TrackingRepository(conn);
    }
}
