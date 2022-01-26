
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.COLON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A configuration class.
 */
public final class Config {

    /**
     * An environmental property for the URI of the access cookie service.
     */
    public static final String AUTH_COOKIE_SERVICE = "AUTH_COOKIE_SERVICE";

    /**
     * An environmental property for the URI of the access token service.
     */
    public static final String AUTH_TOKEN_SERVICE = "AUTH_TOKEN_SERVICE";

    /**
     * An environmental property for the URI of the Sinai access token service.
     */
    public static final String SINAI_AUTH_TOKEN_SERVICE = "SINAI_AUTH_TOKEN_SERVICE";

    /**
     * An environmental property for the URI of an item access mode service.
     */
    public static final String AUTH_ACCESS_SERVICE = "AUTH_ACCESS_SERVICE";

    /**
     * An environmental property for the scale constraint to use for tiered access.
     * <p>
     * See https://cantaloupe-project.github.io/manual/5.0/access-control.html#Tiered%20Access for more information.
     */
    public static final String TIERED_ACCESS_SCALE_CONSTRAINT = "TIERED_ACCESS_SCALE_CONSTRAINT";

    /**
     * A configured cookie service.
     */
    private URI myCookieService;

    /**
     * A configured token service.
     */
    private URI myTokenService;

    /**
     * A configured Sinai token service.
     */
    private URI mySinaiTokenService;

    /**
     * A configured access mode service.
     */
    private URI myAccessService;

    /**
     * A configured tiered access scale constraint.
     */
    private int[] myScaleConstraint;

    /**
     * Creates a new configuration.
     */
    public Config() {
        setScaleConstraint(getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT));

        myCookieService = getURI(Config.AUTH_COOKIE_SERVICE);
        myTokenService = getURI(Config.AUTH_TOKEN_SERVICE);
        mySinaiTokenService = getURI(Config.SINAI_AUTH_TOKEN_SERVICE);
        myAccessService = getURI(Config.AUTH_ACCESS_SERVICE);
    }

    /**
     * Creates a new configuration.
     *
     * @param aCookieService A cookie service URI
     * @param aTokenService A token service URI
     * @param aSinaiTokenService A Sinai token service URI
     * @param aAccessService An access mode service URI
     * @param aScaleConstraint A scale constraint for tiered access
     */
    public Config(final URI aCookieService, final URI aTokenService, final URI aSinaiTokenService,
            final URI aAccessService, final String aScaleConstraint) {
        setScaleConstraint(aScaleConstraint);

        myCookieService = aCookieService;
        myTokenService = aTokenService;
        mySinaiTokenService = aSinaiTokenService;
        myAccessService = aAccessService;
    }

    /**
     * Gets the configured cookie service URI.
     *
     * @return The configured cookie service URI
     */
    public URI getCookieService() {
        return myCookieService;
    }

    /**
     * Sets the cookie service URI.
     *
     * @param aCookieService A cookie service
     * @return This configuration
     */
    public Config setCookieService(final URI aCookieService) {
        myCookieService = aCookieService;
        return this;
    }

    /**
     * Gets the configured token service URI.
     *
     * @return The configured token service URI
     */
    public URI getTokenService() {
        return myTokenService;
    }

    /**
     * Sets the token service URI.
     *
     * @param aTokenService A token service
     * @return This configuration
     */
    public Config setTokenService(final URI aTokenService) {
        myTokenService = aTokenService;
        return this;
    }

    /**
     * Gets the configured Sinai token service URI.
     *
     * @return The configured Sinai token service URI
     */
    public URI getSinaiTokenService() {
        return mySinaiTokenService;
    }

    /**
     * Sets the Sinai token service URI.
     *
     * @param aSinaiTokenService A Sinai token service
     * @return This configuration
     */
    public Config setSinaiTokenService(final URI aSinaiTokenService) {
        mySinaiTokenService = aSinaiTokenService;
        return this;
    }

    /**
     * Gets the configured access mode service URI.
     *
     * @return The configured access mode service URI
     */
    public URI getAccessService() {
        return myAccessService;
    }

    /**
     * Sets an access mode service URI.
     *
     * @param aAccessService An access mode service
     * @return This configuration
     */
    public Config setAccessService(final URI aAccessService) {
        myAccessService = aAccessService;
        return this;
    }

    /**
     * Gets the configured scale constraint for tiered access.
     *
     * @return The configured scale constraint for tiered access
     */
    public int[] getScaleConstraint() {
        return myScaleConstraint.clone();
    }

    /**
     * Sets a scale constraint for tiered access.
     *
     * @param aScaleConstraint A scale constraint for tiered access
     * @return This configuration
     */
    public Config setScaleConstraint(final String aScaleConstraint) {
        try {
            myScaleConstraint = Stream.of(aScaleConstraint.split(COLON)).mapToInt(Integer::parseInt).toArray();
        } catch (final NumberFormatException details) {
            throw new ConfigException(details, aScaleConstraint);
        }

        // Must be able to be mapped to an array of length 2, and numerator must be less than the denominator
        if (myScaleConstraint.length != 2 || myScaleConstraint[0] >= myScaleConstraint[1]) {
            throw new ConfigException(aScaleConstraint);
        }

        return this;
    }

    /**
     * Gets an environmental property as a URI, checking that it exists and is valid.
     *
     * @param aPropertyName An environmental property name
     * @return The property value
     * @throws ConfigException If the supplied property name doesn't exist in the environment or is invalid
     */
    static URI getURI(final String aPropertyName) {
        try {
            return new URI(Optional.ofNullable(System.getenv(aPropertyName))
                    .orElseThrow(() -> new ConfigException(aPropertyName)));
        } catch (final URISyntaxException details) {
            throw new ConfigException(details, aPropertyName);
        }
    }

    /**
     * Gets an environmental property, checking that it exists.
     *
     * @param aPropertyName An environmental property name
     * @return The property value
     * @throws ConfigException If the supplied property name doesn't exist in the environment
     */
    static String getString(final String aPropertyName) {
        return Optional.ofNullable(System.getenv(aPropertyName)).orElseThrow(() -> new ConfigException(aPropertyName));
    }
}
