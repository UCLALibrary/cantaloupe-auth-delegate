
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

        myConfig = new Config(fakeServiceURI, fakeServiceURI, fakeServiceURI, fakeServiceURI, "1:2");
    }

    /**
     * Tests the default Config constructor.
     */
    @Test
    public final void testConfig() {
        final URI cookieService = Config.getURI(Config.AUTH_COOKIE_SERVICE);
        final URI tokenService = Config.getURI(Config.AUTH_TOKEN_SERVICE);
        final URI sinaiTokenService = Config.getURI(Config.SINAI_AUTH_TOKEN_SERVICE);
        final URI accessService = Config.getURI(Config.AUTH_ACCESS_MODE_SERVICE);
        final Config config = new Config();

        assertEquals(cookieService, config.getCookieService());
        assertEquals(tokenService, config.getTokenService());
        assertEquals(sinaiTokenService, config.getSinaiTokenService());
        assertEquals(accessService, config.getAccessModeService());
        assertEquals(2, config.getScaleConstraint().length);
    }

    /**
     * Tests the parameterized Config constructor.
     *
     * @throws ConfigException If the configuration wasn't able to be successfully constructed.
     */
    @Test
    public final void testConfigUriUriUriUriString() {
        final URI cookieService = Config.getURI(Config.AUTH_COOKIE_SERVICE);
        final URI tokenService = Config.getURI(Config.AUTH_TOKEN_SERVICE);
        final URI sinaiTokenService = Config.getURI(Config.SINAI_AUTH_TOKEN_SERVICE);
        final URI accessService = Config.getURI(Config.AUTH_ACCESS_MODE_SERVICE);
        final String scaleConstraint = Config.getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT);
        final Config config =
                new Config(cookieService, tokenService, sinaiTokenService, accessService, scaleConstraint);
        final int[] scaleConstraints = config.getScaleConstraint();

        assertEquals(cookieService, config.getCookieService());
        assertEquals(tokenService, config.getTokenService());
        assertEquals(sinaiTokenService, config.getSinaiTokenService());
        assertEquals(accessService, config.getAccessModeService());
        assertEquals(2, scaleConstraints.length);
        assertEquals(1, scaleConstraints[0]);
        assertEquals(2, scaleConstraints[1]);
    }

    /**
     * Tests getting/setting the cookie service configuration for items with all-or-nothing access.
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
     * Tests getting/setting the Sinai token service configuration.
     *
     * @throws URISyntaxException If the service URI is invalid
     */
    @Test
    public final void testSetSinaiTokenService() {
        final URI sinaiTokenService = Config.getURI(Config.SINAI_AUTH_TOKEN_SERVICE);
        assertEquals(sinaiTokenService, myConfig.setSinaiTokenService(sinaiTokenService).getSinaiTokenService());
    }

    /**
     * Tests getting/setting the access mode service configuration.
     *
     * @throws URISyntaxException If the service URI is invalid
     */
    @Test
    public final void testSetAccessModeService() {
        final URI accessModeService = Config.getURI(Config.AUTH_ACCESS_MODE_SERVICE);
        assertEquals(accessModeService, myConfig.setAccessModeService(accessModeService).getAccessModeService());
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
     * @throws NullPointerException If the property has not been set properly
     */
    @Test
    public final void testGetString() {
        final String expectedConstraint = System.getenv(Config.TIERED_ACCESS_SCALE_CONSTRAINT);
        assertEquals(expectedConstraint, Config.getString(Config.TIERED_ACCESS_SCALE_CONSTRAINT));
    }

    /**
     * Tests getting a configuration property from the package level <code>getURI()</code> method.
     *
     * @throws URISyntaxException If the configured access mode service string isn't a valid URI
     * @throws NullPointerException If the access string property has not been set properly
     */
    @Test
    public final void testGetURI() {
        final String expectedURI = System.getenv(Config.AUTH_ACCESS_MODE_SERVICE);
        assertEquals(expectedURI, Config.getURI(Config.AUTH_ACCESS_MODE_SERVICE).toString());
    }

}
