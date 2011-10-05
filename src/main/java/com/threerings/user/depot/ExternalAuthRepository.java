//
// $Id$

package com.threerings.user.depot;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.util.Tuple;

import com.threerings.user.ExternalAuther;

/**
 * Maps authentication information from external sources (like Facebook, OAuth providers, etc.) to
 * our internal (ooouser) userId number.
 */
@Singleton
public class ExternalAuthRepository extends DepotRepository
{
    @Inject public ExternalAuthRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Returns the OOOUser id of the member with the supplied external credentials, or 0 if the
     * supplied creds are not mapped to a OOOUser account.
     */
    public int loadUserIdForExternal (ExternalAuther auther, String externalId)
    {
        Preconditions.checkNotNull(auther);
        ExternalAuthRecord exrec = load(ExternalAuthRecord.getKey(auther, externalId));
        return (exrec == null) ? 0 : exrec.userId;
    }

    /**
     * Returns the external id of the given OOOUser with the given ExternalAuther.
     */
    public String loadExternalIdForUser (ExternalAuther auther, int userId)
    {
        Preconditions.checkNotNull(auther);
        ExternalAuthRecord exrec = from(ExternalAuthRecord.class).where(
            ExternalAuthRecord.USER_ID.eq(userId).and(ExternalAuthRecord.AUTHER.eq(auther))).load();
        return (exrec == null) ? null : exrec.externalId;
    }

    /**
     * Returns the most recently saved session key for the supplied external account, or null if no
     * mapping exists for said account or no key has yet been stored for it.
     */
    public String loadExternalSessionKey (ExternalAuther auther, String externalId)
    {
        Preconditions.checkNotNull(auther);
        ExternalAuthRecord exrec = load(ExternalAuthRecord.getKey(auther, externalId));
        return (exrec == null) ? null : exrec.sessionKey;
    }

    /**
     * Returns (externalId, sessionKey) for the supplied user, or null if no mapping exists for
     * said user. The sessionKey may be null if no key has yet been stored.
     */
    public Tuple<String, String> loadExternalAuthInfo (ExternalAuther auther, int userId)
    {
        Preconditions.checkNotNull(auther);
        ExternalAuthRecord exrec = from(ExternalAuthRecord.class).where(
            ExternalAuthRecord.AUTHER.eq(auther), ExternalAuthRecord.USER_ID.eq(userId)).load();
        return (exrec == null) ? null : Tuple.newTuple(exrec.externalId, exrec.sessionKey);
    }

    /**
     * Loads a mapping of (exid -> ooouser id) for all members in the supplied list that are found
     * in the database.
     */
    public Map<String, Integer> loadUserIds (ExternalAuther auther, Collection<String> externalIds)
    {
        Preconditions.checkNotNull(auther);
        Map<String, Integer> ids = Maps.newHashMap();
        for (ExternalAuthRecord exrec : from(ExternalAuthRecord.class).where(
                 ExternalAuthRecord.AUTHER.eq(auther),
                 ExternalAuthRecord.EXTERNAL_ID.in(externalIds)).select()) {
            ids.put(exrec.externalId, exrec.userId);
        }
        return ids;
    }

    /**
     * Creates a mapping for the specified user to the supplied external credentials.
     */
    public void mapExternalAccount (int userId, ExternalAuther auther, String externalId,
                                    String sessionKey)
    {
        Preconditions.checkNotNull(auther);
        ExternalAuthRecord exrec = new ExternalAuthRecord();
        exrec.auther = auther;
        exrec.externalId = externalId;
        exrec.userId = userId;
        exrec.sessionKey = sessionKey;
        insert(exrec);
    }

    /**
     * Updates the session key on file for the specified user's mapping for the specified auther.
     */
    public void updateExternalSession (ExternalAuther auther, String externalId, String sessionKey)
    {
        Preconditions.checkNotNull(auther);
        updatePartial(ExternalAuthRecord.getKey(auther, externalId),
                      ExternalAuthRecord.SESSION_KEY, sessionKey);
    }


    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ExternalAuthRecord.class);
    }
}

