
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.COLON;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A configuration class.
 */
public final class Config {

    /**
     * An environmental property for the URL of a IIIF authorization cookie service.
     */
    public static final String AUTH_COOKIE_SERVICE = "AUTH_COOKIE_SERVICE";

    /**
     * An environmental property for the URL of a IIIF authorization token service.
     */
    public static final String AUTH_TOKEN_SERVICE = "AUTH_TOKEN_SERVICE";

    /**
     * An environmental property for the URL of an item access service.
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
    private URL myCookieService;

    /**
     * A configured token service.
     */
    private URL myTokenService;

    /**
     * A configured access service.
     */
    private URL myAccessService;

    /**
     * A configured tiered access scale constraint.
     */
    private int[] myScaleConstraint;

    /**
     * Creates a new configuration.
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    public Config() {
        try {
            myCookieService = new URL(getProperty(Config.AUTH_COOKIE_SERVICE));
            myTokenService = new URL(getProperty(Config.AUTH_TOKEN_SERVICE));
            myAccessService = new URL(getProperty(Config.AUTH_ACCESS_SERVICE));
        } catch (final MalformedURLException details) {
            throw new ConfigException(details.getMessage());
        }

        setScaleConstraint(getProperty(Config.TIERED_ACCESS_SCALE_CONSTRAINT));
    }

    /**
     * Creates a new configuration.
     *
     * @param aCookieService A cookie service URL
     * @param aTokenService A token service URL
     * @param aAccessService An access service URL
     * @param aScaleConstraint A scale constraint for tiered access
     */
    public Config(final URL aCookieService, final URL aTokenService, final URL aAccessService,
            final String aScaleConstraint) {
        myCookieService = aCookieService;
        myTokenService = aTokenService;
        myAccessService = aAccessService;

        setScaleConstraint(aScaleConstraint);
    }

    /**
     * Gets the configured cookie service URL.
     *
     * @return The configured cookie service URL
     */
    public String getCookieService() {
        return myCookieService.toString();
    }

    /**
     * Sets the cookie service URL.
     *
     * @param aCookieService A cookie service
     * @return This configuration
     */
    public Config setCookieService(final URL aCookieService) {
        myCookieService = aCookieService;
        return this;
    }

    /**
     * Gets the configured token service URL.
     *
     * @return The configured token service URL
     */
    public String getTokenService() {
        return myTokenService.toString();
    }

    /**
     * Sets the token service URL.
     *
     * @param aTokenService A token service
     * @return This configuration
     */
    public Config setTokenService(final URL aTokenService) {
        myTokenService = aTokenService;
        return this;
    }

    /**
     * Gets the configured access service URL.
     *
     * @return The configured access service URL
     */
    public String getAccessService() {
        return myAccessService.toString();
    }

    /**
     * Sets an access service URL.
     *
     * @param aAccessService An access service
     * @return This configuration
     */
    public Config setAccessService(final URL aAccessService) {
        myAccessService = aAccessService;
        return this;
    }

    /**
     * Gets the configured scale constraint for tiered access.
     *
     * @return The configured scale constraint for tiered access
     */
    @SuppressWarnings({ "PMD.MethodReturnsInternalArray" })
    public int[] getScaleConstraint() {
        return myScaleConstraint;
    }

    /**
     * Sets a scale constraint for tiered access.
     *
     * @param aScaleConstraint A scale constraint for tiered access
     * @return This configuration
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    public Config setScaleConstraint(final String aScaleConstraint) {
        try {
            myScaleConstraint = Stream.of(aScaleConstraint.split(COLON)).mapToInt(Integer::parseInt).toArray();
        } catch (final NumberFormatException details) {
            throw new ConfigException(aScaleConstraint);
        }

        // Must be able to be mapped to an array of length 2, and numerator must be less than the denominator
        if (myScaleConstraint.length != 2 || myScaleConstraint[0] >= myScaleConstraint[1]) {
            throw new ConfigException(aScaleConstraint);
        }

        return this;
    }

    /**
     * Gets an environmental property and checks that it exists.
     *
     * @param aEnvPropertyName An environmental property name
     * @return The property value
     * @throws ConfigException If the supplied property name doesn't exist in the environment
     */
    static String getProperty(final String aEnvPropertyName) {
        final Map<String, String> envMap = System.getenv();

        // Does our ENV property exist?
        if (!envMap.containsKey(aEnvPropertyName)) {
            throw new ConfigException(aEnvPropertyName);
        } else {
            return envMap.get(aEnvPropertyName);
        }
    }
}
