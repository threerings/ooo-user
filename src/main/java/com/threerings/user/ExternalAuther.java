//
// $Id$

package com.threerings.user;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.StringUtil;

/**
 * Represents external authentication sources supported by OOOuser.
 */
public enum ExternalAuther
    implements ByteEnum
{
    FACEBOOK(1, "facebook.com"),
    OPEN_SOCIAL(2, "opensocial.com"),
    OPEN_ID(3, "openid.net"),
    HEYZAP(4, "heyzap.com") {
        @Override public String makeEmail (String externalId)  {
            return super.makeEmail(StringUtil.encode(externalId));
        }
        @Override public String getExternalId (String email) {
            return StringUtil.decode(super.getExternalId(email));
        }
    },
    STEAM(5, "steampowered.com"),
    SEGA_PASS(6, "sega.com"),
    KONGREGATE(7, "kongregate.com"),
    ARMORGAMES(8, "armorgames.com");

    /**
     * Creates a fake email address given the supplied external user id.
     */
    public String makeEmail (String externalId)
    {
        return externalId + "@" + _domain;
    }

    /**
     * Returns the external id associated with the given email for this auther
     */
    public String getExternalId (String email)
    {
        int idx = email.indexOf("@");
        if (idx <= 0) {
            return email;
        }
        return email.substring(0, idx);
    }

    // from ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    ExternalAuther (int code, String domain) {
        if (code < 1 || code > 31) { // these must fit in an int
            throw new IllegalArgumentException("Code out of byte range: " + code);
        }
        _code = (byte)code;
        _domain = domain;
    }

    protected byte _code;
    protected String _domain;
}
