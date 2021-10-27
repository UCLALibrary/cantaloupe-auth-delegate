
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
        myConfig = new Config(getFakeServiceURL(), getFakeServiceURL(), getFakeServiceURL(), getFakeScaleConstraint());
    }

    /**
     * Tests the default Config constructor.
     */
    @Test
    public final void testConfig() {
        final String cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        final String tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        final String accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        final Config config = new Config();

        assertEquals(accessService, config.getAccessService());
        assertEquals(tokenService, config.getTokenService());
        assertEquals(cookieService, config.getCookieService());
        assertEquals(2, config.getScaleConstraint().length);
    }

    /**
     * Tests the parameterized Config constructor.
     *
     * @throws ConfigException If the configuration wasn't able to be successfully constructed.
     */
    @Test
    public final void testConfigURLURLURLString() throws ConfigException, MalformedURLException {
        final String cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        final String tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        final String accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        final String scaleConstraint = Config.getProperty(Config.DEGRADED_IMAGE_SCALE_CONSTRAINT);
        final Config config =
                new Config(new URL(cookieService), new URL(tokenService), new URL(accessService), scaleConstraint);

        assertEquals(accessService, config.getAccessService());
        assertEquals(tokenService, config.getTokenService());
        assertEquals(cookieService, config.getCookieService());
        assertEquals(2, config.getScaleConstraint().length);
    }

    /**
     * Tests getting/setting the cookie service configuration.
     *
     * @throws MalformedURLException If the service URL is malformed
     */
    @Test
    public final void testSetCookieService() throws MalformedURLException {
        final String cookieService = Config.getProperty(Config.AUTH_COOKIE_SERVICE);
        assertEquals(cookieService, myConfig.setCookieService(new URL(cookieService)).getCookieService());
    }

    /**
     * Tests getting/setting the token service configuration.
     *
     * @throws MalformedURLException If the service URL is malformed
     */
    @Test
    public final void testSetTokenService() throws MalformedURLException {
        final String tokenService = Config.getProperty(Config.AUTH_TOKEN_SERVICE);
        assertEquals(tokenService, myConfig.setTokenService(new URL(tokenService)).getTokenService());
    }

    /**
     * Tests getting/setting the access service configuration.
     *
     * @throws MalformedURLException If the service URL is malformed
     */
    @Test
    public final void testSetAccessService() throws MalformedURLException {
        final String accessService = Config.getProperty(Config.AUTH_ACCESS_SERVICE);
        assertEquals(accessService, myConfig.setAccessService(new URL(accessService)).getAccessService());
    }

    /**
     * Tests getting/setting the scale constraint configuration.
     */
    @Test
    public final void testSetScaleConstraint() {
        final String scaleConstraint = Config.getProperty(Config.DEGRADED_IMAGE_SCALE_CONSTRAINT);
        assertEquals(2, myConfig.setScaleConstraint(scaleConstraint).getScaleConstraint().length);
    }

    /**
     * Tests setting the scale constraint configuration with an invalid value.
     */
    @Test(expected = ConfigException.class)
    public final void testSetScaleConstraintInvalid() {
        myConfig.setScaleConstraint("4:3");
    }

    /**
     * Tests getting a configuration property from the package level <code>getProperty()</code> method.
     *
     * @throws MalformedURLException If the configured access service string isn't a valid URL
     * @throws NullPointerException If the access string property has not been set properly
     */
    @Test
    public final void testGetProperty() throws MalformedURLException {
        final String expectedURL = System.getenv(Config.AUTH_ACCESS_SERVICE);
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

    /**
     * Gets a scale constraint for no-op parts of our testing.
     *
     * @return A fake scale constraint
     */
    private String getFakeScaleConstraint() {
        return "1:2";
    }
}
