
package edu.ucla.library.iiif.auth.delegate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import info.freelibrary.util.StringUtils;

/**
 * A test of CantaloupeAuthDelegate.
 */
public class CantaloupeAuthDelegateIT {

    /**
     * The template for image URLs. The slots are:
     * <ul>
     * <li>Cantaloupe base URL</li>
     * <li>Image API version</li>
     * <li>image identifier</li>
     * </ul>
     */
    private static final String IMAGE_URL_TEMPLATE = "{}/iiif/{}/{}";

    /**
     * The id of the restricted image.
     */
    private static final String RESTRICTED_IMAGE_ID = "test-restricted.tif";

    /**
     * The id of the non-restricted image.
     */
    private static final String OPEN_IMAGE_ID = "test-open.tif";

    /**
     * The file path of the info.json template for restricted images.
     */
    private static final File RESTRICTED_RESPONSE_TEMPLATE =
            new File("src/test/resources/services-info-restricted.json");

    /**
     * The file path of the info.json template for non-restricted images.
     */
    private static final File OPEN_RESPONSE_TEMPLATE = new File("src/test/resources/services-info-open.json");

    /**
     * An internal HTTP client.
     */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * Tests pre-authorizing a request.
     */
    @Ignore
    @Test
    public final void testPreAuthorize() {
        // TODO
    }

    /**
     * Tests authorizing a request.
     */
    @Ignore
    @Test
    public final void testAuthorize() {
        // TODO
    }

    /******
     * v2 *
     ******/

    /**
     * Tests the HTTP response of a request for a restricted image using Image API 2.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseRestrictedV2() throws IOException, InterruptedException {
        testResponse(RESTRICTED_IMAGE_ID, RESTRICTED_RESPONSE_TEMPLATE);
    }

    /**
     * Tests the HTTP response of a request for a non-restricted image using Image API 2.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseOpenV2() throws IOException, InterruptedException {
        testResponse(OPEN_IMAGE_ID, OPEN_RESPONSE_TEMPLATE);
    }

    /******
     * v3 *
     ******/

    /**
     * Tests the HTTP response of a request for a restricted image using Image API 3.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseRestrictedV3() throws IOException, InterruptedException {
        testResponse(RESTRICTED_IMAGE_ID, RESTRICTED_RESPONSE_TEMPLATE);
    }

    /**
     * Tests the HTTP response of a request for a non-restricted image using Image API 3.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseOpenV3() throws IOException, InterruptedException {
        testResponse(OPEN_IMAGE_ID, OPEN_RESPONSE_TEMPLATE);
    }

    /**
     * Tests the HTTP response of a request. We read the keys into a sorted map to be able to get a consistent
     * representation (regardless of JSON formatting).
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aExpected An file with the expected JSON response
     * @throws IOException If there is trouble reading the test file
     */
    private void testResponse(final String aImageID, final File aExpected)
            throws IOException, InterruptedException {
        final Map<String, String> envProperties = System.getenv();
        final String iiifURL = envProperties.get(TestConfig.IIIF_URL_PROPERTY);
        final String imageURL = StringUtils.format(IMAGE_URL_TEMPLATE, iiifURL, "2", aImageID);
        final URI requestURL = URI.create(imageURL + "/info.json");
        final HttpRequest request = HttpRequest.newBuilder(requestURL).build();
        final HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());
        final String[] urls;

        // If we have a restricted item, the Hauth services URLs also need to be added to the info.json
        if (aExpected == RESTRICTED_RESPONSE_TEMPLATE) {
            final String cookieServiceURL = envProperties.get(Config.AUTH_COOKIE_SERVICE);
            final String tokenServiceURL = envProperties.get(Config.AUTH_TOKEN_SERVICE);

            urls = new String[] { imageURL, cookieServiceURL, tokenServiceURL };
        } else {
            urls = new String[] { imageURL };
        }

        TestUtils.assertEquals(StringUtils.format(StringUtils.read(aExpected), urls), response.body());
    }

}
