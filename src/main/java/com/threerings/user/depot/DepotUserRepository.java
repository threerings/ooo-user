//
// $Id$

package com.threerings.user.depot;

import static com.threerings.user.Log.log;

import java.sql.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.Builder3;
import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.UserUtil;
import com.samskivert.servlet.user.Username;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;
import com.threerings.user.DetailedUser;
import com.threerings.user.OOOAuxData;
import com.threerings.user.OOOBillAuxData;
import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;
import com.threerings.user.ValidateRecord;

/**
 * Depot implementation of the OOO user repository.
 */
@Singleton
public class DepotUserRepository extends DepotRepository
{
    /** A user's access level. */
    public static enum Access
    {
        /** Access granted, user is not banned nor coming from a tainted machine. */
        ACCESS_GRANTED,

        /** User is trying to create a new account from a tainted machine. */
        NEW_ACCOUNT_TAINTED,

        /** The user is banned from playing. */
        ACCOUNT_BANNED,

        /** The user can not create another free account on this machine. */
        NO_NEW_FREE_ACCOUNT,

        /** The user has bounced a check or reversed a payment. */
        DEADBEAT;
    }

    @Computed @Entity
    public static class CountRecord extends PersistentRecord
    {
        /** The computed count. */
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public DepotUserRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Looks up a user by userid.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUser loadUser (int userId)
    {
        return toUser(load(OOOUserRecord.getKey(userId)));
    }

    /**
     * Looks up a user by username.
     *
     * @return the user with the specified username or null if no user with that username exists.
     */
    public OOOUser loadUser (String username)
    {
        return loadUser(username, false);
    }

    /**
     * Loads up all users in the supplied set of user ids.
     *
     * @deprecated use #loadUsers.
     */
    @Deprecated
    public HashIntMap<OOOUser> loadUsersFromId (Set<Integer> userIds)
    {
        HashIntMap<OOOUser> userMap = new HashIntMap<OOOUser>();
        for (OOOUserRecord userRec : loadAll(OOOUserRecord.class, userIds)) {
            userMap.put(userRec.userId, toUser(userRec));
        }
        return userMap;
    }

    /**
     * Loads up all users in the supplied set of user ids.
     */
    public Map<Integer, OOOUser> loadUsers (Set<Integer> userIds)
    {
        // TODO: remove loadUsersFromId, switch to just a HashMap<Integer, OOOUser>
        return loadUsersFromId(userIds);
    }

    /**
     * Loads up all users with the specified email.
     */
    public Iterable<OOOUser> lookupUsersByEmail (String email)
    {
        List<OOOUser> users = Lists.newArrayList();
        for (OOOUserRecord record : findAll(OOOUserRecord.class,
                    new Where(OOOUserRecord.EMAIL, email))) {
            users.add(toUser(record));
        }
        return users;
    }

    /**
     * Looks up a user by username and optionally loads their machine identifier information.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUser loadUser (String username, boolean loadIdents)
    {
        return resolveIdents(toUser(load(OOOUserRecord.class,
                new Where(StringFuncs.lower(OOOUserRecord.USERNAME).eq(username.toLowerCase())))),
                loadIdents);
    }

    /**
     * Looks up a user by email address.
     */
    public OOOUser loadUserByEmail (String email, boolean loadIdents)
    {
        return resolveIdents(toUser(load(OOOUserRecord.class,
                new Where(StringFuncs.lower(OOOUserRecord.EMAIL).eq(email.toLowerCase())))),
                loadIdents);
    }

    /**
     * Looks up a user by their session identifier.
     *
     * @return the user associated with the specified session or null of no session exists with the
     * supplied identifier.
     */
    public OOOUser loadUserBySession (String authcode)
    {
        return loadUserBySession(authcode, false);
    }

    /**
     * Looks up a user by their session identifier.
     *
     * @return the user associated with the specified session or null of no session exists with the
     * supplied identifier.
     */
    public OOOUser loadUserBySession (String authcode, boolean loadIdents)
    {
        SessionRecord sess = load(SessionRecord.getKey(authcode));
        // Check against the beginning of the day rather than right now because our database stores
        // only the expire date, but the user may have a valid cookie that doesn't expire until
        // later today.
        long expireTime = Calendars.now().zeroTime().toTime();
        return (sess == null || sess.expires.getTime() < expireTime) ? null :
            resolveIdents(toUser(load(OOOUserRecord.class,
                                      OOOUserRecord.getKey(sess.userId))), loadIdents);
    }

    /**
     * Looks up the session identifer for the given user.
     */
    public String loadSessionAuthcode (int userId)
    {
        SessionRecord sess = from(SessionRecord.class).where(SessionRecord.USER_ID, userId).load();
        return sess == null ? null : sess.authcode;
    }

    /**
     * Creates a new session for the specified user and returns the randomly generated session
     * identifier for that session. If a session entry already exists for the specified user it
     * will be reused.
     *
     * @param expireDays the number of days in which the session token should expire.
     */
    public String registerSession (OOOUser user, int expireDays)
    {
        String authcode = loadSessionAuthcode(user.userId);

        // if we found one, update its expires time and reuse it
        if (authcode != null) {
            // figure out when to expire the session
            Date expires = Calendars.now().addDays(expireDays).toSQLDate();
            updatePartial(SessionRecord.getKey(authcode), SessionRecord.EXPIRES, expires);
            return authcode;

        } else {
            // otherwise create a new one and insert it into the table
            authcode = UserUtil.genAuthCode(user);
            setSession(user.userId, authcode, expireDays);
            return authcode;
        }
    }

    /**
     * Creates a new session record for the specified user with the specified session identifier.
     *
     * @param expireDays the number of days in which the session token should expire
     */
    public void setSession (int userId, String authcode, int expireDays)
    {
        SessionRecord sess = new SessionRecord();
        sess.authcode = authcode;
        sess.userId = userId;
        sess.expires = Calendars.now().addDays(expireDays).toSQLDate();
        insert(sess);
    }

    /**
     * Clears the given user's existing session, if found.
     */
    public void clearSession (int userId)
    {
        from(SessionRecord._R).where(SessionRecord.USER_ID, userId).delete();
    }

    /**
     * Validates that the supplied session key is still valid and if so, refreshes it for the
     * specified number of days.
     *
     * @return true if the session was located and refreshed, false if it no longer exists.
     */
    public boolean refreshSession (String authcode, int expireDays)
    {
        Date expires = Calendars.now().addDays(expireDays).toSQLDate();
        // attempt to update an existing session record, returning true if we found and updated it
        return updatePartial(SessionRecord.getKey(authcode), SessionRecord.EXPIRES, expires) == 1;
    }

    /**
     * Prunes any expired sessions from the sessions table.
     */
    public void pruneSessions ()
    {
        Date now = new Date(System.currentTimeMillis());
        deleteAll(SessionRecord.class, new Where(SessionRecord.EXPIRES.lessEq(now)));
    }

    /**
     * Returns an array of usernames registered with the specified email address or the empty array
     * if none are registered with said address.
     */
    public String[] getUsernames (String email)
    {
        List<String> usernames = Lists.newArrayList();
        Where where = new Where(OOOUserRecord.EMAIL, email);
        for (OOOUserRecord record : findAll(OOOUserRecord.class, where)) {
            usernames.add(record.username);
        }
        return usernames.toArray(new String[usernames.size()]);
    }

    /**
     * Returns an array of usernames from the supplied collection that have the specified token set.
     */
    public List<String> getTokenUsernames (Collection<String> usernames, byte token)
    {
        // We're doing a manual token check after loading the users, however ideally having the
        // depot support for a regexp comparison on a hex converted tokens field would be faster
        List<String> retnames = Lists.newArrayList();
        if (usernames.size() > 0) {
            Where where = new Where(OOOUserRecord.USERNAME.in(usernames));
            for (OOOUserRecord record : findAll(OOOUserRecord.class, where)) {
                if (record.holdsToken(token)) {
                    retnames.add(record.username);
                }
            }
        }
        return retnames;
    }

    /**
     * Loads up the machine ident information for the supplied user.
     */
    public String[] loadMachineIdents (int userId)
    {
        List<String> idents = Lists.newArrayList();
        Where where = new Where(UserIdentRecord.USER_ID, userId);
        for (UserIdentRecord record : findAll(UserIdentRecord.class, where)) {
            if (!StringUtil.isBlank(record.machIdent)) {
//                log.info("Adding machine ident", "userId", userId, "machIdent", record.machIdent);
                idents.add(record.machIdent);
            }
        }
        String[] machIdents = idents.toArray(new String[idents.size()]);
        Arrays.sort(machIdents); // sort the idents in java to ensure correct collation
        return machIdents;
    }

    /**
     * Loads up the machine ident information for the supplied user.
     */
    public void loadMachineIdents (OOOUser user)
    {
        user.machIdents = loadMachineIdents(user.userId);
    }

    /**
     * Returns a list of all users that have ever reported the specified machine identifier.
     * TODO: is this used anywhere outside of underwire? If not, it can be removed.
     */
    public List<Tuple<Integer, String>> getUsersOfMachIdent (String machIdent)
    {
        List<Tuple<Integer,String>> users = Lists.newArrayList();
        Join join = new Join(UserIdentRecord.class, Ops.and(
                                 OOOUserRecord.USER_ID.eq(UserIdentRecord.USER_ID),
                                 UserIdentRecord.MACH_IDENT.eq(machIdent)));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, join)) {
            users.add(new Tuple<Integer,String>(record.userId, record.username));
        }
        return users;
    }

    /**
     * Returns a list of all usernames and their flags that have ever reported the specified
     * machine identifier.
     */
    public List<OOOUserCard> getUsersOfMachIdentCards (String machIdent)
    {
        return from(OOOUserRecord.class)
            .where(UserIdentRecord.MACH_IDENT, machIdent)
            .join(OOOUserRecord.USER_ID, UserIdentRecord.USER_ID)
            .select(BUILD_OOO_USER_CARD,
                OOOUserRecord.USER_ID, OOOUserRecord.USERNAME, OOOUserRecord.FLAGS);
    }

    /**
     * Returns a list of all users that have ever reported the specified machine identifier.
     */
    public List<Tuple<Integer, String>> getUsersOfMachIdents (String[] idents)
    {
        List<Tuple<Integer,String>> users = Lists.newArrayList();
        Join join = new Join(UserIdentRecord.class, Ops.and(
                                 OOOUserRecord.USER_ID.eq(UserIdentRecord.USER_ID),
                                 UserIdentRecord.MACH_IDENT.in(Arrays.asList(idents))));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, join)) {
            users.add(new Tuple<Integer,String>(record.userId, record.username));
        }
        return users;
    }

    /**
     * Add the userId -> machIdent mapping to the database.
     */
    public void addUserIdent (int userId, String machIdent)
    {
        // don't add blank or null idents
        if (!StringUtil.isBlank(machIdent)) {
            try {
                insert(new UserIdentRecord(userId, machIdent));
            } catch (DuplicateKeyException dke) {
                // ignore, since the cache may have lied about this record not being present
            }
        }
    }

    /**
     * Returns the number of times this machIdent appears.
     */
    public int getMachineIdentCount (String machIdent)
    {
        return load(CountRecord.class, new FromOverride(UserIdentRecord.class),
                new Where(UserIdentRecord.MACH_IDENT, machIdent)).count;
    }

    /**
     * Checks to see if the specified machine identifier is tainted.
     */
    public boolean isTaintedIdent (String machIdent)
    {
        // blank or null idents can't be tainted
        if (StringUtil.isBlank(machIdent)) {
            return false;
        }
        return load(TaintedIdentRecord.getKey(machIdent)) != null;
    }

    /**
     * Returns the subset of the supplied machine idents that are tainted.
     */
    public Collection<String> filterTaintedIdents (String[] idents)
    {
        if (idents == null || idents.length == 0) {
            return Collections.emptyList();
        }

        return from(TaintedIdentRecord.class)
            .where(TaintedIdentRecord.MACH_IDENT.in(idents))
            .select(TaintedIdentRecord.MACH_IDENT);
    }

    /**
     * Store to the database that the passed in machIdent has been tainted by a banned player.
     */
    public void addTaintedIdent (String machIdent)
    {
        // don't taint blank or null idents
        if (!StringUtil.isBlank(machIdent)) {
            try {
                insert(new TaintedIdentRecord(machIdent));
            } catch (DuplicateKeyException dke) {
                // that's fine
            }
        }
    }

    /**
     * Remove a machine ident from the tainted table.
     */
    public void removeTaintedIdent (String machIdent)
    {
        delete(TaintedIdentRecord.getKey(machIdent));
    }

    /**
     * Checks to see if the specified machine identifier is banned.
     */
    public boolean isBannedIdent (String machIdent, int siteId)
    {
        // blank or null idents can't be tainted
        if (StringUtil.isBlank(machIdent)) {
            return false;
        }
        return load(BannedIdentRecord.getKey(machIdent, siteId)) != null;
    }

    /**
     * Returns the subset of the supplied machine idents that are banned.
     */
    public Collection<String> filterBannedIdents (String[] idents, int siteId)
    {
        if (idents == null || idents.length == 0) {
            return Collections.emptyList();
        }
        return from(BannedIdentRecord.class)
            .where(BannedIdentRecord.SITE_ID.eq(siteId), BannedIdentRecord.MACH_IDENT.in(idents))
            .select(BannedIdentRecord.MACH_IDENT);
    }

    /**
     * Store to the database that the passed in machIdent has been banned on the site.
     */
    public void addBannedIdent (String machIdent, int siteId)
    {
        insert(new BannedIdentRecord(machIdent, siteId));
    }

    /**
     * Remove a machine ident from the banned table.
     */
    public void removeBannedIdent (String machIdent, int siteId)
    {
        delete(BannedIdentRecord.getKey(machIdent, siteId));
    }

    /**
     * Creates a new user record in the repository with no auxiliary data.
     */
    public int createUser (Username username, String password, String email, int siteId)
        throws UserExistsException
    {
        return createUser(username, Password.makeFromCrypto(password), email, siteId, 0);
    }

    /**
     * Creates a new user record in the repository with no auxiliary data.
     */
    public int createUser (
            Username username, Password password, String email, int siteId, int tagId)
        throws UserExistsException
    {
        return createUser(username, password, email, siteId, tagId, null, (byte)-1, null);
    }

    /**
     * Creates a new user record in the repository.
     */
    public int createUser (Username username, Password password, String email, int siteId,
                           int tagId, int birthyear, byte gender, String missive)
        throws UserExistsException
    {
        // convert birth year to a fake birthday (Jan 1 of that year)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, birthyear);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, 0);
        return createUser(username, password, email, siteId, tagId,
                          new Date(cal.getTimeInMillis()), gender, missive);
    }

    /**
     * Creates a new user record in the repository.
     */
    public int createUser (Username username, Password password, String email, int siteId,
                           int tagId, Date birthday, byte gender, String missive)
        throws UserExistsException
    {
        OOOUserRecord user = new OOOUserRecord();

        // fill in the base user information
        user.username = username.getUsername();
        user.password = password.getEncrypted();
        user.realname = "";
        user.email = email;
        user.created = new Date(System.currentTimeMillis());
        user.siteId = siteId;

        // fill in the ooo-specific user information
        user.tokens = new byte[0];
        user.spots = "";
        user.affiliateTagId = tagId;

        try {
            insert(user);
        } catch (DuplicateKeyException e) {
            throw new UserExistsException("error.user_exists");
        }

        // store the auxiliary data (if provided)
        if (birthday != null || gender >= 0 || missive != null) {
            OOOAuxDataRecord record = new OOOAuxDataRecord();
            record.userId = user.userId;
            record.birthday = birthday;
            record.gender = gender;
            record.missive = StringUtil.truncate((missive == null ? "" : missive), 255);
            insert(record);
        }

        HistoricalUserRecord hrec = new HistoricalUserRecord();
        hrec.userId = user.userId;
        hrec.username = user.username;
        hrec.created = user.created;
        hrec.siteId = siteId;
        insert(hrec);

        return user.userId;
    }

    /**
     * Changes a user's username.
     *
     * @return true if the old username existed and was changed to the new name, false if the old
     * username did not exist.
     *
     * @exception UserExistsException thrown if the new name is already in use.
     */
    public boolean changeUsername (int userId, String username)
        throws UserExistsException
    {
        try {
            return 0 != updatePartial(OOOUserRecord.getKey(userId),
                                      OOOUserRecord.USERNAME, username);
        } catch (DuplicateKeyException pe) {
            throw new UserExistsException("error.user_exists");
        }
    }

    /**
     * Updates the specified user's email address.
     */
    public void changeEmail (int userId, String email)
    {
        updatePartial(OOOUserRecord.getKey(userId), OOOUserRecord.EMAIL, email);
    }

    /**
     * Updates the specified user's email address and marks their account as unvalidated.
     */
    public void changeEmailAndInvalidate (int userId, String email)
    {
        updatePartial(OOOUserRecord.getKey(userId), OOOUserRecord.EMAIL, email,
                      OOOUserRecord.FLAGS, OOOUserRecord.FLAGS.bitAnd(~OOOUser.VALIDATED_FLAG));
    }

    /**
     * Updates the specified user's password (should already be encrypted).
     */
    public void changePassword (int userId, String password)
    {
        updatePartial(OOOUserRecord.getKey(userId), OOOUserRecord.PASSWORD, password);
    }

    /**
     * Updates a user that was previously fetched from the repository.  Only fields that have been
     * modified since it was loaded will be written to the database and those fields will
     * subsequently be marked clean once again.
     *
     * @return true if the record was updated, false if the update was skipped because no fields in
     * the user record were modified.
     */
    public boolean updateUser (OOOUser user)
    {
        OOOUserRecord.DepotOOOUser duser = (OOOUserRecord.DepotOOOUser)user;
        if (duser.mods == null) {
            return false;
        }
        update(OOOUserRecord.fromUser(user), duser.mods.toArray(new ColumnExp[duser.mods.size()]));
        duser.mods = null;
        return true;
    }

    /**
     * Adds the supplied flags to the specified user's flags.
     */
    public void addFlags (int userId, int addMask)
    {
        updatePartial(OOOUserRecord.getKey(userId),
                      OOOUserRecord.FLAGS, OOOUserRecord.FLAGS.bitOr(addMask));
    }

    /**
     * Clears the supplied flags from the specified user's flags.
     */
    public void clearFlags (int userId, int clearMask)
    {
        updatePartial(OOOUserRecord.getKey(userId),
                      OOOUserRecord.FLAGS, OOOUserRecord.FLAGS.bitAnd(~clearMask));
    }

    /**
     * 'Delete' the users account such that they can no longer access it, however we do not delete
     * the record from the db.  The name is changed such that the original name has XX=FOO if the
     * name were FOO originally.  If we have to lop off any of the name to get our prefix to fit we
     * use a minus sign instead of a equals side.  The password field is set to be the empty string
     * so that no one can log in (since nothing hashes to the empty string.  We also make sure
     * their email address no longer works, so in case we don't ignore 'deleted' users when we do
     * the sql to get emailaddresses for the mass mailings we still won't spam delete folk.  We
     * leave the emailaddress intact exect for the @ sign which gets turned to a #, so that we can
     * see what their email was incase it was an accidently deletion and we have to verify through
     * email.
     */
    public void deleteUser (OOOUser user)
    {
        if (user.isDeleted()) {
            return;
        }

        OOOUserRecord.DepotOOOUser duser = (OOOUserRecord.DepotOOOUser)user;
        duser.setModified("username");
        duser.setModified("password");
        duser.setModified("email");

        OOOUserRecord record = OOOUserRecord.fromUser(duser);

        // 'disable' their email address
        String newEmail = duser.email.replace('@', '#');

        String oldName = user.username;
        for (int ii = 0; ii < 100; ii++) {
            try {
                String newUsername = StringUtil.truncate(ii + "=" + oldName, 24);
                updatePartial(OOOUserRecord.getKey(record.userId),
                                      OOOUserRecord.USERNAME, newUsername,
                                      OOOUserRecord.EMAIL, newEmail,
                                      OOOUserRecord.PASSWORD, "");
            } catch (DuplicateKeyException e) {
                // try again
                continue;
            }
            return;
        }
    }

    /**
     * Mark this user's account as banned, update the tainted machine idents table as needed.
     *
     * @return true if the user exists and was banned, false if not.
     */
    public boolean ban (int site, String username)
    {
        OOOUser user = loadUser(username, false);
        if (user == null) {
            return false;
        }

        if (!user.setBanned(site, true)) {
            return false;
        }
        updateUser(user);
        String[] idents = loadMachineIdents(user.userId);
        Collection<String> tainted = filterTaintedIdents(idents);
        for (String id : idents) {
            if (!tainted.contains(id)) {
                addTaintedIdent(id);
            }
        }
        return true;
    }

    /**
     * Remove the ban from the users account, optionally untainting his machine idents.
     *
     * @return true if the user exists and was unbanned, false if not.
     */
    public boolean unban (int site, String username, boolean untaint)
    {
        // Not currently tainting every system this user has ever touched or will touch in the
        // future
        OOOUser user = loadUser(username, untaint);
        if (user == null) {
            return false;
        }

        if (!user.setBanned(site, false)) {
            return false;
        }
        updateUser(user);

        if (untaint) {
            for (String machIdent : user.machIdents) {
                removeTaintedIdent(machIdent);
            }
        }
        return true;
    }

    /**
     * Checks whether or not the user in question should be allowed access.
     *
     * @param site the site for which we are validating the user.
     * @param newPlayer true if the user is attempting to create a new game account.
     */
    public Access validateUser (int site, OOOUser user, String machIdent, boolean newPlayer)
    {
        // if this user's idents were not loaded, complain
        if (user.machIdents == OOOUser.IDENTS_NOT_LOADED) {
            log.warning("Requested to validate user with unloaded idents",
                "who", user.username, new Exception());
            // err on the side of not screwing our customers
            return Access.ACCESS_GRANTED;
        }

        // if we have never seen them before...
        if (user.machIdents == null) {
            // add their ident to the userobject and db
            user.machIdents = new String[] { machIdent };
            addUserIdent(user.userId, machIdent);

        } else if (Arrays.binarySearch(user.machIdents, machIdent) < 0) {
            // add the machIdent to the users list of associated idents
            user.machIdents = ArrayUtil.append(user.machIdents, machIdent);
            // and slap it in the db
            addUserIdent(user.userId, machIdent);
        }

        // if this is a banned user, mark that ident
        if (user.isBanned(site)) {
            addTaintedIdent(machIdent);
            return Access.ACCOUNT_BANNED;
        }

        // if this is a banned machIdent just return banned status
        if (isBannedIdent(machIdent, site)) {
            return Access.ACCOUNT_BANNED;
        }

        // don't let those bastards grief us.
        if (newPlayer && (isTaintedIdent(machIdent)) ) {
            return Access.NEW_ACCOUNT_TAINTED;
        }

        // if the user has bounced a check or reversed payment, let them know
        if (user.isDeadbeat(site)) {
            return Access.DEADBEAT;
        }

        // if they have 0 sessions and they're not a subscriber, then make sure there aren't too
        // many other free accounts already created with this machine ident
        if (!user.isSubscriber() && !user.hasBoughtCoins() && newPlayer &&
                (playedRecentFreeAccounts(machIdent, RECENT_ACCOUNT_CUTOFF) >
                 MAX_FREE_ACCOUNTS_PER_MACHINE)) {
            return Access.NO_NEW_FREE_ACCOUNT;
        }

        // you're all clear kid...
        return Access.ACCESS_GRANTED;
    }

    /**
     * Checks whether or not the machine in question should be allowed access.
     *
     * @param newPlayer true if the user is attempting to create a new game
     * account.
     * @param site the site we're trying to validate this machine on.
     */
    public Access validateIdent (int site, String machIdent, boolean newPlayer)
    {
        // if this is a banned machIdent just return banned status
        if (isBannedIdent(machIdent, site)) {
            return Access.ACCOUNT_BANNED;
        }

        // don't let those bastards grief us.
        if (newPlayer && isTaintedIdent(machIdent)) {
            return Access.NEW_ACCOUNT_TAINTED;
        }

        // make sure there aren't too many other free accounts already created with this ident
        if (newPlayer && (playedRecentFreeAccounts(machIdent, RECENT_ACCOUNT_CUTOFF) >
                MAX_FREE_ACCOUNTS_PER_MACHINE)) {
            return Access.NO_NEW_FREE_ACCOUNT;
        }

        // you're all clear kid...
        return Access.ACCESS_GRANTED;
    }

    /**
     * Loads all subaffiliate mappings.
     */
    public List<AffiliateTagRecord> loadAffiliateTags ()
    {
        return findAll(AffiliateTagRecord.class);
    }

    /**
     * Adds a new affiliate tag mapping and returns the assigned identifier.
     */
    public int registerAffiliateTag (String tag)
    {
        AffiliateTagRecord record =
            load(AffiliateTagRecord.class, new Where(AffiliateTagRecord.TAG, tag));
        if (record == null) {
            record = new AffiliateTagRecord();
            record.tag = tag;
            insert(record);
        }
        return record.tagId;
    }

    /**
     * Returns the afilliate tag for the id, or null if it doesn't exist.
     */
    public String loadAffiliateTag (int tagId)
    {
        AffiliateTagRecord record = load(AffiliateTagRecord.getKey(tagId));
        return record == null ? null : record.tag;
    }

    /**
     * Returns a list of detail records for the users registered in our database, starting with the
     * specified record number and containing at most <code>count</code> elements.
     *
     * @param filter if true, unvalidated users and users that are already testers or the like are
     * filtered out.
     */
    public List<DetailedUser> getDetailRecords (int start, int count, boolean filter)
    {
        List<DetailedUser> users = Lists.newArrayList();
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(OOOUserRecord.class));
        clauses.add(new Join(
                    OOOUserRecord.USER_ID, OOOAuxDataRecord.USER_ID).setType(Join.Type.LEFT_OUTER));
        if (filter) {
            clauses.add(new Where(Ops.and(OOOUserRecord.FLAGS.notEq(0),
                            Funcs.arrayLength(OOOUserRecord.TOKENS).eq(0))));
        }
        clauses.add(OrderBy.descending(OOOUserRecord.USER_ID));
        clauses.add(new Limit(start, count));
        for (DetailedUserRecord record : findAll(DetailedUserRecord.class, clauses)) {
            users.add(record.toDetailedUser());
        }
        return users;
    }

    /**
     * Returns a list of all detail records that match the specified search string in their
     * username or email address.
     */
    public List<DetailedUser> searchDetailRecords (String term)
    {
        String likeTerm = "%" + term + "%";
        List<DetailedUser> users = Lists.newArrayList();
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(OOOUserRecord.class));
        clauses.add(new Join(
                    OOOUserRecord.USER_ID, OOOAuxDataRecord.USER_ID).setType(Join.Type.LEFT_OUTER));
        clauses.add(new Where(Ops.or(OOOUserRecord.USERNAME.like(likeTerm),
                        OOOUserRecord.EMAIL.like(likeTerm))));
        clauses.add(OrderBy.descending(OOOUserRecord.USER_ID));
        for (DetailedUserRecord record : findAll(DetailedUserRecord.class, clauses)) {
            users.add(record.toDetailedUser());
        }
        return users;
    }

    /**
     * Creates a pending record for an account that has been created but not yet validated (which
     * involves the user accessing a secret URL we send to them in an email message).
     */
    public ValidateRecord createValidateRecord (int userId, boolean persist)
    {
        // delete any old validate mappings for the user
        deleteAll(ValidateDepotRecord.class, new Where(ValidateDepotRecord.USER_ID.eq(userId)));

        // create a new one and insert it into the database
        ValidateRecord rec = ValidateRecord.create(userId, persist);
        insert(ValidateDepotRecord.fromValidateRecord(rec));
        return rec;
    }

    /**
     * Fetches the validate record matching the specified secret and removes it from the pending
     * validations table.
     */
    public ValidateRecord getValidateRecord (final String secret)
    {
        ValidateDepotRecord record = load(ValidateDepotRecord.getKey(secret));
        if (record == null) {
            return null;
        }
        delete(record);
        return record.toValidateRecord();
    }

    /**
     * Fetches the validate record for the specifed user.
     */
    public ValidateRecord getValidateRecord (final int userId)
    {
        ValidateDepotRecord record =
                load(ValidateDepotRecord.class, new Where(ValidateDepotRecord.USER_ID, userId));
        return record == null ? null : record.toValidateRecord();
    }

    /**
     * Purges expired validation records from the table.
     */
    public void purgeValidationRecords ()
    {
        deleteAll(ValidateDepotRecord.class, new Where(ValidateDepotRecord.INSERTED.lessThan(
                        Calendars.now().zeroTime().addMonths(-1).toSQLDate())));
    }

    /**
     * Returns the total number of registered users and the number of users that registered in the
     * last 24 hours. We love the stats.
     */
    public int[] getRegiStats ()
    {
        int[] data = new int[2];
        data[0] = load(CountRecord.class, new FromOverride(HistoricalUserRecord.class)).count;
        data[1] = load(CountRecord.class, new FromOverride(HistoricalUserRecord.class),
                    new Where(HistoricalUserRecord.CREATED, new Date(System.currentTimeMillis()))
                ).count;
        return data;
    }

    /**
     * Returns the count of registrations per day for the last <code>limit</code> days. The
     * returned tuple array contains <code>({@link Date}, count)</code> pairs.
     */
    public List<Tuple<Date,Integer>> getRecentRegCount (int limit)
    {
        List<Tuple<Date, Integer>> list = Lists.newArrayList();
        for (RecentUserRecord recent : findAll(RecentUserRecord.class,
                    new FromOverride(HistoricalUserRecord.class),
                    new GroupBy(HistoricalUserRecord.CREATED),
                    OrderBy.descending(HistoricalUserRecord.CREATED),
                    new Limit(0, limit))) {
            list.add(new Tuple<Date, Integer>(recent.created, recent.entries));
        }
        return list;
    }

    /**
     * Returns a new Set that is a subset of the names in the provided collection, the new Set
     * containing only usernames that have purchased coins. The original collection is not
     * modified.
     */
    public Set<String> filterCoinBuyers (final Collection<String> usernames)
    {
        Set<String> filtered = Sets.newHashSet();
        List<OOOUserRecord> records = findAll(OOOUserRecord.class,
                new FromOverride(OOOUserRecord.class, OOOBillAuxDataRecord.class),
                new Where(Ops.and(
                        OOOUserRecord.USERNAME.in(usernames),
                        OOOBillAuxDataRecord.USER_ID.eq(OOOUserRecord.USER_ID),
                        Ops.not(OOOBillAuxDataRecord.FIRST_COIN_BUY.isNull()))));
        for (OOOUserRecord record : records) {
            filtered.add(record.username);
        }
        return filtered;
    }

    /**
     * Returns a new Set that is a subset of the userIds in the provided collection, the new Set
     * containing only userIds that have purchased coins for the first time in the interval
     * provided.  The original collection is not modified.
     */
    public Set<Integer> filterNewCoinBuyers (
        final Collection<Integer> userIds, final Date start, final Date end)
    {
        Set<Integer> filtered = Sets.newHashSet();
        for (OOOBillAuxDataRecord record : findAll(OOOBillAuxDataRecord.class,
                    new Where(Ops.and(
                            OOOBillAuxDataRecord.USER_ID.in(userIds),
                            OOOBillAuxDataRecord.FIRST_COIN_BUY.greaterEq(start),
                            OOOBillAuxDataRecord.FIRST_COIN_BUY.lessEq(end))))) {
            filtered.add(record.userId);
        }
        return filtered;
    }

    /**
     * Loads up the aux data record for the specified user. Returns null if none exists for that id.
     */
    public OOOAuxData getAuxRecord (int userId)
    {
        OOOAuxDataRecord record = load(OOOAuxDataRecord.class,
                new Where(OOOAuxDataRecord.USER_ID.eq(userId)));
        return record == null ? null : record.toOOOAuxData();
    }

    /**
     * Loads up the billing aux data record for the specified user. Returns a blank record (with
     * userId filled in) if none exists for that id.
     */
    public OOOBillAuxData getBillAuxData (int userId)
    {
        OOOBillAuxDataRecord bauxr = load(OOOBillAuxDataRecord.class,
                new Where(OOOBillAuxDataRecord.USER_ID.eq(userId)));
        if (bauxr == null) {
            OOOBillAuxData baux = new OOOBillAuxData();
            baux.userId = userId;
            return baux;
        }
        return bauxr.toOOOBillAuxData();
    }

    /**
     * Updates the supplied record in the database, creating the record if necessary.
     */
    public void updateBillAuxData (OOOBillAuxData record)
    {
        store(OOOBillAuxDataRecord.fromOOOBillAuxData(record));
    }

    /**
     * Gets the number of users referred by affiliates who bought coins within the given date range.
     */
    public IntIntMap getAffiliateCoinBuyerCount (Date start, Date end)
        throws PersistenceException
    {
        return getAffiliateUserCount(
            createAffiliateClause(start, end),
            new Join(OOOUserRecord.class,
                Ops.and(HistoricalUserRecord.USER_ID.eq(OOOUserRecord.USER_ID),
                    OOOUserRecord.FLAGS.bitAnd(OOOUser.HAS_BOUGHT_COINS_FLAG).notEq(0)
                )
            )
        );
    }

    /**
     * Gets the number of users referred by a particular affiliate who bought coins for each date
     * within the given date range.
     */
    public Map<Date, Integer> getAffiliateCoinBuyerCounts (Date start, Date end, int affiliateId)
        throws PersistenceException
    {
        return getAffiliateCounts(
            createAffiliateClause(start, end, affiliateId),
            new Join(OOOUserRecord.class,
                Ops.and(HistoricalUserRecord.USER_ID.eq(OOOUserRecord.USER_ID),
                    OOOUserRecord.FLAGS.bitAnd(OOOUser.HAS_BOUGHT_COINS_FLAG).notEq(0)
                )
            )
        );
    }

    /**
     * Gets the number of users created per affiliate in the given date range.
     */
    public IntIntMap getAffiliateRegistrationCount (Date start, Date end)
        throws PersistenceException
    {
        return getAffiliateUserCount(createAffiliateClause(start, end));
    }

    /**
     * Gets the number of users created for a particular affiliate per day in the given date range.
     */
    public Map<Date,Integer> getAffiliateRegistrationCounts (Date start, Date end, int affiliateId)
        throws PersistenceException
    {
        return getAffiliateCounts(createAffiliateClause(start, end, affiliateId));
    }

    /**
     * Creates a where clause for users created within the given date range.
     */
    protected static Where createAffiliateClause (Date start, Date end)
        throws PersistenceException
    {
        return new Where(
            Ops.and(
                HistoricalUserRecord.CREATED.greaterEq(start),
                HistoricalUserRecord.CREATED.lessEq(end)
            )
        );
    }

    /**
     * Creates a where clause for users created within the given date range referred by the given
     * affiliate id.
     */
    public static Where createAffiliateClause (Date start, Date end, int affiliateId)
    {
        return new Where(
            Ops.and(
                HistoricalUserRecord.CREATED.greaterEq(start),
                HistoricalUserRecord.CREATED.lessEq(end),
                HistoricalUserRecord.SITE_ID.eq(affiliateId)
            )
        );
    }

    /**
     * Helper function for affiliate user count queries.
     */
    protected IntIntMap getAffiliateUserCount (QueryClause ... clauses)
        throws PersistenceException
    {
        List<QueryClause> queryClauses = Lists.newArrayList();
        queryClauses.add(new FromOverride(HistoricalUserRecord.class));
        for (QueryClause additional : clauses) {
            if (additional != null) {
                queryClauses.add(additional);
            }
        }
        queryClauses.add(new GroupBy(AffiliateUserCountRecord.SITE_ID));
        List<AffiliateUserCountRecord> userCounts = findAll(AffiliateUserCountRecord.class,
            queryClauses);

        IntIntMap results = new IntIntMap();
        for (AffiliateUserCountRecord record : userCounts) {
            results.put(record.siteId, record.count);
        }
        return results;
    }

    /**
     * Helper function for affiliate user count queries.
     */
    protected Map<Date,Integer> getAffiliateCounts (QueryClause ... clauses)
        throws PersistenceException
    {
        List<QueryClause> queryClauses = Lists.newArrayList();
        queryClauses.add(new FromOverride(HistoricalUserRecord.class));
        for (QueryClause additional : clauses) {
            queryClauses.add(additional);
        }
        queryClauses.add(new GroupBy(AffiliateCountRecord.CREATED));
        queryClauses.add(OrderBy.descending(AffiliateCountRecord.CREATED));
        List<AffiliateCountRecord> userCounts = findAll(AffiliateCountRecord.class, queryClauses);
        Map<Date,Integer> results = new LinkedHashMap<Date,Integer>();
        for (AffiliateCountRecord record : userCounts) {
            results.put(record.created, record.count);
        }
        return results;
    }

    /**
     * Returns the max userid currently in use.
     */
    protected int getMaxUserId ()
    {
        return load(MaxUserRecord.class,
                new FromOverride(OOOUserRecord.class),
                new FieldOverride(MaxUserRecord.USER_ID, Funcs.max(OOOUserRecord.USER_ID))).userId;
    }

    /**
     * Returns the number of free accounts that have been played at least
     * once from this machine ident and were created vaguely recently.
     * Returns the number of free accounts that have been played at least once from this machine
     * ident and were created vaguely recently.
     *
     * @param daysInThePast is a negative number representing days to go back.
     */
    protected int playedRecentFreeAccounts (String machIdent, int daysInThePast)
    {
        Date since = Calendars.now().addDays(daysInThePast).toSQLDate();
        return load(CountRecord.class, new FromOverride(OOOUserRecord.class, UserIdentRecord.class),
            new Where(Ops.and(
                OOOUserRecord.USER_ID.eq(UserIdentRecord.USER_ID),
                Ops.not(Ops.like(OOOUserRecord.USERNAME, "%=%")),
                UserIdentRecord.MACH_IDENT.eq(machIdent),
                OOOUserRecord.FLAGS.bitAnd(OOOUser.HAS_BOUGHT_COINS_FLAG).eq(0),
                OOOUserRecord.CREATED.greaterThan(since))))
            .count;
    }

    /**
     * Converts a possibly null OOOUserRecord to a OOOUser.
     */
    protected OOOUser toUser (OOOUserRecord record)
    {
        return (record == null ? null : record.toUser());
    }

    /**
     * Optionally resolves machine identifiers for the supplied user.
     */
    protected OOOUser resolveIdents (OOOUser user, boolean loadIdents)
    {
        if (user != null && loadIdents) {
            user.machIdents = loadMachineIdents(user.userId);
        }
        return user;
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(AffiliateTagRecord.class);
        classes.add(BannedIdentRecord.class);
        classes.add(HistoricalUserRecord.class);
        classes.add(OOOAuxDataRecord.class);
        classes.add(OOOUserRecord.class);
        classes.add(SessionRecord.class);
        classes.add(TaintedIdentRecord.class);
        classes.add(UserIdentRecord.class);
        classes.add(ValidateDepotRecord.class);
    }

    protected static final Builder3<OOOUserCard, Integer, String, Integer> BUILD_OOO_USER_CARD =
            new Builder3<OOOUserCard, Integer, String, Integer>() {
        public OOOUserCard build (Integer userId, String userName, Integer flags) {
            return new OOOUserCard(userId, userName, flags);
        }
    };

    /** The number of days in the past from now where we no longer
     * consider an account as 'recent' */
    protected static final int RECENT_ACCOUNT_CUTOFF = -3*30;

    /** The number of free accounts that can be created per machine. */
    protected static final int MAX_FREE_ACCOUNTS_PER_MACHINE = 2;
}
