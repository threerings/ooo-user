//
// $Id$

package com.threerings.user;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import com.samskivert.util.Interval;

import static com.threerings.user.Log.log;

/**
 * Tracks logins by a user identifier and if they attempt to login too often, lets us know.
 * Note that the period is how often we clear the entire table, so specific users may find
 * up to 2x the attempt rate during their first set of attempts if it happens to cross the
 * boundary.
 */
public class LoginThrottle<K>
{
    public LoginThrottle (int maxLogins, long period)
    {
        _maxLogins = maxLogins;

        (new Interval() {
            @Override
            public void expired ()
            {
                synchronized(_recentLogins) {
                    _recentLogins.clear();
                }
            }
        }).schedule(period, true);
    }

    /**
     * Notes that the user attempted to login.
     * @return Whether they're allowed to continue based on throttle settings.
     */
    @Deprecated
    public boolean noteLogin (K userIdentifier)
    {
        synchronized(_recentLogins) {
            int loginCount = 1 + _recentLogins.add(userIdentifier, 1);
            if (loginCount > _maxLogins) {
                recordThrottledAttempt(userIdentifier, loginCount);
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Notes that the user successfully logged in (so reduces their count by 1).
     */
    @Deprecated
    public void noteLoginSuccess (K userIdentifier)
    {
        synchronized(_recentLogins) {
            _recentLogins.add(userIdentifier, -1);
        }
    }

    /**
     * Should we block a login attempt because the user has already failed too many times?
     */
    public boolean isLoginAttemptBlocked (K userIdentifier)
    {
        int count;
        synchronized (_recentLogins) {
            count = _recentLogins.count(userIdentifier);
        }
        if (_maxLogins <= count) {
            recordThrottledAttempt(userIdentifier, count);
            return true;
        }
        return false;
    }

    /**
     * Note a failed login, and return true if it's time to tell them that we'll block
     * them for a little while.
     */
    public boolean noteFailedLogin (K userIdentifier)
    {
        synchronized (_recentLogins) {
            return _maxLogins <= 1 + _recentLogins.add(userIdentifier, 1);
        }
    }

    /**
     * Log that we had a failed attempt. Can be overridden if you really don't care about tracking
     * that sort of thing.
     */
    protected void recordThrottledAttempt (K userIdentifier, int loginCount)
    {
        log.info("Throttled login attempt", "identifier", userIdentifier, "loginCount", loginCount);
    }

    /** How many logins they're allowed to try during a period. */
    protected int _maxLogins;

    /** Recent login attempt counts by user identifier. */
    protected Multiset<K> _recentLogins = HashMultiset.create();
}
