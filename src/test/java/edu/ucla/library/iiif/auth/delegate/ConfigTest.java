
package edu.ucla.library.iiif.auth.delegate;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import info.freelibrary.util.I18nRuntimeException;

/**
 * Tests of the Config class.
 */
public class ConfigTest {

    /**
     * A configuration.
     */
    private Config myConfig;

    /**
     * Sets up the testing environment.
     */
    @Before
    public final void setUp() {
        myConfig = new Config(getFakeServiceURL(), getFakeServiceURL(), getFakeServiceURL());
    }

    /**
     * Tests the default Config constructor.
     */
    @Test
    public final void testConfig() {
        final URL cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        final URL tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        final URL accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        final Config config = new Config();

        assertEquals(accessService.toString(), config.getAccessService());
        assertEquals(tokenService.toString(), config.getTokenService());
        assertEquals(cookieService.toString(), config.getCookieService());
    }

    /**
     * Tests the parameterized Config constructor.
     *
     * @throws ConfigException If the configuration wasn't able to be successfully constructed.
     */
    @Test
    public final void testConfigURLURLURL() throws ConfigException {
        final URL cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        final URL tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        final URL accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        final Config config = new Config(cookieService, tokenService, accessService);

        assertEquals(accessService.toString(), config.getAccessService());
        assertEquals(tokenService.toString(), config.getTokenService());
        assertEquals(cookieService.toString(), config.getCookieService());
    }

    /**
     * Tests getting/setting the cookie service configuration.
     */
    @Test
    public final void testSetCookieService() {
        final URL cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        assertEquals(cookieService.toString(), myConfig.setCookieService(cookieService).getCookieService());
    }

    /**
     * Tests getting/setting the token service configuration.
     */
    @Test
    public final void testSetTokenService() {
        final URL tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        assertEquals(tokenService.toString(), myConfig.setTokenService(tokenService).getTokenService());
    }

    /**
     * Tests getting/setting the access service configuration.
     */
    @Test
    public final void testSetAccessService() {
        final URL accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        assertEquals(accessService.toString(), myConfig.setAccessService(accessService).getAccessService());
    }

    /**
     * Tests getting a configuration property from the package level <code>getProperty()</code> method.
     *
     * @throws MalformedURLException If the configured access service string isn't a valid URL
     * @throws NullPointerException If the access string property has not been set properly
     */
    @Test
    public final void testGetProperty() throws MalformedURLException {
        final URL expectedURL = new URL(System.getenv(Config.AUTH_ACCESS_SERVICE));
        assertEquals(expectedURL, Config.getProperty(Config.AUTH_ACCESS_SERVICE));
    }

    /**
     * Gets a service URL for no-op parts of our testing.
     *
     * @return A fake service URL
     */
    private URL getFakeServiceURL() {
        try {
            return new URL("https://example.com/service");
        } catch (final MalformedURLException details) {
            throw new I18nRuntimeException(details);
        }
    }
}
