//
// $Id$

package com.threerings.user.depot;

import java.util.Map;
import java.util.Properties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;

import com.samskivert.depot.ConnectionProvider;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.servlet.RedirectException;
import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.Authenticator;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.User;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.RequestUtils;
import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;

import static com.threerings.user.Log.log;

/**
 * A OOOUserManager using a depot repository.
 */
public class DepotUserManager
{
    /** An instance of the insecure authenticator for general-purpose use. */
    public static final Authenticator AUTH_INSECURE = new InsecureAuthenticator();

    /** An instance of the password authenticator for general-purpose use. */
    public static final Authenticator AUTH_PASSWORD = new PasswordAuthenticator();

    /**
     * A totally insecure authenticator that authenticates any user.  <em>Note:</em> Applications
     * that make use of this authenticator should make sure the user has already been authenticated
     * through some other means.
     */
    public static class InsecureAuthenticator implements Authenticator
    {
        // documentation inherited
        public void authenticateUser (User user, String username, Password password)
            throws InvalidPasswordException
        {
            // don't care
        }
    }

    /**
     * An authenticator that requires that the user-supplied password match the actual user
     * password.
     */
    public static class PasswordAuthenticator implements Authenticator
    {
        // documentation inherited
        public void authenticateUser (User user, String username, Password password)
            throws AuthenticationFailedException
        {
            if (!user.passwordsMatch(password)) {
                throw new InvalidPasswordException("error.invalid_password");
            }
        }
    }

    /**
     * Creates our Depot user manager and prepares it for operation.
     */
    public DepotUserManager (Properties config, ConnectionProvider conprov)
        throws DatabaseException
    {
        this(config, new PersistenceContext("userdb", conprov, null));
    }

    /**
     * Creates our Depot user manager and prepares it for operation.
     */
    public DepotUserManager (Properties config, PersistenceContext pctx)
        throws DatabaseException
    {
        this(config, pctx, null);
    }

    /**
     * Creates our Depot user manager and prepares it for operation.
     */
    public DepotUserManager (Properties config, PersistenceContext pctx, RunQueue pruneQueue)
        throws DatabaseException
    {
        init(config, pctx, pruneQueue);
    }

    /**
     * Creates a depot user manager which must subsequently be initialized with a call to
     * {@link #init}.
     */
    public DepotUserManager ()
    {
    }

    /**
     * Prepares this user manager for operation. Presently the user manager requires the
     * following configuration information:
     *
     * <ul>
     * <li><code>login_url</code>: Should be set to the URL to which to redirect a requester if
     * they are required to login before accessing the requested page. For example:
     *
     * <pre>
     * login_url = /usermgmt/login.ajsp?return=%R
     * </pre>
     *
     * The <code>%R</code> will be replaced with the URL encoded URL the user is currently
     * requesting (complete with query parameters) so that the login code can redirect the user
     * back to this request once they are authenticated.
     * </ul>
     *
     * @param config the user manager configuration properties.
     * @param pctx the persistence context
     */
    public void init (Properties config, PersistenceContext pctx)
        throws DatabaseException
    {
        init(config, pctx, null);
    }

    /**
     * Prepares this user manager for operation. See {@link #init(Properties,PersistenceContext)}.
     *
     * @param pruneQueue an optional run queue on which to run our periodic session pruning task.
     */
    public void init (Properties config, PersistenceContext pctx, RunQueue pruneQueue)
        throws DatabaseException
    {
        // save this for later
        _config = config;

        // create the user repository
        _repository = createRepository(pctx);

        // fetch the login URL from the properties
        _loginURL = config.getProperty("login_url");
        if (_loginURL == null) {
            log.warning("No login_url supplied in user manager config. Authentication won't work.");
            _loginURL = "/missing_login_url";
        }

        // look up any override to our user auth cookie
        String authCook = config.getProperty("auth_cookie.name");
        if (!StringUtil.isBlank(authCook)) {
            _userAuthCookie = authCook;
        }

        if (USERMGR_DEBUG) {
            log.info("UserManager initialized", "acook", _userAuthCookie, "login", _loginURL);
        }

        // register a cron job to prune the session table every hour
        _pruner = new Interval(pruneQueue == null ? Interval.RUN_DIRECT : pruneQueue) {
            @Override public void expired () {
                _repository.pruneSessions();
            }
        };
        _pruner.schedule(SESSION_PRUNE_INTERVAL, true);

        // look up the access denied URL
        _accessDeniedURL = config.getProperty("access_denied_url");
        if (_accessDeniedURL == null) {
            log.warning("No 'access_denied_url' supplied in user manager config. " +
                "Restricted pages will behave strangely.");
        }

        // load up our affiliate tag mappings
        for (AffiliateTagRecord sub : getRepository().loadAffiliateTags()) {
            _tagMap.put(sub.tag, sub.tagId);
        }
    }

    public void shutdown ()
    {
        // cancel our session table pruning thread
        _pruner.cancel();
    }

    /**
     * Returns a reference to the repository in use by this user manager.
     */
    public DepotUserRepository getRepository ()
    {
        return _repository;
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
        tagId = getRepository().registerAffiliateTag(tag);
        _tagMap.put(tag, tagId);
        return tagId;
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
     * Returns the authentication token for the given request
     */
    public String getAuthToken (HttpServletRequest req)
    {
        return CookieUtil.getCookieValue(req, _userAuthCookie);
    }

    /**
     * Fetches the necessary authentication information from the http request and loads the user
     * identified by that information.
     *
     * @return the user associated with the request or null if no user was associated with the
     * request or if the authentication information is bogus.
     */
    public OOOUser loadUser (HttpServletRequest req)
    {
        String authcook = getAuthToken(req);
        if (USERMGR_DEBUG) {
            log.info("Loading user by cookie", _userAuthCookie, authcook);
        }
        return loadUser(authcook);
    }

    /**
     * Loads up a user based on the supplied session authentication token.
     */
    public OOOUser loadUser (String authcode)
    {
        OOOUser user = (authcode == null) ? null : _repository.loadUserBySession(authcode, false);
        if (USERMGR_DEBUG) {
            log.info("Loaded user by authcode", "code", authcode, "user", user);
        }
        return user;
    }

    /**
     * Fetches the necessary authentication information from the http request and loads the user
     * identified by that information. If no user could be loaded (because the requester is not
     * authenticated), a redirect exception will be thrown to redirect the user to the login page
     * specified in the user manager configuration.
     *
     * @return the user associated with the request.
     */
    public OOOUser requireUser (HttpServletRequest req)
        throws RedirectException
    {
        OOOUser user = loadUser(req);
        // if no user was loaded, we need to redirect these fine people to the login page
        if (user == null) {
            String eurl = RequestUtils.getLocationEncoded(req);
            String target = _loginURL.replace("%R", eurl);
            if (USERMGR_DEBUG) {
                log.info("No user found in require, redirecting", "to", target);
            }
            throw new RedirectException(target);
        }
        return user;
    }

    /**
     * Extends the standard {@link #requireUser(HttpServletRequest)} with
     * the additional requirement that the user hold the specified token.
     * If they do not, they will be redirected to a page informing them
     * access is denied.
     *
     * @return the user associated with the request.
     */
    public OOOUser requireUser (HttpServletRequest req, byte token)
        throws RedirectException
    {
        OOOUser user = requireUser(req);
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
    public OOOUser requireUser (HttpServletRequest req, byte[] tokens)
        throws RedirectException
    {
        OOOUser user = requireUser(req);
        if (!user.holdsAnyToken(tokens)) {
            throw new RedirectException(_accessDeniedURL);
        }
        return user;
    }

    /**
     * Attempts to authenticate the requester and initiate an authenticated session for them. An
     * authenticated session involves their receiving a cookie that proves them to be authenticated
     * and an entry in the session database being created that maps their information to their
     * userid. If this call completes, the session was established and the proper cookies were set
     * in the supplied response object. If invalid authentication information is provided or some
     * other error occurs, an exception will be thrown.
     *
     * @param username The username supplied by the user.
     * @param password The password supplied by the user.
     * @param persist If true, the cookie will expire in one month, if false, the cookie will
     * expire at the end of the user's browser session.
     * @param req The request via which the login page was loaded.
     * @param rsp The response in which the cookie is to be set.
     * @param auth The authenticator used to check whether the user should be authenticated.
     *
     * @return the user object of the authenticated user.
     */
    public OOOUser login (String username, Password password, boolean persist,
                       HttpServletRequest req, HttpServletResponse rsp, Authenticator auth)
        throws AuthenticationFailedException
    {
        // load up the requested user
        OOOUser user = _repository.loadUser(username);
        if (user == null) {
            throw new NoSuchUserException("error.no_such_user");
        }

        // run the user through the authentication gamut
        auth.authenticateUser(user, username, password);

        // give them the necessary cookies and business
        effectLogin(user, persist, req, rsp);

        return user;
    }

    /**
     * Attempts to authenticate the requester and initiate an authenticated session for them. A
     * session token will be assigned to the user and returned along with the associated {@link
     * User} record. It is assumed that the client will maintain the session token via its own
     * means.
     *
     * @param username the username supplied by the user.
     * @param password the password supplied by the user.
     * @param expires the number of days in which this session should expire.
     * @param auth the authenticator used to check whether the user should be authenticated.
     *
     * @return the user object of the authenticated user.
     */
    public Tuple<OOOUser,String> login (
        String username, Password password, int expires, Authenticator auth)
        throws AuthenticationFailedException
    {
        // load up the requested user
        OOOUser user = _repository.loadUser(username);
        if (user == null) {
            throw new NoSuchUserException("error.no_such_user");
        }

        // run the user through the authentication gamut
        auth.authenticateUser(user, username, password);

        // register a session for this user
        String authcode = _repository.registerSession(user, expires);
        if (USERMGR_DEBUG) {
            log.info("Session started", "user", username, "code", authcode);
        }
        return new Tuple<OOOUser,String>(user, authcode);
    }

    /**
     * If a user is already known to be authenticated for one reason or other, this method can be
     * used to give them the appropriate authentication cookies to effect their login.
     *
     * @param persist If true, the cookie will expire in one month, if false, the cookie will
     * expire at the end of the user's browser session.
     * @return The registered session authcode
     */
    public String effectLogin (
        OOOUser user, boolean persist, HttpServletRequest req, HttpServletResponse rsp)
    {
        return effectLogin(user, persist ? PERSIST_EXPIRE_DAYS : NON_PERSIST_EXPIRE_DAYS, req, rsp);
    }

    /**
     * If a user is already known to be authenticated for one reason or other, this method can be
     * used to give them the appropriate authentication cookies to effect their login.
     *
     * @param expires the number of days in which to expire the session cookie, 0 means expire at
     * the end of the browser session.
     * @return The registered session authcode
     */
    public String effectLogin (
        OOOUser user, int expires, HttpServletRequest req, HttpServletResponse rsp)
    {
        String authcode = _repository.registerSession(user, Math.max(expires, 1));
        Cookie acookie = new Cookie(_userAuthCookie, authcode);
        // strip the hostname from the server and use that as the domain unless configured not to
        if (!"false".equalsIgnoreCase(_config.getProperty("auth_cookie.strip_hostname"))) {
            CookieUtil.widenDomain(req, acookie);
        }
        acookie.setPath("/");
        acookie.setMaxAge((expires > 0) ? (expires*24*60*60) : -1);
        if (USERMGR_DEBUG) {
            log.info("Setting cookie " + acookie + ".");
        }
        rsp.addCookie(acookie);
        return authcode;
    }

    /**
     * Logs the user out.
     */
    public void logout (HttpServletRequest req, HttpServletResponse rsp)
    {
        // nothing to do if they don't already have an auth cookie
        String authcode = CookieUtil.getCookieValue(req, _userAuthCookie);
        if (authcode == null) {
            return;
        }

        // set them up the bomb
        Cookie c = new Cookie(_userAuthCookie, "x");
        c.setPath("/");
        c.setMaxAge(0);
        CookieUtil.widenDomain(req, c);
        if (USERMGR_DEBUG) {
            log.info("Clearing cookie " + c + ".");
        }
        rsp.addCookie(c);

        // we need an unwidened one to ensure that old-style cookies are wiped as well
        c = new Cookie(_userAuthCookie, "x");
        c.setPath("/");
        c.setMaxAge(0);
        rsp.addCookie(c);
    }

    /**
     * Validates that the supplied session key is still valid and if so, refreshes it for the
     * specified number of days.
     *
     * @return true if the session was located and refreshed, false otherwise.
     */
    public boolean refreshSession (String sessionKey, int expireDays)
    {
        return _repository.refreshSession(sessionKey, expireDays);
    }

    /**
     * Called by the user manager to create the user repository. Derived classes can override this
     * and create a specialized repository if they so desire.
     */
    protected DepotUserRepository createRepository (PersistenceContext pctx)
        throws DatabaseException
    {
        return new DepotUserRepository(pctx);
    }

    /** Our user manager configuration. */
    protected Properties _config;

    /** The user repository. */
    protected DepotUserRepository _repository;

    /** The interval for user session pruning. */
    protected Interval _pruner;

    /** The URL for the user login page. */
    protected String _loginURL;

    /** The name of our user authentication cookie. */
    protected String _userAuthCookie = USERAUTH_COOKIE;

    /** Maintains a mapping of affiliate tags. */
    protected Map<String,Integer> _tagMap = Maps.newHashMap();

    /** The URL to which we redirect users whose access is denied. */
    protected String _accessDeniedURL;

    /** The user authentication cookie name. */
    protected static final String USERAUTH_COOKIE = "id_";

    /** Prune the session table every hour. */
    protected static final long SESSION_PRUNE_INTERVAL = 60L * 60L * 1000L;

    /** Indicates how long (in days) that a "persisting" session token should last. */
    protected static final int PERSIST_EXPIRE_DAYS = 30;

    /** Indicates how long (in days) that a "non-persisting" session token should last. */
    protected static final int NON_PERSIST_EXPIRE_DAYS = 1;

    /** Change this to true and recompile to debug cookie handling. */
    protected static final boolean USERMGR_DEBUG = false;
}
