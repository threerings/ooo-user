//
// $Id$

package com.threerings.user;

import java.sql.Date;

import com.samskivert.util.StringUtil;

/**
 * A record detailing a user's non-critical responses to the Yohoho beta
 * registration.
 */
public class YoBetaInfo
{
    /** The user id of the user. */
    public int userId;

    /** The user's birthday. */
    public Date birthday;

    /** The user's state or province. */
    public String state;

    /** The user's country. */
    public String country;

    /** The user's connection speed. */
    public byte connection;

    /** The user's computer operating system. */
    public byte os;

    /** The user's computer CPU speed. */
    public byte cpu;

    /** The user's computer memory size. */
    public byte memory;

    /** Whether the user has played an MMORPG. */
    public byte playMmorpg;

    /** Whether the user has played a beta MMORPG. */
    public byte playBetaMmorpg;

    /** Whether the user has played a text MUD. */
    public byte playMud;

    /** Whether the user plays puzzle games. */
    public byte playPuzzle;

    /** The user's testing availability this summer. */
    public byte summer;

    /** The source from which the user heard about Yohoho. */
    public byte source;

    /** The user's personal missive to us. */
    public String missive;

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
