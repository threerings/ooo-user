//
// $Id$

package com.threerings.user;

/**
 * Defines an XML-RPC interface to certain ooouser services. This is implemented by the register
 * webapp, but can be implemented by other systems that wish to provide API compatibility with a
 * potentially different backend implementation.
 */
public interface OOOXmlRpcService
{
    /**
     * Returns true if the supplied credentials are valid, false otherwise.
     *
     * @param username the name of the user in question.
     * @param password the MD5 encoded password for the user.
     */
    boolean authUser (String username, String password);
}
