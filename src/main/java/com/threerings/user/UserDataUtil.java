//
// $Id$

package com.threerings.user;

import javax.servlet.http.HttpServletRequest;

import com.samskivert.net.MailUtil;
import com.samskivert.servlet.util.DataValidationException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

/**
 * Miscellaneous user data related routines.
 */
public class UserDataUtil
{
    /** The minimum length of a valid password. */
    public static final int MIN_PASSWORD_LENGTH = 4;

    /** The minimum length of a valid username. */
    public static final int MIN_USERNAME_LENGTH = 3;

    /** The maximum length of a valid username. */
    public static final int MAX_USERNAME_LENGTH = 12;

    /**
     * Returns the username in the supplied request, or throws an
     * informative exception if the username is absent or invalid.
     */
    public static String getUsername (HttpServletRequest req, String page)
        throws DataValidationException
    {
        String username = ParameterUtil.requireParameter(
            req, "username", page + ".error.missing_username");
        username = username.trim();
        int len = username.length();
        if (len < MIN_USERNAME_LENGTH || len > MAX_USERNAME_LENGTH ||
            !username.matches("[a-zA-Z0-9_]+")) {
            throw new DataValidationException(page + ".error.invalid_username");
        }
        return username;
    }

    /**
     * Returns the password in the supplied request, or throws an
     * informative exception if the password is absent or invalid.
     */
    public static String getPassword (
        HttpServletRequest req, String page, String ptype)
        throws DataValidationException
    {
        return getPassword(req, page, ptype, false);
    }

    /**
     * Returns the password in the supplied request, or throws an
     * informative exception if the password is absent or invalid.
     *
     * @param optional If true, null may be returned and if the parameter is absent no exception
     *           will be thrown. If the parameter is present it is still checked for validity.
     */
    public static String getPassword (
        HttpServletRequest req, String page, String ptype, boolean optional)
        throws DataValidationException
    {
        String password = optional ? ParameterUtil.getParameter(req, ptype, true) :
            ParameterUtil.requireParameter(req, ptype, page + ".error.missing_" + ptype);
        if (password != null) {
            checkPassword(page, password, ptype);
        }
        return password;
    }

    /**
     * Throws an informative exception if the password is absent or
     * invalid.
     */
    public static void checkPassword (
        String page, String password, String ptype)
        throws DataValidationException
    {
        int len = password.length();
        if (len < MIN_PASSWORD_LENGTH) {
            throw new DataValidationException(
                page + ".error.invalid_" + ptype);
        }
    }

    /**
     * Returns the email in the supplied request, or throws an informative
     * exception if the email is absent or invalid.
     */
    public static String getEmail (
        HttpServletRequest req, String page, boolean require)
        throws DataValidationException
    {
        String email = ParameterUtil.getParameter(req, "email", true);
        if (StringUtil.isBlank(email)) {
            if (require) {
                throw new DataValidationException(
                    page + ".error.missing_email");
            }
            return "";
        }
        email = email.trim();
        if (email.length() > MAX_EMAIL_LENGTH ||
            !MailUtil.isValidAddress(email)) {
            throw new DataValidationException(page + ".error.invalid_email");
        }
        return email;
    }

    /**
     * Returns the specified partial (first or last) name in the supplied
     * request, or throws an informative exception if the name is absent
     * or invalid.
     */
    public static String getName (
        HttpServletRequest req, String page, String pname)
        throws DataValidationException
    {
        String name = ParameterUtil.requireParameter(
            req, pname, page + ".error.missing_" + pname).trim();
        int len = name.length();
        if (len < MIN_NAME_LENGTH || len > MAX_NAME_LENGTH ||
            !name.matches("[a-zA-Z]+")) {
            throw new DataValidationException(
                page + ".error.invalid_" + pname);
        }
        return name;
    }

    /**
     * Returns the state in the supplied request, or throws an informative
     * exception if the state is absent or invalid.
     */
    public static String getState (HttpServletRequest req, String page)
        throws DataValidationException
    {
        String state = ParameterUtil.getParameter(req, "state", false);
        state = state.trim();
        if (state.length() > MAX_STATE_LENGTH) {
            throw new DataValidationException(
                page + ".error.invalid_state");
        }
        return state;
    }

    /**
     * Returns the country in the supplied request, or throws an
     * informative exception if the country is absent or invalid.
     */
    public static String getCountry (HttpServletRequest req, String page)
        throws DataValidationException
    {
        String country = ParameterUtil.requireParameter(
            req, "country", page + ".error.missing_country");
        country = country.trim();
        int len = country.length();
        if (len < MIN_COUNTRY_LENGTH || len > MAX_COUNTRY_LENGTH) {
            throw new DataValidationException(
                page + ".error.invalid_country");
        }
        return country;
    }

    /**
     * Returns the missive in the supplied request, or throws an
     * informative exception if the missive is absent or invalid.
     */
    public static String getMissive (HttpServletRequest req, String page)
        throws DataValidationException
    {
        String missive = ParameterUtil.getParameter(req, "missive", false);
        missive = missive.trim();
        if (missive.length() > MAX_MISSIVE_LENGTH) {
            throw new DataValidationException(
                page + ".error.invalid_missive");
        }
        return missive;
    }

    /**
     * Returns 1 if the named parameter is present in the supplied
     * request, 0 if not.
     */
    public static byte isChecked (HttpServletRequest req, String name)
        throws DataValidationException
    {
        String value = ParameterUtil.getParameter(req, name, true);
        return (byte)((value != null) ? 1 : 0);
    }

    /**
     * Returns the value of the given select menu, or throws an error if
     * the value is not defined or 0 (denoting the default "[Select One]"
     * selection.)
     */
    public static byte requireSelectItem (
        HttpServletRequest req, String name, String error)
        throws DataValidationException
    {
        byte value = (byte)ParameterUtil.getIntParameter(req, name, 0, error);
        if (value == 0) {
            throw new DataValidationException(error);
        }
        return value;
    }

    /** The maximum length of a valid email address. */
    protected static final int MAX_EMAIL_LENGTH = 128;

    /** The minimum length of a valid name in characters. */
    protected static final int MIN_NAME_LENGTH = 1;

    /** The maximum length of a valid name in characters. */
    protected static final int MAX_NAME_LENGTH = 63;

    /** The minimum birth year a user can enter.  God help us if we have
     * many centegenarians. */
    protected static final int MIN_BIRTHYEAR = 1900;

    /** The minimum birth date a user can enter. */
    protected static final int MIN_BIRTHDATE = 1;

    /** The maximum birth date a user can enter. */
    protected static final int MAX_BIRTHDATE = 31;

    /** The minimum length of a valid country in characters. */
    protected static final int MIN_COUNTRY_LENGTH = 2;

    /** The maximum length of a valid state in characters. */
    protected static final int MAX_STATE_LENGTH = 64;

    /** The maximum length of a valid country in characters. */
    protected static final int MAX_COUNTRY_LENGTH = 64;

    /** The maximum length of a valid missive in characters. */
    protected static final int MAX_MISSIVE_LENGTH = 32768;
}
