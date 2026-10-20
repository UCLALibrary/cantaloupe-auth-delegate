
package edu.ucla.library.iiif.auth.delegate;

/**
 * Expected access mode values: OPEN, TIERED, or ALL_OR_NOTHING. These determine the level of access a requested item is
 * allowed, and are leftovers from the Hauth DB.
 */
public enum AccessMode {

    /** An open access mode. The database value for this is zero. */
    OPEN,

    /** A tiered access mode. The database value for this is one. */
    TIERED,

    /** An all-or-nothing access mode. The database value for this is two. */
    ALL_OR_NOTHING;

}
