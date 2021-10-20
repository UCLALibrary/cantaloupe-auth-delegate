package edu.ucla.library.iiif.auth.delegate.hauth;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * A response from the Hauth access service.
 */
class HauthResponse {

    /**
     * The JSON key for the response's item ID.
     */
    static final String ID_KEY = "id";

    /**
     * The JSON key for the response's access level.
     */
    static final String ACCESS_KEY = "restricted";

    /**
     * An item ID.
     */
    private String myID;

    /**
     * Whether the response includes a restricted item.
     */
    private boolean myItemIsRestricted;

    /**
     * Gets whether the response includes a restricted item.
     *
     * @return True if the response contains a restricted item; else, false
     */
    @JsonGetter(ACCESS_KEY)
    boolean isRestricted() {
        return myItemIsRestricted;
    }

    /**
     * Sets whether the response includes a restricted item.
     *
     * @param aRestrictedItem True if the response contains a restricted item; else, false
     * @return This response
     */
    @JsonSetter(ACCESS_KEY)
    HauthResponse setRestriction(final boolean aRestrictedItem) {
        myItemIsRestricted = aRestrictedItem;
        return this;
    }

    /**
     * Gets the item ID that this response references.
     *
     * @return The item ID
     */
    @JsonGetter(ID_KEY)
    String getID() {
        return myID;
    }

    /**
     * Sets the item ID referenced by this response.
     *
     * @param aID An item ID
     * @return This response
     */
    @JsonSetter(ID_KEY)
    HauthResponse setID(final String aID) {
        myID = aID;
        return this;
    }

}
