//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.depot.IndexDesc;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.user.OOOUser;

/**
 * Emulates {@link OOOUser} for the Depot.
 */
@Entity(name="users",
        indices={ @Index(name="ixLowerUsername", unique=true), @Index(name="ixLowerEmail") })
public class OOOUserRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OOOUserRecord> _R = OOOUserRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> USERNAME = colexp(_R, "username");
    public static final ColumnExp<String> PASSWORD = colexp(_R, "password");
    public static final ColumnExp<String> EMAIL = colexp(_R, "email");
    public static final ColumnExp<String> REALNAME = colexp(_R, "realname");
    public static final ColumnExp<Date> CREATED = colexp(_R, "created");
    public static final ColumnExp<Integer> SITE_ID = colexp(_R, "siteId");
    public static final ColumnExp<Integer> AFFILIATE_TAG_ID = colexp(_R, "affiliateTagId");
    public static final ColumnExp<Integer> FLAGS = colexp(_R, "flags");
    public static final ColumnExp<byte[]> TOKENS = colexp(_R, "tokens");
    public static final ColumnExp<Byte> YOHOHO = colexp(_R, "yohoho");
    public static final ColumnExp<String> SPOTS = colexp(_R, "spots");
    public static final ColumnExp<Integer> SHUN_LEFT = colexp(_R, "shunLeft");
    // AUTO-GENERATED: FIELDS END

    /** Fields that will be checked for modification and updated when calling
     * {@link DepotUserRepository#updateUser}. */
    public static ColumnExp<?>[] UPDATABLE_FIELDS = {
        USERNAME, PASSWORD, EMAIL, REALNAME, AFFILIATE_TAG_ID,
        FLAGS, TOKENS, YOHOHO, SPOTS, SHUN_LEFT
    };

    public static final int SCHEMA_VERSION = 4;

    /** The user's assigned integer userid. */
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    public int userId;

    /** The user's chosen username. */
    @Column(length=128, unique=true)
    public String username;

    /** The user's chosen password (encrypted). */
    @Column(length=128)
    public String password;

    /** The user's email address. */
    @Column(length=128) @Index(name="ixEmail")
    public String email;

    /** The user's real name (first, last and whatever else they opt to provide). */
    @Column(length=128)
    public String realname;

    /** The date this record was created. */
    public Date created;

    /** The site identifier of the site through which the user created
     * their account. (Their affiliation, if you will.) */
    public int siteId;

    /** The id of any opaque tag provided by the affiliate to tag this user for their purposes. */
    public int affiliateTagId;

    /** The flags detailing the user's various bits of status. (VALIDATED_FLAG, etc) */
    public int flags;

    /** The tokens detailing the user's site access permissions. (ADMIN, TESTER, etc) */
    public byte[] tokens;

    /** The user's account status for Puzzle Pirates. (TRIAL_STATE, SUBSCRIBER_STATE, etc) */
    public byte yohoho;

    /** The spots that have been given to the user by various crews. */
    @Column(length=128)
    public String spots;

    /** The amount of time remaining on the users shun, in minutes. */
    public int shunLeft;

    /**
     * Defines the index on {@link #username} converted to lower case.
     */
    public static IndexDesc ixLowerUsername ()
    {
        return new IndexDesc(StringFuncs.lower(OOOUserRecord.USERNAME), Order.ASC);
    }

    /**
     * Defines the index on {@link #email} converted to lower case.
     */
    public static IndexDesc ixLowerEmail ()
    {
        return new IndexDesc(StringFuncs.lower(OOOUserRecord.EMAIL), Order.ASC);
    }

    /**
     * Creates a OOOUserRecord from a OOOUser.
     */
    public static OOOUserRecord fromUser (OOOUser user)
    {
        OOOUserRecord record = new OOOUserRecord();
        record.userId = user.userId;
        record.username = user.username;
        record.created = user.created;
        record.realname = user.realname;
        record.password = user.password;
        record.email = user.email;
        record.siteId = user.siteId;
        record.flags = user.flags;
        record.tokens = user.tokens;
        record.yohoho = user.yohoho;
        record.spots = user.spots;
        record.shunLeft = user.shunLeft;
        record.affiliateTagId = user.affiliateTagId;
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link OOOUserRecord}
     * with the supplied key values.
     */
    public static Key<OOOUserRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END

    /**
     * Returns true if this user holds the specified token.
     */
    public boolean holdsToken (byte token)
    {
        if (tokens == null) {
            return false;
        }
        for (byte heldTok : tokens) {
            if (heldTok == token) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a OOOUser version of this record.
     */
    public OOOUser toUser ()
    {
        OOOUser user = new DepotOOOUser();
        user.userId = userId;
        user.username = username;
        user.created = created;
        user.realname = realname;
        user.password = password;
        user.email = email;
        user.siteId = siteId;
        user.flags = flags;
        user.tokens = tokens;
        user.yohoho = yohoho;
        user.spots = spots;
        user.shunLeft = shunLeft;
        user.affiliateTagId = affiliateTagId;
        return user;
    }

    /**
     * A OOOUser with special dirty handling.
     */
    protected class DepotOOOUser extends OOOUser
    {
        public Set<ColumnExp<?>> mods;

        @Override
        protected void setModified (String field)
        {
            if (mods == null) {
                mods = Sets.newHashSet();
            }
            mods.add(colexp(_R, field));
        }
    }
}
