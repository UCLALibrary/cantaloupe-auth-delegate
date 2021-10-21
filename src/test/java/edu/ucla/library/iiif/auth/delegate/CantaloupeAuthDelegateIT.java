
package edu.ucla.library.iiif.auth.delegate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import org.junit.Test;

import info.freelibrary.util.StringUtils;

/**
 * A test of CantaloupeAuthDelegate.
 */
public class CantaloupeAuthDelegateIT {

    /**
     * The URL path for the restricted test info.json file.
     */
    private static final String RESTRICTED_TEST_INFO_FILE = "/2/test-1.tif/info.json";

    /**
     * The URL path for the open test info.json file.
     */
    private static final String OPEN_TEST_INFO_FILE = "/2/test-2.tif/info.json";

    /**
     * The file path for the restricted services info.json file.
     */
    private static final File RESTRICTED_TEST_FILE_PATH = new File("src/test/resources/services-info-restricted.json");

    /**
     * The file path for the open services info.json file.
     */
    private static final File OPEN_TEST_FILE_PATH = new File("src/test/resources/services-info-open.json");

    /**
     * An internal HTTP client.
     */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * Tests pre-authorizing a request.
     */
    @Test
    public final void testPreAuthorize() {

    }

    /**
     * Tests authorizing a request.
     */
    @Test
    public final void testAuthorize() {

    }

    /**
     * Tests getting extra restricted IIIF (v2) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF2InformationResponseKeysRestricted() throws IOException, InterruptedException {
        testGetExtraIIIFInformationResponseKeys(RESTRICTED_TEST_INFO_FILE, RESTRICTED_TEST_FILE_PATH);
    }

    /**
     * Tests getting extra restricted IIIF (v3) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF3InformationResponseKeysRestricted() throws IOException, InterruptedException {
        testGetExtraIIIFInformationResponseKeys(RESTRICTED_TEST_INFO_FILE, RESTRICTED_TEST_FILE_PATH);
    }

    /**
     * Tests getting extra non-restricted IIIF (v2) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF2InformationResponseKeysOpen() throws IOException, InterruptedException {
        testGetExtraIIIFInformationResponseKeys(OPEN_TEST_INFO_FILE, OPEN_TEST_FILE_PATH);
    }

    /**
     * Tests getting extra non-restricted IIIF (v3) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF3InformationResponseKeysOpen() throws IOException, InterruptedException {
        testGetExtraIIIFInformationResponseKeys(OPEN_TEST_INFO_FILE, OPEN_TEST_FILE_PATH);
    }

    /**
     * Tests getting extra IIIF information response keys for v2 or v3. We read the keys into a sorted map to be able to
     * get a consistent representation (regardless of JSON formatting).
     *
     * @param aFound A string with the found JSON response
     * @param aExpected An file with the expected JSON response
     * @throws IOException If there is trouble reading the test file
     */
    private void testGetExtraIIIFInformationResponseKeys(final String aFound, final File aExpected)
            throws IOException, InterruptedException {
        final Map<String, String> envProperties = System.getenv();
        final String hauthURL = envProperties.get(TestConfig.HAUTH_URL_PROPERTY);
        final String iiifURL = envProperties.get(TestConfig.IIIF_URL_PROPERTY);
        final HttpRequest request = HttpRequest.newBuilder(URI.create(iiifURL + aFound)).build();
        final HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());
        final String[] urls;

        // If we have a restricted item, the Hauth services URLs also need to be added to the info.json
        if (aExpected == RESTRICTED_TEST_FILE_PATH) {
            urls = new String[] { iiifURL, hauthURL, hauthURL };
        } else {
            urls = new String[] { iiifURL };
        }

        TestUtils.assertEquals(StringUtils.format(StringUtils.read(aExpected), urls), response.body());
    }

}