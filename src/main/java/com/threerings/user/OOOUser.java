//
// $Id$

package com.threerings.user;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.collect.ImmutableList;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.User;
import com.samskivert.util.ArrayIntSet;
import static com.threerings.user.Log.log;

/**
 * Extends the basic samskivert user record with special Three Rings business.
 */
public class OOOUser extends User
{
    /** The flag set when the user's e-mail address has been validated. */
    public static final int VALIDATED_FLAG = (1 << 0);

    /** A flag set when a user has opted-in to receive partner site spam. */
    public static final int AFFILIATE_SPAM_FLAG = (1 << 1);

    /** A flag set when a user pays us money to buy coins. */
    public static final int HAS_BOUGHT_COINS_FLAG = (1 << 2);

    /** A flag set when a user redeems a ubisoft cd key. */
    public static final int UBISOFT_KEY_REDEEMED_FLAG = (1 << 3);

    /** A flag set when a user pays us money for bulk time. */
    public static final int HAS_BOUGHT_TIME_FLAG = (1 << 4);

    /** Indicates that a user is an active player of Y!PP. */
    public static final int IS_ACTIVE_YOHOHO_PLAYER = (1 << 5);

    /** Indicates that a user is an active player of B!H. */
    public static final int IS_ACTIVE_BANG_PLAYER = (1 << 6);

    /** Indicates that a user is an active player of GG. */
    public static final int IS_ACTIVE_GARDENS_PLAYER = (1 << 7);

    /** Indicates that the user has an active subscription on the family friendly servers */
    public static final int FAMILY_SUBSCRIBER = (1 << 8);

    /** Indicates that the user has been involved in a conversion to a Steam account (either as the
     * source or the destination of the conversion). */
    public static final int CONVERTED_TO_STEAM = (1 << 9);

    /** An access token indicating that this user is an admin. */
    public static final byte ADMIN = 1;

    /** An access token indicating that this user is a maintainer. */
    public static final byte MAINTAINER = 2;

    /** An access token indicating that this user is an insider. */
    public static final byte INSIDER = 3;

    /** An access token indicating that this user is a tester. */
    public static final byte TESTER = 4;

    /** An access token indicating that this user is banned from Puzzle Pirates. */
    public static final byte PP_BANNED = 5;

    /** An access token indicating that this user is customer support personnel. */
    public static final byte SUPPORT = 6;

    /** An access token allowing them to spend more than the default max. */
    public static final byte BIG_SPENDER = 7;

    /** An access token indicating that the user has bounced a check or reversed a payment for
     * Puzzle Pirates. */
    public static final byte PP_DEADBEAT = 8;

    /** An access token indicating that this user is banned from Bang! Howdy. */
    public static final byte BANG_BANNED = 9;

    /** An access token indicating that the user has bounced a check or reversed a payment for
     * Bang! Howdy. */
    public static final byte BANG_DEADBEAT = 10;

    /** An access token indicating that this user is banned from MetaSOY. */
    public static final byte MSOY_BANNED = 11;

    /** An access token indicating that the user has bounced a check or reversed a payment for
     * MetaSOY. */
    public static final byte MSOY_DEADBEAT = 12;

    /** An access token indicating that this user is banned from an app. */
    public static final byte APPS_BANNED = 13;

    /** An access token indicating that the user has bounced a check or reversed a payment for an
     * app. */
    public static final byte APPS_DEADBEAT = 14;

    /** An access token indicating that the user is banned from Project X. */
    public static final byte PROJECTX_BANNED = 15;

    /** An access token indicating that the user has bounced a check or reversed a payment for
     * Project X. */
    public static final byte PROJECTX_DEADBEAT = 16;

    /** An access token indicating that the user is banned from Who. */
    public static final byte WHO_BANNED = 17;

    /** An access token indicating that the user has bounced a check or reversed a payment for
     * Who. */
    public static final byte WHO_DEADBEAT = 18;

    /** An access token indicating that this user is junior customer support personnel. */
    public static final byte JR_SUPPORT = 19;

    /** Billing status flags for a particular service. */
    public static final byte TRIAL_STATE = 0;
    public static final byte SUBSCRIBER_STATE = 1;
    public static final byte BILLING_FAILURE_STATE = 2;
    public static final byte EX_SUBSCRIBER_STATE = 3;
    public static final byte BANNED_STATE = 4;

    /** A regular expression that defines the valid characters for affiliate site identifier
     * strings. */
    public static final String SITE_STRING_REGEX = "[-._A-Za-z0-9]+";

    /** The default site id to use in the absence of others; currently puzzlepirates.com.
     * Eventually we'll have to have something on a per-domain basis. */
    public static final int DEFAULT_SITE_ID = 2;

    /** The puzzlepirates.com site identifier. */
    public static final int PUZZLEPIRATES_SITE_ID = 2;

    /** affiliate site id used for "alt" accounts created through pp.com/register/. */
    public static final int PUZZLEPIRATES_ALT_SITE_ID = 164;

    /** The gamegardens.com site identifier. */
    public static final int GAMEGARDENS_SITE_ID = 6;

    /** The site id we group personal referrers under when doing reporting.  This should never be
     * inserted into the database. */
    public static final int REFERRAL_SITE_ID = 7;

    /** The banghowdy.com site identifier. */
    public static final int BANGHOWDY_SITE_ID = 8;

    /** The whirled.com site identifier. */
    public static final int METASOY_SITE_ID = 9;

    /** The puzzlepiratesfamily.com site identifier */
    public static final int YPPFAMILY_SITE_ID = 10;

    /** The apps.threerings.net site identifier. */
    public static final int APPS_SITE_ID = 11;

    /** The Everything app site identifier. */
    public static final int EVERYTHING_SITE_ID = 12;

    /** The BiteMe app site identifier. */
    public static final int BITEME_SITE_ID = 13;

    /** The Down Town app site identifier. */
    public static final int DOWNTOWN_SITE_ID = 14;

    /** The Face Pirate app site identifier. */
    public static final int FACEPIRATE_SITE_ID = 15;

    /** The ProjectX app site identifier - not yet used for billing, only register. */
    public static final int PROJECTX_SITE_ID = 204;

    /** The Who site identifier - jumping to 1000 to avoid collisions with YPP and SK affiliates */
    public static final int WHO_SITE_ID = 1000;

    /** The Ubisoft site id, which we need for various hackery. */
    public static final int UBISOFT_SITE_ID = 40;

    /** A mapping from domain to site id for the OOO sites. */
    public static List<OOOSite> SITES = ImmutableList.of(
        new OOOSite(PUZZLEPIRATES_SITE_ID, "puzzlepirates", "puzzlepirates.com"),
        new OOOSite(GAMEGARDENS_SITE_ID, "gardens", "gamegardens.com"),
        new OOOSite(BANGHOWDY_SITE_ID, "bang", "banghowdy.com"),
        new OOOSite(METASOY_SITE_ID, "metasoy", "whirled.com"),
        new OOOSite(YPPFAMILY_SITE_ID, "family", "puzzlepiratesfamily.com"),
        new OOOSite(APPS_SITE_ID, "apps", "apps.threerings.net"),
        new OOOSite(EVERYTHING_SITE_ID, "everything", "notused"),
        new OOOSite(BITEME_SITE_ID, "biteme", "notused"),
        new OOOSite(DOWNTOWN_SITE_ID, "downtown", "notused"),
        new OOOSite(PROJECTX_SITE_ID, "projectx", "spiralknights.com"),
        new OOOSite(WHO_SITE_ID, "who", "doctorwhowit.com"));

    /** The subscriber column name for Puzzle Pirates subscribers. Used by various repository
     * methods. */
    public static final String PUZZLEPIRATES_COLUMN = "yohoho";

    /** Used to make sure someone doesn't do something stupid. */
    public static final String[] IDENTS_NOT_LOADED = {};

    /** The flags detailing the user's various bits of status. (VALIDATED_FLAG, etc) */
    public int flags;

    /** The tokens detailing the user's site access permissions. (ADMIN, TESTER, etc) */
    public byte[] tokens;

    /** The user's account status for Yohoho! Puzzle Pirates. (TRIAL_STATE, SUBSCRIBER_STATE,
     * etc) */
    public byte yohoho;

    /** The spots that have been given to the user by various crews. */
    public String spots;

    /** The amount of time remaining on the users shun, in minutes. */
    public int shunLeft;

    /** The id of any opaque tag provided by the affiliate to tag this user for their purposes. */
    public int affiliateTagId;

    /** The list of machine identifiers associated with this user. */
    public transient String[] machIdents = IDENTS_NOT_LOADED;

    /**
     * Returns the banned token for the site or 0 if an invalid site.
     */
    public static byte getBannedToken (int site)
    {
        switch (site) {
        case YPPFAMILY_SITE_ID:
        case PUZZLEPIRATES_SITE_ID: return PP_BANNED;
        case BANGHOWDY_SITE_ID: return BANG_BANNED;
        case METASOY_SITE_ID: return MSOY_BANNED;
        case EVERYTHING_SITE_ID: return APPS_BANNED;
        case BITEME_SITE_ID: return APPS_BANNED;
        case DOWNTOWN_SITE_ID: return APPS_BANNED;
        case FACEPIRATE_SITE_ID: return APPS_BANNED;
        case PROJECTX_SITE_ID: return PROJECTX_BANNED;
        case WHO_SITE_ID: return WHO_BANNED;
        default: return (byte)0; // no other sites currently support banning
        }
    }

    /**
     * Returns the deadbeat token for the site or 0 if an unsupported site.
     */
    public static byte getDeadbeatToken (int site)
    {
        switch (site) {
        case YPPFAMILY_SITE_ID:
        case PUZZLEPIRATES_SITE_ID: return PP_DEADBEAT;
        case BANGHOWDY_SITE_ID: return BANG_DEADBEAT;
        case METASOY_SITE_ID: return MSOY_DEADBEAT;
        case EVERYTHING_SITE_ID: return APPS_DEADBEAT;
        case BITEME_SITE_ID: return APPS_DEADBEAT;
        case DOWNTOWN_SITE_ID: return APPS_DEADBEAT;
        case FACEPIRATE_SITE_ID: return APPS_DEADBEAT;
        case PROJECTX_SITE_ID: return PROJECTX_DEADBEAT;
        case WHO_SITE_ID: return WHO_DEADBEAT;
        default:
            log.warning("Requested deadbeat token for unsupported site", "site", site);
            return (byte)0;
        }
    }

    /**
     * Returns whether the user's e-mail address has been validated.
     */
    public boolean isValidated ()
    {
        return isFlagSet(VALIDATED_FLAG);
    }

    /**
     * Returns true if the user has even purchased coins from us.
     */
    public boolean hasBoughtCoins ()
    {
        return isFlagSet(HAS_BOUGHT_COINS_FLAG);
    }

    /**
     * Returns true if the user has even purchased time from us.
     */
    public boolean hasBoughtTime ()
    {
        return isFlagSet(HAS_BOUGHT_TIME_FLAG);
    }

    /**
     * @return true if the specified flag has been set.
     */
    public boolean isFlagSet (int flag)
    {
        return ((flags & flag) != 0);
    }

    /**
     * Updates the user's e-mail validation status.
     */
    public void setValidated (boolean validated)
    {
        setFlag(VALIDATED_FLAG, validated);
    }

    /**
     * Checks whether the user is flagged as a family ocean subscriber.
     */
    public boolean isFamilySubscriber ()
    {
        return isFlagSet(OOOUser.FAMILY_SUBSCRIBER);
    }

    /**
     * Set or clear the specified flag.
     */
    public void setFlag (int flag, boolean set)
    {
        if (set) {
            this.flags |= flag;
        } else {
            this.flags &= ~flag;
        }
        setModified("flags");
    }

    /**
     * Adds the supplied access token to this user's token ring.
     */
    public void addToken (byte token)
    {
        // check to see if they already have it
        if (!holdsToken(token)) {
            if (tokens == null) {
                tokens = new byte[] { token };
            } else {
                int tcount = tokens.length;
                byte[] ntokens = new byte[tcount+1];
                System.arraycopy(tokens, 0, ntokens, 0, tcount);
                ntokens[tcount] = token;
                tokens = ntokens;
            }
            setModified("tokens");
        }
    }

    @Override
    public void setPassword(String password) {
        // we might want to consider disabling this in favor of directly using char[]
        // because char[] can be wiped when it's done being used for security,
        // but for now, we'll keep it as is to not break existing functionality
        setPassword(password.toCharArray());
    }

    /**
     * Sets the user's password and wipes the plaintext password from memory.
     *
     * @param password the password to set
     */
    public void setPassword(char[] password) {
        String encrypted = Crypto.hashPassword(password);
        setPassword(Password.makeFromCrypto(encrypted));
    }

    /**
     * Checks the user's password against the provided plaintext password.
     *
     * @param password the plaintext password to check
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(char[] password) {
        return Crypto.verifyPassword(password, this.password);
    }

    /**
     * Checks whether the user's password is hashed using Argon2.
     */
    public boolean isArgon2Hashed() {
        return Crypto.isArgon2Hashed(password);
    }

    /**
     * Checks if the user's password needs to be rehashed based on the current
     * parameters.
     *
     * @return true if the password needs to be rehashed, false otherwise
     */
    public boolean needsRehash() {
        return !isArgon2Hashed() || Crypto.needsRehash(this.password);
    }

    /**
     * Set all the spots that this user has recieved.
     */
    public void setSpots (ArrayIntSet blackspots)
    {
        if (blackspots == null) {
            throw new IllegalArgumentException("Blackspots parameter can not be null");
        }
        String newspots = "";
        Iterator<Integer> itr = blackspots.iterator();
        for (int ii = 0; itr.hasNext(); ii++) {
            Integer crewid = itr.next();
            if (ii == 0) {
                newspots += crewid;
            } else {
                newspots += ":" + crewid;
            }
        }
        spots = newspots;

        setModified("spots");
    }

    /**
     * Converts the String representation of the users black spots to an ArrayIntSet. Each spot is,
     * in fact, the crewid of the crew that gave it to them.
     */
    public ArrayIntSet getSpots ()
    {
        ArrayIntSet blackSpots = new ArrayIntSet();
        if (spots == null) {
            return blackSpots;
        }

        StringTokenizer tok = new StringTokenizer(spots, ":");

        while (tok.hasMoreTokens()) {
            String crewid = tok.nextToken();
            try {
                int spot = Integer.parseInt(crewid);
                blackSpots.add(spot);
            } catch (NumberFormatException nfe) {
                log.warning("Failed parsing spots.",
                    "user", username, "crewid", crewid, "excpetion", nfe);
            }
        }

        return blackSpots;
    }

    /**
     * Removes the supplied access token from this user's token ring.
     */
    public void removeToken (byte token)
    {
        // make sure they actually have it
        if (holdsToken(token)) {
            // the tokens array is likely to always be very small, so we
            // don't go to the trouble of trying to do this with arraycopy
            int tcount = tokens.length;
            byte[] ntokens = new byte[tcount-1];
            for (int ii = 0, npos = 0; ii < tcount; ii++) {
                if (tokens[ii] == token) {
                    continue;
                }
                ntokens[npos++] = tokens[ii];
            }
            tokens = ntokens;
            setModified("tokens");
        }
    }

    /**
     * Returns true if this user holds the specified token.
     */
    public boolean holdsToken (byte token)
    {
        if (tokens == null) {
            return false;
        }

        int tcount = tokens.length;
        for (int ii = 0; ii < tcount; ii++) {
            if (tokens[ii] == token) {
                return true;
            }
        }

        return false;
    }

    /**
     * Set the billing status of the user for a particular site.
     *
     * @return true if the status changed, false if not.
     */
    public boolean setBillingStatus (int site, byte status)
    {
        if (yohoho != status) {
            yohoho = status;
            setModified("yohoho");
            return true;
        }
        return false;
    }

    /**
     * Return the billing status for the passed in site.
     */
    public byte getBillingStatus (int site)
    {
        switch (site) {
        case YPPFAMILY_SITE_ID:
            return isFamilySubscriber() ? SUBSCRIBER_STATE : yohoho;
        default:
            return yohoho;
        }
    }

    public boolean isMaintainer ()
    {
        return holdsToken(MAINTAINER);
    }

    @Override
    public boolean isAdmin ()
    {
        return holdsToken(ADMIN) || isMaintainer();
    }

    /**
     * Returns true if this user is an "insider" (and as such, should be allowed in for free)
     */
    public boolean isInsider ()
    {
        return (holdsToken(INSIDER) || isAdmin());
    }

    /**
     * Returns true if this user holds the support token
     */
    public boolean isSupport ()
    {
        return holdsToken(SUPPORT);
    }

    /**
     * Returns true if this user holds the support token (or higher)
     */
    public boolean isSupportPlus ()
    {
        return isSupport() || isAdmin();
    }

    /**
     * Returns true if this user is subscriber to Puzzle Pirates.
     */
    public boolean isSubscriber ()
    {
        return isSubscriber(PUZZLEPIRATES_SITE_ID);
    }

    public boolean isSubscriber (int site)
    {
        return ((getBillingStatus(site) == SUBSCRIBER_STATE) || isInsider());
    }

    /**
     * Returns true if we have allowed the user to be a big spender.
     */
    public boolean isBigSpender ()
    {
        return holdsToken(BIG_SPENDER);
    }

    /**
     * Returns true if this user is banned.
     */
    public boolean isBanned (int site)
    {
        byte token = getBannedToken(site);
        return (token == 0 ? false : holdsToken(token));
    }

    /**
     * Configures this user's banned status for the specified site.
     */
    public boolean setBanned (int site, boolean banned)
    {
        byte token = getBannedToken(site);
        if (token == 0) {
            log.warning("Requested to update banned for invalid site", "site", site);
            return false;
        }
        if (banned) {
            addToken(token);
        } else {
            removeToken(token);
        }
        return true;
    }

    /**
     * Returns true if this user has bounced a check or reversed a payment.
     */
    public boolean isDeadbeat (int site)
    {
        byte token = getDeadbeatToken(site);
        return (token == 0 ? false : holdsToken(token));
    }

    /**
     * Configures this user's deadbeat status for the specified site.
     */
    public void setDeadbeat (int site, boolean deadbeat)
    {
        byte token = getDeadbeatToken(site);
        if (token != 0) {
            if (deadbeat) {
                addToken(token);
            } else {
                removeToken(token);
            }
        }
    }

    /**
     * Returns our Yohoho! billing status.
     */
    public byte getYohohoStatus ()
    {
        return getBillingStatus(PUZZLEPIRATES_SITE_ID);
    }

    /**
     * Marks this user as a non paying user.
     */
    public void makeTrialYohoho ()
    {
        setBillingStatus(PUZZLEPIRATES_SITE_ID, TRIAL_STATE);
    }

    /**
     * Returns true if this user holds any of the specified tokens.
     */
    public boolean holdsAnyToken (byte[] tokset)
    {
        if (tokens == null) {
            return false;
        }

        int tcount = tokens.length, scount = tokset.length;
        for (int ii = 0; ii < tcount; ii++) {
            for (int tt = 0; tt < scount; tt++) {
                if (tokens[ii] == tokset[tt]) {
                    return true;
                }
            }
        }

        return false;
    }

    // A protected method can be called by another class in the same package, but if you extend the
    // class containing the protected method and try to call that method from a class in the same
    // package as the derived class, it doesn't work. However, if we simply override the method and
    // do nothing but call <code>super</code>, it's considered "legal." Yay!
    @Override
    protected void setDirtyMask (FieldMask mask)
    {
        super.setDirtyMask(mask);
    }
}
