//
// $Id$

package com.threerings.user.depot;

import static com.threerings.user.Log.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.user.AffiliateUtils;
import com.threerings.user.OOOUser;

public abstract class AffiliateInfo
{
    /** The name of the session attribute used to hold the affiliate suffix */
    public static final String AFFILIATE_SUFFIX_ATTRIBUTE =
        AffiliateInfo.class.getName() + ".AffiliateSuffix";

    /**
     * Class determining the behavior if the affiliate is 'new referral' i.e. the URL
     * has a 'from' and optionally, a 'tag' parameter.
     */
    protected static class NewReferral extends AffiliateInfo
    {
        protected final String fromParameter;

        protected final int siteId;

        protected final int tagId;

        public NewReferral (String fromParameter, int siteId, int tagId)
        {
            this.fromParameter = fromParameter;
            this.siteId = siteId;
            this.tagId = tagId;
        }

        @Override
        public void addToContext (HttpServletRequest req)
        {
            if (isPersonalSuffix(fromParameter)) {
                setAffiliateSuffix(req, fromParameter, tagId);
            } else {
                setAffiliateSuffix(req, "" + siteId, tagId);
            }
        }

        @Override
        public String getAffiliateName ()
        {
            return fromParameter;
        }

        @Override
        public boolean hasName ()
        {
            return !isPersonalSuffix(fromParameter);
        }

        @Override
        public boolean hasSiteId ()
        {
            return true;
        }

        @Override
        public int getSiteId ()
        {
            return siteId;
        }

        @Override
        public void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_TAG_COOKIE, "" + tagId);
        }

        @Override
        public void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            if (isPersonalSuffix(fromParameter)) {
                giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, fromParameter);
            } else {
                giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, "" + siteId);
            }
        }

        @Override
        public void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // TODO probably need nothing
            giveCookie(req, rsp, SITE_REFER_COOKIE, fromParameter);
        }

        @Override
        public int getTagId ()
        {
            return tagId;
        }
    }

    /**
     * Class determining the behavior if the wasn't referred to us in this particular
     * request, but they were referred to us before, and we've recorded that in cookies.
     */
    protected static class CurrentCookies extends AffiliateInfo
    {
        protected final String siteIdCookieValue;

        protected final int tagIdCookieValue;

        public CurrentCookies (String siteIdCookieValue, int tagIdCookieValue)
        {
            this.siteIdCookieValue = siteIdCookieValue;
            this.tagIdCookieValue = tagIdCookieValue;
        }

        @Override
        public void addToContext (HttpServletRequest req)
        {
            setAffiliateSuffix(req, siteIdCookieValue, tagIdCookieValue);
        }

        @Override
        public String getAffiliateName ()
        {
            return null;
        }

        @Override
        public boolean hasName ()
        {
            return false;
        }

        @Override
        public boolean hasSiteId ()
        {
            return true;
        }

        @Override
        public int getSiteId ()
        {
            try {
                return Integer.parseInt(siteIdCookieValue);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }

        @Override
        public void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_TAG_COOKIE, "" + tagIdCookieValue);
        }

        @Override
        public void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, siteIdCookieValue);
        }

        @Override
        public void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, SITE_REFER_COOKIE, "");
        }

        @Override
        public int getTagId ()
        {
            return tagIdCookieValue;
        }
    }

    /**
     * Class determining the behavior if the user has an 'old-style' cookie indicating
     * that they came to us as a referral.
     */
    protected static class LegacyCookies extends AffiliateInfo
    {
        protected final String siteRefer;

        protected final int siteId;

        public LegacyCookies (int siteId, String siteReferCookie)
        {
            this.siteRefer = siteReferCookie;
            this.siteId = siteId;
        }

        @Override
        public void addToContext (HttpServletRequest req)
        {
            if (isPersonalSuffix(siteRefer)) {
                setAffiliateSuffix(req, siteRefer, AffiliateTagRecord.NO_TAG);
            } else {
                setAffiliateSuffix(req, "" + siteId, AffiliateTagRecord.NO_TAG);
            }
        }

        @Override
        public String getAffiliateName ()
        {
            if (!isPersonalSuffix(siteRefer)) {
                return siteRefer;
            } else {
                return "";
            }
        }

        @Override
        public boolean hasName ()
        {
            return !isPersonalSuffix(siteRefer);
        }

        @Override
        public boolean hasSiteId ()
        {
            return true;
        }

        @Override
        public int getSiteId ()
        {
            return siteId;
        }

        @Override
        public void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // TODO: should we actually be removing the cookie here
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_TAG_COOKIE, "");
        }

        @Override
        public void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            if (isPersonalSuffix(siteRefer)) {
                giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, siteRefer);
            } else {
                giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, "" + siteId);
            }
        }

        @Override
        public void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // TODO: should be be removing this cookie?
            giveCookie(req, rsp, SITE_REFER_COOKIE, siteRefer);
        }

        @Override
        public int getTagId ()
        {
            return AffiliateTagRecord.NO_TAG;
        }
    }

    /**
     * Define the behavior if we don't have any affiliate information but we want to track the user
     * in a separate category from the 'default site'.
     */
    protected static class AlternateDefault extends AffiliateInfo
    {
        protected final int site;

        protected AlternateDefault (int site)
        {
            this.site = site;
        }

        @Override
        public boolean isDefaultAffiliate ()
        {
            return true;
        }

        @Override
        public void addToContext (HttpServletRequest req)
        {
            setAffiliateSuffix(req, "" + site, AffiliateTagRecord.NO_TAG);
        }

        @Override
        public String getAffiliateName ()
        {
            return "";
        }

        @Override
        public int getTagId ()
        {
            return AffiliateTagRecord.NO_TAG;
        }

        @Override
        public boolean hasName ()
        {
            return false;
        }

        @Override
        public boolean hasSiteId ()
        {
            return true;
        }

        @Override
        public int getSiteId ()
        {
            return site;
        }

        @Override
        public void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // Don't issue a cookie;
        }

        @Override
        public void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_ID_COOKIE, "" + site);
        }


        @Override
        public void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // Don't issue a cookie;
        }
    }

    /**
     * Determines the behavior if a tag cookie exists, but not an affiliate (site id) cookie.
     * "0" will be supplied as the affiliate to the 'affsuf' context attribute.
     */
    protected static class TagOnly extends AffiliateInfo
    {
        protected final int tagId;

        public TagOnly (int tagId)
        {
            this.tagId = tagId;
        }

        @Override
        public void addToContext (HttpServletRequest req)
        {
            setAffiliateSuffix(req, "0", tagId);
        }

        @Override
        public String getAffiliateName ()
        {
            return "";
        }

        @Override
        public boolean hasName ()
        {
            return false;
        }

        @Override
        public boolean hasSiteId ()
        {
            return false;
        }

        @Override
        public int getSiteId ()
        {
            return 0;
        }

        @Override
        public void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            giveCookie(req, rsp, AffiliateUtils.AFFILIATE_TAG_COOKIE, String.valueOf(tagId));
        }

        @Override
        public void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
            // do not issue a cookie
        }

        @Override
        public void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp)
        {
           // do not issue a cookie
        }

        @Override
        public int getTagId ()
        {
            return tagId;
        }
    }

    /**
     * Find out whether we know the name of the affiliate.
     */
    public abstract boolean hasName ();

    /**
     * Get the name of the affiliate.
     *
     * @return - The name as a string.
     */
    public abstract String getAffiliateName ();

    /**
     * Find out whether we know the site id of the affiliate
     */
    public abstract boolean hasSiteId ();

    /**
     * Get the site id of the affiliate.
     *
     * @return - the site id of the affiliate
     */
    public abstract int getSiteId ();

    /**
     * Get the id for the affiliate tag.
     */
    public abstract int getTagId ();

    /**
     * Compute the appropriate affiliate 'affsuf' and add it to the session along with the
     * 'affid' used by some of the web templates.  Now with less Velocity.
     */
    public abstract void addToContext (HttpServletRequest req);

    /**
     * Issue the site_id cookie to the browser (non-velocity).
     */
    public abstract void issueSiteIdCookie (HttpServletRequest req, HttpServletResponse rsp);

    /**
     * Issue the site_refer cookie to the browser.
     */
    public abstract void issueSiteReferCookie (HttpServletRequest req, HttpServletResponse rsp);

    /**
     * Issue the affiliate_tag cookie to the browser (non-velocity).
     */
    public abstract void issueAffiliateTagCookie (HttpServletRequest req, HttpServletResponse rsp);

    /**
     * Normally returns false, but can be overridden to indicate that we've been given
     * a cookie with a default affiliate passed in to us. "Default" isn't quite the right word
     * since this is really used for letting pages hardwire their default affiliate to be something
     * other than the default, but everything else uses this word, and I don't have a better idea
     * offhand.
     */
    public boolean isDefaultAffiliate ()
    {
        return false;
    }

    /**
     * Make an affiliate info object from the cookies and parameters in the request.
     */
    public static AffiliateInfo getAffiliateInfo (
        DepotUserManager usermgr, ReferralRepository repository, SiteIdentifier identifier,
        HttpServletRequest req, boolean parseFromParameter)
    {
        int siteId = identifier.identifySite(req);
        return getAffiliateInfo(usermgr, repository, identifier, req, siteId, parseFromParameter);
    }

    /**
     * Make an affiliate info object from the cookies and parameters in the request.
     *
     * @param alternateDefault - override default affiliate (hard-coded into affiliate pages)
     * @param parseFromParameter - Should we look at the request parameter "from"?
     * @return - The affiliate info or null if there wasn't enough information in the request to
     *           construct it.
     */
    public static AffiliateInfo getAffiliateInfo (
        DepotUserManager usermgr, ReferralRepository repository, SiteIdentifier identifier,
        HttpServletRequest req, int alternateDefault, boolean parseFromParameter)
    {
        // if they're already a user with us, bail
        if (null != usermgr.loadUser(req)) {
            return null;
        }

        String fromParameter = parseFromParameter ? parseFromParameter(req) : "";

        // is this a new referral - i.e. this request was referred from a referral link?
        if (!StringUtil.isBlank(fromParameter)) {
            String tagParameter = readAffiliateTag(req);

            int siteId = parseReferrer(repository, identifier, fromParameter);
            if (siteId == 0) {
                log.warning("User referred by bogus referrer [referrer=" + fromParameter + "].");
                return null;
            }

            if (!StringUtil.isBlank(tagParameter) && !isPersonalSuffix(fromParameter)) {
                int tagId = usermgr.getAffiliateTagId(tagParameter);

                return new NewReferral(fromParameter, siteId, tagId);
            } else {
                return new NewReferral(fromParameter, siteId, AffiliateTagRecord.NO_TAG);
            }
        }

        // do we have modern cookies for this referral?
        String siteIdCookieValue = readSiteIdCookie(req);
        if (!StringUtil.isBlank(siteIdCookieValue)) {
            int tagIdCookieValue = readTagId(req);

            return new CurrentCookies(siteIdCookieValue, tagIdCookieValue);
        }

        // do we have old fashioned cookies for this referral?
        String siteReferCookie = CookieUtil.getCookieValue(req, SITE_REFER_COOKIE);
        if (!StringUtil.isBlank(siteReferCookie)) {
            int siteId = parseReferrer(repository, identifier, siteReferCookie);
            return new LegacyCookies(siteId, siteReferCookie);
        }

        // we have no useful affiliate information.  If we have been provided with an alternative
        // default site, then use that as the site-id.
        if (alternateDefault != identifier.identifySite(req)) {
            return new AlternateDefault(alternateDefault);
        }

        // we have a tag cookie but no affiliate cookie
        int tagIdCookieValue = readTagId(req);
        if (tagIdCookieValue != AffiliateTagRecord.NO_TAG) {
            return new TagOnly(tagIdCookieValue);
        }

        // we couldn't figure out anything useful to do with whatever affiliate related info
        // we had, so we return null and the caller can do nothing.
        return null;
    }

    /**
     * @param repository For verifying referrer ids against the database
     * @param identifier Velocity site repository
     * @param referrer Referral request string, may be a personal referrer of
     * the format r[0-9]+ or user-[0-9]+, or a site referrer of format [0-9]+
     * @return the referrer siteId, which may be a positive or negative number,
     * or 0 if no match.
     */
    public static int parseReferrer (ReferralRepository repository, SiteIdentifier identifier,
        String referrer)
    {
        // blank referrer string
        if (StringUtil.isBlank(referrer)) {
            return 0;
        }

        // handle new-style personal referrals
        if (referrer.matches("r[0-9]+")) {
            int refId = 0;
            try {
                refId = Integer.parseInt(referrer.substring(1));
            } catch (NumberFormatException nfe) {
                log.warning("Bogus user referral: " + referrer);
                return 0;
            }

            // look up the record in the referrer repository
            ReferralRecord rec = repository.lookupReferral(refId);
            return (rec == null) ? OOOUser.DEFAULT_SITE_ID : (-1 * rec.referrerId);
        }

        // handle old-style personal referrals
        if (referrer.startsWith("user-")) {
            try {
                return -1 * Integer.parseInt(referrer.substring(5));
            } catch (NumberFormatException nfe) {
                log.warning("Bogus user referral: " + referrer);
                return 0;
            }
        }

        // check referrer string against site repository
        int site = identifier.getSiteId(referrer);
        if (site != -1) {
            return site;
        }
        log.warning("Bogus site specified in referrer " + "[value=" + referrer + "].");
        return 0;
    }

    /**
     * Parse the contents of the 'from' parameter.
     */
    public static String parseFromParameter (HttpServletRequest req)
    {
        String value = ParameterUtil.getParameter(req, "from", true);
        if (value != null) {
            // strip the crap some affiliates append to their ID
            Matcher am = _affregex.matcher(value);
            if (am.find()) {
                value = am.group();
            }
        }

        return value;
    }

    /**
     * Find out whether the affiliate name is actually a personal referral as
     * opposed to a site referral.
     *
     * @return - True if the affiliate name is a personal referral. False if
     *         it's a site or unknown.
     */
    protected static boolean isPersonalSuffix (String name)
    {
        return name != null && name.matches("r[0-9]+");
    }

    /**
     * Get the value stored in the site id cookie.
     */
    protected static String readSiteIdCookie (HttpServletRequest req)
    {
        return CookieUtil.getCookieValue(req, AffiliateUtils.AFFILIATE_ID_COOKIE);
    }

    /**
     * Calculate and add the affiliate id and affiliate suffix to the session.  No longer
     * added to the Velocity context; this is velocity-independant.
     * @param req
     * @param referrer
     * @param tagId
     */
    protected void setAffiliateSuffix (HttpServletRequest req, String referrer, int tagId)
    {
        if (tagId != AffiliateTagRecord.NO_TAG) {
            req.getSession().setAttribute(AFFILIATE_SUFFIX_ATTRIBUTE, "-" + referrer + "-" + tagId);
        } else {
            req.getSession().setAttribute(AFFILIATE_SUFFIX_ATTRIBUTE, "-" + referrer);
        }

        req.getSession().setAttribute(AffiliateUtils.AFFILIATE_ID_ATTRIBUTE, referrer);
    }

    /**
     * Add a cookie to the response (velocity independant).
     * @param req servlet request object
     * @param rsp sevlet response object
     * @param name cookie identifier
     * @param value value for the cookie
     */
    protected static void giveCookie (HttpServletRequest req, HttpServletResponse rsp,
                                      String name, String value)
    {
        Cookie cookie = new Cookie(name, value);
        // cookie expires in one month
        cookie.setMaxAge(30 * 24 * 60 * 60);
        // set the path and widened domain (eg www.domain.com -> .domain.com) of the cookie
        cookie.setPath("/");
        CookieUtil.widenDomain(req, cookie);
        rsp.addCookie(cookie);
    }

    /**
     * Get the affiliate tag from the tag parameter in the request.
     *
     * @param req - The request.
     * @return - The tag string.
     */
    protected static String readAffiliateTag (HttpServletRequest req)
    {
        return req.getParameter("tag");
    }

    protected static int readTagId (HttpServletRequest req)
    {
        // if they have an affiliate tag cookie, get it.
        String cookie = CookieUtil.getCookieValue(req, AffiliateUtils.AFFILIATE_TAG_COOKIE);
        if (cookie != null) {
            try {
                return Integer.parseInt(cookie);
            } catch (NumberFormatException e) {
                return AffiliateTagRecord.NO_TAG;
            }
        } else {
            return AffiliateTagRecord.NO_TAG;
        }
    }

    /** A regexp that matches just the proper parts of an affiliate id. */
    protected static Pattern _affregex = Pattern.compile(OOOUser.SITE_STRING_REGEX);

    /**
     * The name of the legacy cookie used to store the affiliate name passed by
     * them to us with their original request.
     */
    protected static final String SITE_REFER_COOKIE = "site_refer";
}
