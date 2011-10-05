//
// $Id$

package com.threerings.user;

/**
 * Maintains a mapping from an arbitrary text tag to an integer identifier.
 */
public class AffiliateTag
{
    /** Value indicating that the affiliate did not provide a tag **/
    public static final int NO_TAG = 0; 

    /** The automatically generated tag id. */
    public int tagId;

    /** The arbitrary text tag. */
    public String tag;
    
}
