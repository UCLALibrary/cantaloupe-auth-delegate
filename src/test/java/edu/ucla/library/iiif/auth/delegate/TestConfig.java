
package edu.ucla.library.iiif.auth.delegate;

/**
 * Configuration options for the test suite.
 */
public final class TestConfig {

    /**
     * A IIIF server's base URL.
     */
    public static final String IIIF_URL_PROPERTY = "IIIF_IMAGE_URL";

    /**
     * A Hauth server's base URL.
     */
    public static final String HAUTH_URL_PROPERTY = "HAUTH_URL";

    /**
     * Creates a new test configuration class.
     */
    private TestConfig() {
        // This is intentionally left empty
    }

}
