
package edu.ucla.library.iiif.auth.delegate.hauth;

/**
 * Expected access mode values: OPEN, TIERED, or ALL_OR_NOTHING. These determine the level of access a requested item is
 * allowed.
 *
 * @see <a href="https://iiif.io/api/auth/1.0/#interaction-with-access-controlled-resources">Interaction with
 *      Access-Controlled Resources </a>
 */
public enum AccessMode {

    /** An open access mode. */
    OPEN,

    /** A tiered access mode. */
    TIERED,

    /** An all-or-nothing access mode. */
    ALL_OR_NOTHING;
}
