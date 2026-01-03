//
// $Id$

package com.threerings.user;

import java.util.Map;
import java.util.Properties;
import jakarta.servlet.http.HttpServletRequest;

import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.RunQueue;
import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.servlet.RedirectException;

import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserManager;
import com.samskivert.servlet.user.UserRepository;

import static com.threerings.user.Log.log;

/**
 * Extends the standard user manager with OOO-specific support.
 */
public class OOOUserManager extends UserManager
{
    /**
     * Creates our OOO User manager and prepares it for operation.
     */
    public OOOUserManager (Properties config, ConnectionProvider conprov)
        throws PersistenceException
    {
        this(config, conprov, null);
    }

    /**
     * Creates our OOO user manager and prepares it for operation.
     */
    public OOOUserManager (Properties config, ConnectionProvider conprov, RunQueue pruneQueue)
        throws PersistenceException
    {
        // legacy business
        init(config, conprov, pruneQueue);
    }

    /**
     * Creates an OOO user manager which must subsequently be initialized with
     * a call to {@link #init}.
     */
    public OOOUserManager ()
    {
    }

    @Override
    public OOOUserRepository getRepository ()
    {
        return (OOOUserRepository)_repository;
    }

    @Override
    public void init (Properties config, ConnectionProvider conprov, RunQueue pruneQueue)
        throws PersistenceException
    {
        super.init(config, conprov, pruneQueue);

        // create the blast repository
        _blastRepo = new GameBlastAuxRepository(conprov);

        // look up the access denied URL
        _accessDeniedURL = config.getProperty("access_denied_url");
        if (_accessDeniedURL == null) {
            log.warning("No 'access_denied_url' supplied in user manager config. " +
                "Restricted pages will behave strangely.");
        }

        // load up our affiliate tag mappings
        for (AffiliateTag sub : getRepository().loadAffiliateTags()) {
            _tagMap.put(sub.tag, sub.tagId);
        }
    }

    /**
     * Get the gameblast aux data repository.
     */
    public GameBlastAuxRepository getBlastRepository ()
    {
        return _blastRepo;
    }

    /**
     * Returns the id to which the specified tag has been mapped, assigning a new tag id if
     * necessary.
     *
     * @return -1 if an error occurred assigning a new id.
     */
    public int getAffiliateTagId (String tag)
    {
        // if we've already mapped this value, we're good to go
        Integer tagId = _tagMap.get(tag);
        if (tagId != null) {
            return tagId;
        }

        // register a new affiliate tag and add it to our mappings
        try {
            tagId = getRepository().registerAffiliateTag(tag);
            _tagMap.put(tag, tagId);
            return tagId;

        } catch (PersistenceException pe) {
            log.warning("Failed to register new affiliate tag '" + tag + "'.", pe);
            return -1;
        }
    }

    /**
     * Both tag strings and ids are unique, so grab the string (key) based on the id (value)
     * @param tagId ID to search for (eg 45)
     * @return String value of the tag or tags (eg "nov07GroupA::DF34A9")
     */
    public String getAffiliateTagString (int tagId)
    {
        Integer theTagId = Integer.valueOf(tagId);

        for (Map.Entry<String,Integer> entry : _tagMap.entrySet()) {
            if (theTagId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Extends the standard {@link #requireUser(HttpServletRequest)} with
     * the additional requirement that the user hold the specified token.
     * If they do not, they will be redirected to a page informing them
     * access is denied.
     *
     * @return the user associated with the request.
     */
    public User requireUser (HttpServletRequest req, byte token)
        throws PersistenceException, RedirectException
    {
        OOOUser user = (OOOUser)requireUser(req);
        if (!user.holdsToken(token)) {
            throw new RedirectException(_accessDeniedURL);
        }
        return user;
    }

    /**
     * Extends the standard {@link #requireUser(HttpServletRequest)} with
     * the additional requirement that the user hold one of the specified
     * tokens. If they do not, they will be redirected to a page informing
     * them access is denied.
     *
     * @return the user associated with the request.
     */
    public User requireUser (HttpServletRequest req, byte[] tokens)
        throws PersistenceException, RedirectException
    {
        OOOUser user = (OOOUser)requireUser(req);
        if (!user.holdsAnyToken(tokens)) {
            throw new RedirectException(_accessDeniedURL);
        }
        return user;
    }

    @Override
    protected UserRepository createRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        return new OOOUserRepository(conprov);
    }

    /** The repository for gameblast auxiliary data. */
    protected GameBlastAuxRepository _blastRepo;

    /** The URL to which we redirect users whose access is denied. */
    protected String _accessDeniedURL;

    /** Maintains a mapping of affiliate tags. */
    protected Map<String,Integer> _tagMap = Maps.newHashMap();
}
