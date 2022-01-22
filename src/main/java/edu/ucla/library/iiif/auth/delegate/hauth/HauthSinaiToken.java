package edu.ucla.library.iiif.auth.delegate.hauth;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * An authorization token created by the Sinai application.
 */
public class HauthSinaiToken {

    /**
     * An authorization header.
     * <p>
     * N.B.: The keys in Cantaloupe's request context are capitalized with HTTP/1.1, and lowercased with HTTP/2.
     */
    @JsonIgnore
    public static final String HEADER = "Authorization";

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
     * A JSON key for the Sinai affiliate information.
     */
    private static final String SINAI_AFFILIATE_KEY = "sinaiAffiliate";

    /**
     * The version of Hauth that sent this token.
     */
    private String myVersion;

    /**
     * Whether the bearer is affiliated with Sinai.
     */
    private boolean isSinaiAffiliate;

    /**
     * Provides a default constructor for Jackson to use in (de)serialization.
     */
    @SuppressWarnings("unused")
    private HauthSinaiToken() {
        // This is intentionally left empty
    }

    /**
     * Creates a new Hauth token.
     *
     * @param aHauthVersion The version of Hauth that sent the token
     * @param aSinaiAffiliate Whether the bearer is affiliated with Sinai
     */
    public HauthSinaiToken(final String aHauthVersion, final boolean aSinaiAffiliate) {
        myVersion = aHauthVersion;
        isSinaiAffiliate = aSinaiAffiliate;
    }

    /**
     * Sets the version of the authorization token.
     *
     * @param aVersion A token version
     * @return The authorization token
     */
    @JsonSetter(HAUTH_VERSION_KEY)
    public HauthSinaiToken setVersion(final String aVersion) {
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
     * Sets whether the bearer is affiliated with Sinai.
     *
     * @param aSinaiAffiliate Whether the bearer is affiliated with Sinai
     * @return The authorization token
     */
    @JsonSetter(SINAI_AFFILIATE_KEY)
    public HauthSinaiToken setSinaiAffiliate(final boolean aSinaiAffiliate) {
        isSinaiAffiliate = aSinaiAffiliate;
        return this;
    }

    /**
     * Gets whether the bearer is affiliated with Sinai.
     *
     * @return True if the bearer is affiliated with Sinai; else, false
     */
    @JsonGetter(SINAI_AFFILIATE_KEY)
    public boolean hasSinaiAffiliate() {
        return isSinaiAffiliate;
    }

}
