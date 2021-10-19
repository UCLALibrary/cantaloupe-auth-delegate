
package edu.ucla.library.iiif.auth.delegate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

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
     * Creates a new configuration.
     */
    public Config() {
        myCookieService = getProperty(Config.AUTH_COOKIE_SERVICE);
        myTokenService = getProperty(Config.AUTH_TOKEN_SERVICE);
        myAccessService = getProperty(Config.AUTH_ACCESS_SERVICE);
    }

    /**
     * Creates a new configuration.
     *
     * @param aCookieService A cookie service URL
     * @param aTokenService A token service URL
     * @param aAccessService An access service URL
     */
    public Config(final URL aCookieService, final URL aTokenService, final URL aAccessService) {
        myCookieService = aCookieService;
        myTokenService = aTokenService;
        myAccessService = aAccessService;
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
     * Gets an environmental property and checks that its value is valid.
     *
     * @param aEnvPropertyName An environmental property name
     * @return A URL
     * @throws ConfigException If the supplied property value isn't a URL
     */
    static URL getProperty(final String aEnvPropertyName) {
        final Map<String, String> envMap = System.getenv();

        // Does our ENV property exist?
        if (!envMap.containsKey(aEnvPropertyName)) {
            throw new ConfigException(aEnvPropertyName);
        }

        // Is our ENV property a URL?
        try {
            return new URL(envMap.get(aEnvPropertyName));
        } catch (final MalformedURLException details) {
            throw new ConfigException(details, aEnvPropertyName);
        }
    }
}
