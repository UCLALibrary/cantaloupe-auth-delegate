package edu.ucla.library.iiif.auth.delegate.hauth;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * An authorization token created by the Hauth service.
 */
public class HauthToken {

    /**
     * An authorization header.
     * <p>
     * N.B.: Cantaloupe's request context uses a map with lowercase keys.
     */
    @JsonIgnore
    public static final String HEADER = "authorization";

    /**
     * The type of authorization token this is.
     */
    @JsonIgnore
    public static final String TYPE = "Bearer";

    /**
     * A JSON key for the Hauth version.
     */
    private static final String HAUTH_VERSION_KEY = "version";

    /**
     * A JSON key for the campus network information.
     */
    private static final String CAMPUS_NETWORK_KEY = "campus-network";

    /**
     * The version of Hauth that sent this token.
     */
    private String myVersion;

    /**
     * Whether the IP is valid for the campus network.
     */
    private boolean isNetworkIP;

    /**
     * Provides a default constructor for Jackson to use in (de)serialization.
     */
    @SuppressWarnings("unused")
    private HauthToken() {
        // This is intentionally left empty
    }

    /**
     * Creates a new Hauth token.
     *
     * @param aHauthVersion The version of Hauth that sent the token
     * @param aValidIP Whether the IP was a campus network IP
     */
    public HauthToken(final String aHauthVersion, final boolean aValidIP) {
        myVersion = aHauthVersion;
        isNetworkIP = aValidIP;
    }

    /**
     * Sets the version of the authorization token.
     *
     * @param aVersion A token version
     * @return The authorization token
     */
    @JsonSetter(HAUTH_VERSION_KEY)
    public HauthToken setVersion(final String aVersion) {
        myVersion = aVersion;
        return this;
    }

    /**
     * Gets the version of the authorization token.
     *
     * @return The version of the authorization token
     */
    @JsonGetter(HAUTH_VERSION_KEY)
    public String getVersion() {
        return myVersion;
    }

    /**
     * Sets whether the token comes from a valid IP.
     *
     * @param aValidIP Whether the token comes from a valid IP
     * @return The authorization token
     */
    @JsonSetter(CAMPUS_NETWORK_KEY)
    public HauthToken setValidIP(final boolean aValidIP) {
        isNetworkIP = aValidIP;
        return this;
    }

    /**
     * Gets whether the token comes from a valid IP.
     *
     * @return True if the token comes from a valid IP; else, false
     */
    @JsonGetter(CAMPUS_NETWORK_KEY)
    public boolean isValidIP() {
        return isNetworkIP;
    }

}
