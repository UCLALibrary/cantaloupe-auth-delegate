
package edu.ucla.library.iiif.auth.delegate;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

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
        final String fakeService = "https://example.com/service";
        final URI fakeServiceURI;

        try {
            fakeServiceURI = new URI(fakeService);
        } catch (final URISyntaxException details) {
            throw new ConfigException(details, fakeService);
        }

        myConfig = new Config(fakeServiceURI, fakeServiceURI, fakeServiceURI, "1:2");
    }

    /**
     * Tests the default Config constructor.
     */
    @Test
    public final void testConfig() {
        final URI cookieService = Config.getURI(Config.AUTH_COOKIE_SERVICE);
        final URI tokenService = Config.getURI(Config.AUTH_TOKEN_SERVICE);
        final URI accessService = Config.getURI(Config.AUTH_ACCESS_SERVICE);
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
    public final void testConfigURIURIURIString() {
        final URI cookieService = Config.getURI(Config.AUTH_COOKIE_SERVICE);
        final URI tokenService = Config.getURI(Config.AUTH_TOKEN_SERVICE);
        final URI accessService = Config.getURI(Config.AUTH_ACCESS_SERVICE);
        final String scaleConstraint = Config.getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT);
        final Config config = new Config(cookieService, tokenService, accessService, scaleConstraint);
        final int[] scaleConstraints = config.getScaleConstraint();

        assertEquals(accessService, config.getAccessService());
        assertEquals(tokenService, config.getTokenService());
        assertEquals(cookieService, config.getCookieService());
        assertEquals(2, scaleConstraints.length);
        assertEquals(1, scaleConstraints[0]);
        assertEquals(2, scaleConstraints[1]);
    }

    /**
     * Tests getting/setting the cookie service configuration.
     *
     * @throws URISyntaxException If the service URI is invalid
     */
    @Test
    public final void testSetCookieService() {
        final URI cookieService = Config.getURI(Config.AUTH_COOKIE_SERVICE);
        assertEquals(cookieService, myConfig.setCookieService(cookieService).getCookieService());
    }

    /**
     * Tests getting/setting the token service configuration.
     *
     * @throws URISyntaxException If the service URI is invalid
     */
    @Test
    public final void testSetTokenService() {
        final URI tokenService = Config.getURI(Config.AUTH_TOKEN_SERVICE);
        assertEquals(tokenService, myConfig.setTokenService(tokenService).getTokenService());
    }

    /**
     * Tests getting/setting the access service configuration.
     *
     * @throws URISyntaxException If the service URI is invalid
     */
    @Test
    public final void testSetAccessService() {
        final URI accessService = Config.getURI(Config.AUTH_ACCESS_SERVICE);
        assertEquals(accessService, myConfig.setAccessService(accessService).getAccessService());
    }

    /**
     * Tests getting/setting the scale constraint configuration.
     */
    @Test
    public final void testSetScaleConstraint() {
        final String scaleConstraint = Config.getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT);
        final int[] scaleConstraints = myConfig.setScaleConstraint(scaleConstraint).getScaleConstraint();

        assertEquals(2, scaleConstraints.length);
        assertEquals(1, scaleConstraints[0]);
        assertEquals(2, scaleConstraints[1]);
    }

    /**
     * Tests setting the scale constraint configuration with an invalid value.
     */
    @Test(expected = ConfigException.class)
    public final void testSetScaleConstraintInvalid() {
        myConfig.setScaleConstraint("4:3");
    }

    /**
     * Tests getting a configuration property from the package level <code>getString()</code> method.
     *
     * @throws NullPointerException If the access string property has not been set properly
     */
    @Test
    public final void testGetString() {
        final String expectedConstraint = System.getenv(Config.TIERED_ACCESS_SCALE_CONSTRAINT);
        assertEquals(expectedConstraint, Config.getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT));
    }

    /**
     * Tests getting a configuration property from the package level <code>getURI()</code> method.
     *
     * @throws URISyntaxException If the configured access service string isn't a valid URI
     * @throws NullPointerException If the access string property has not been set properly
     */
    @Test
    public final void testGetURI() {
        final String expectedURI = System.getenv(Config.AUTH_ACCESS_SERVICE);
        assertEquals(expectedURI, Config.getURI(Config.AUTH_ACCESS_SERVICE).toString());
    }

}
