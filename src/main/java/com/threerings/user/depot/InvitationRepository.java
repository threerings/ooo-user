//
// $Id$

package com.threerings.user.depot;

import java.sql.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

/**
 * The invitation repository.
 */
@Singleton
public class InvitationRepository extends DepotRepository
{
    @Computed @Entity
    public static class CountRecord extends PersistentRecord
    {
        /** The computed count. */
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public InvitationRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(InvitationRecord.class,
                new SchemaMigration.Retype(3, InvitationRecord.INVITER_USER_ID));
        ctx.registerMigration(InvitationRecord.class,
                new SchemaMigration.Add(4, InvitationRecord.SENT, "'2000-01-01'"));
    }

    /**
     * Creates a new invitation record.
     */
    public InvitationRecord createInvitationRecord (String email)
    {
        return createInvitationRecord(email, 0);
    }

    /**
     * Creates a new invitation record.
     */
    public InvitationRecord createInvitationRecord (String email, String username)
    {
        OOOUserRecord user = load(
                OOOUserRecord.class, new Where(OOOUserRecord.USERNAME.eq(username)));
        return user == null ? null : createInvitationRecord(email, user.userId);
    }

    /**
     * Creates a new invitation record.
     */
    public InvitationRecord createInvitationRecord (String email, int inviterId)
    {
        InvitationRecord rec = new InvitationRecord();
        rec.email = email;
        rec.inviterUserId = inviterId;
        rec.created = new Date(System.currentTimeMillis());
        rec.sent = rec.created;
        for (int ii = 0; ii < 10; ii++) {
            rec.invitation = Long.toString(Math.abs((new Random()).nextLong()), 16);
            try {
                insert(rec);
            } catch (DuplicateKeyException e) {
                continue;
            }
            return rec;
        }
        return null;
    }

    /**
     * Creates a new multiple invitation record.
     */
    public MultipleInvitationRecord createMultipleInvitationRecord (String code, int max)
    {
        MultipleInvitationRecord rec = new MultipleInvitationRecord();
        rec.invitation = code;
        rec.maxInvitations = max;
        try {
            insert(rec);
        } catch (DuplicateKeyException e) {
            return null;
        }
        return rec;
    }

    /**
     * Fetches an invitation record.
     */
    public InvitationRecord getInvitation (String invitation)
    {
        return load(InvitationRecord.getKey(invitation));
    }

    /**
     * Returns a list of invitations starting with the specified record number and containing at
     * most <code>count</code> elements.
     */
    public List<InvitationRecord> getInvitationRecords (int start, int count)
    {
        return findAll(InvitationRecord.class, OrderBy.descending(InvitationRecord.CREATED),
                new Limit(start, count));
    }

    /**
     * Returns a list of unused invitations created before a certain date.
     */
    public List<InvitationRecord> getUnusedInvitationRecords (Date before)
    {
        return findAll(InvitationRecord.class,
                new Where(Ops.and(InvitationRecord.USER_ID.eq(0),
                        InvitationRecord.CREATED.lessEq(before),
                        InvitationRecord.SENT.lessEq(before))),
                OrderBy.ascending(InvitationRecord.EMAIL));
    }

    /**
     * Returns true if the multiple invitation code is valid and has invitations remaining.
     */
    public MultipleInvitationRecord getOpenMultipleInvitation (String code)
    {
        MultipleInvitationRecord rec = load(MultipleInvitationRecord.class,
                new Where(MultipleInvitationRecord.INVITATION.eq(code)));
        if (rec == null) {
            return null;
        }
        return isMultipleInvitationOpen(rec) ? rec : null;
    }

    /**
     * Returns a list of multiple invtations records.
     */
    public List<MultipleInvitationRecord> getMultipleInvitations ()
    {
        return findAll(MultipleInvitationRecord.class);
    }

    /**
     * Activates an invitation record.
     */
    public void activateInvitation (InvitationRecord rec, int userId)
    {
        rec.userId = userId;
        update(rec);
    }

    /**
     * Activates a multiple invitation.
     *
     * @return false if the user has already claimed an invitation
     */
    public boolean activateMultipleInvitation (int invitationId, int userId)
    {
        UserInvitationRecord uir = new UserInvitationRecord();
        uir.invitationId = invitationId;
        uir.userId = userId;
        try {
            insert(uir);
        } catch (DuplicateKeyException e) {
            return false;
        }
        return true;
    }

    /**
     * Updates the created date for a set of invitations.
     */
    public void pokeInvitations (List<String> invitations)
    {
        updatePartial(InvitationRecord.class,
                new Where(InvitationRecord.INVITATION.in(invitations)),
                KeySet.newKeySet(InvitationRecord.class, Lists.transform(invitations,
                        new Function<String, Key<InvitationRecord>>() {
                            public Key<InvitationRecord> apply (String invitation) {
                                return InvitationRecord.getKey(invitation);
                            }
                        })),
                InvitationRecord.SENT, new Date(System.currentTimeMillis()));
    }

    /**
     * Verifies a multiple invitation record is still open.
     */
    protected boolean isMultipleInvitationOpen (MultipleInvitationRecord rec)
    {
        return (rec.maxInvitations > load(CountRecord.class,
                new FromOverride(UserInvitationRecord.class),
                new Where(UserInvitationRecord.INVITATION_ID.eq(rec.invitationId))).count);
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(InvitationRecord.class);
        classes.add(MultipleInvitationRecord.class);
        classes.add(UserInvitationRecord.class);
    }
}
