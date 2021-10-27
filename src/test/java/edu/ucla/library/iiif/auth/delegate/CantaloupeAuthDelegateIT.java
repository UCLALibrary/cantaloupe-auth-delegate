
package edu.ucla.library.iiif.auth.delegate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
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
     * Tests the HTTP response of a request without an Authorization header for a restricted image using Image API 2.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseRestrictedNoTokenV2() throws IOException, InterruptedException {
        testResponse(RESTRICTED_IMAGE_ID, null);
    }

    /**
     * Tests the HTTP response of a request for a non-restricted image using Image API 2.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseOpenV2() throws IOException, InterruptedException {
        testResponse(OPEN_IMAGE_ID, null);
    }

    /******
     * v3 *
     ******/

    /**
     * Tests the HTTP response of a request without an Authorization header for a restricted image using Image API 3.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseRestrictedNoTokenV3() throws IOException, InterruptedException {
        testResponse(RESTRICTED_IMAGE_ID, null);
    }

    /**
     * Tests the HTTP response of a request for a non-restricted image using Image API 3.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testResponseOpenV3() throws IOException, InterruptedException {
        testResponse(OPEN_IMAGE_ID, null);
    }

    /**
     * Tests the HTTP response of a request.
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aToken An access token for authentication, to be added to the Cantaloupe request in an Authorization
     *        header
     * @throws IOException If there is trouble reading the test file
     */
    private void testResponse(final String aImageID, final String aToken)
            throws IOException, InterruptedException {
        final Map<String, String> envProperties = System.getenv();
        final String iiifURL = envProperties.get(TestConfig.IIIF_URL_PROPERTY);

        final List<String> authServiceURLs;
        final List<String> responseTemplateURLs;
        final File templateResponseFile;
        final String expected;

        final String imageID;
        final String imageURL;
        final HttpRequest.Builder requestBuilder;
        final HttpResponse<String> response;

        if (aImageID == RESTRICTED_IMAGE_ID) {
            // The Hauth service URLs need to be added to the info.json
            authServiceURLs = List.of(envProperties.get(Config.AUTH_COOKIE_SERVICE),
                    envProperties.get(Config.AUTH_TOKEN_SERVICE));

            // We expect a scaled image in the response
            imageID = StringUtils.format("{};{}", aImageID, "1:2");
            templateResponseFile = RESTRICTED_RESPONSE_TEMPLATE;
        } else {
            // No Hauth service URLs are expected in the info.json
            authServiceURLs = List.of();

            // We expect the full image to be available
            imageID = aImageID;
            templateResponseFile = OPEN_RESPONSE_TEMPLATE;
        }

        imageURL = StringUtils.format(IMAGE_URL_TEMPLATE, iiifURL, "2", imageID);

        responseTemplateURLs = new ArrayList<>();
        responseTemplateURLs.add(imageURL);
        responseTemplateURLs.addAll(authServiceURLs);

        expected = StringUtils.format(StringUtils.read(templateResponseFile), responseTemplateURLs.toArray());

        requestBuilder = HttpRequest.newBuilder(URI.create(imageURL + "/info.json"));

        if (aToken != null) {
            requestBuilder.header("Authorization", StringUtils.format("Bearer {}", aToken));
        }

        response = HTTP.send(requestBuilder.build(), BodyHandlers.ofString());

        TestUtils.assertEquals(expected, response.body());
    }

}
