package edu.ucla.library.iiif.auth.delegate;

import java.net.URL;

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
     * A configured cookie service.
     */
    private URL myCookieService;

    /**
     * A configured token service.
     */
    private URL myTokenService;

    /**
     * Creates a new configuration.
     */
    public Config() {
        // This is intentionally left empty.
    }

    /**
     * Creates a new configuration.
     *
     * @param aCookieService A cookie service URL
     * @param aTokenService A token service URL
     */
    public Config(final URL aCookieService, final URL aTokenService) {
        myCookieService = aCookieService;
        myTokenService = aTokenService;
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
}
