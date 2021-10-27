
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
import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

/**
 * A test of CantaloupeAuthDelegate.
 */
public class CantaloupeAuthDelegateIT {

    /**
     * The test's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CantaloupeAuthDelegateIT.class, MessageCodes.BUNDLE);

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
     * The id of the restricted image.
     */
    private static final String RESTRICTED_IMAGE_DEGRADED_ID = "test-restricted.tif;1:2";

    /**
     * The id of the non-restricted image.
     */
    private static final String OPEN_IMAGE_ID = "test-open.tif";

    /**
     * The file paths of the info.json templates.
     */
    private static final Map<String, File> RESPONSE_TEMPLATES =
            Map.of(RESTRICTED_IMAGE_ID, new File("src/test/resources/services-info-restricted.json"),
                    RESTRICTED_IMAGE_DEGRADED_ID, new File("src/test/resources/services-info-restricted;1:2.json"),
                    OPEN_IMAGE_ID, new File("src/test/resources/services-info-open.json"));

    /**
     * An internal HTTP client.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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
        // Now do the redirect
        testResponse(RESTRICTED_IMAGE_DEGRADED_ID, null);
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
        // Now do the redirect
        testResponse(RESTRICTED_IMAGE_DEGRADED_ID, null);
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

        final List<String> responseTemplateURLs = new ArrayList<>();
        final File templateResponseFile = RESPONSE_TEMPLATES.get(aImageID);
        final String expected;

        final String imageURL = StringUtils.format(IMAGE_URL_TEMPLATE, iiifURL, "2", aImageID);
        final HttpRequest.Builder requestBuilder;
        final HttpResponse<String> response;

        responseTemplateURLs.add(imageURL);

        if (aImageID.startsWith(RESTRICTED_IMAGE_ID)) {
            // The Hauth service URLs need to be added to the info.json
            responseTemplateURLs.add(envProperties.get(Config.AUTH_COOKIE_SERVICE));
            responseTemplateURLs.add(envProperties.get(Config.AUTH_TOKEN_SERVICE));
        }

        expected = StringUtils.format(StringUtils.read(templateResponseFile), responseTemplateURLs.toArray());

        requestBuilder = HttpRequest.newBuilder(URI.create(imageURL + "/info.json"));

        if (aToken != null) {
            requestBuilder.header("Authorization", StringUtils.format("Bearer {}", aToken));
        }

        response = HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString());

        switch (response.statusCode()) {
            case HTTP.OK:
                TestUtils.assertEquals(expected, response.body());
                break;
            case HTTP.FOUND:
                final Optional<String> locationHeader = response.headers().firstValue("Location");

                Assert.assertTrue(locationHeader.isPresent() && locationHeader.get().contains(";1:2"));
                break;
            default:
                final String errorMessage = LOGGER.getMessage(MessageCodes.CAD_006, response.statusCode());

                Assert.fail(errorMessage);
                break;
        }
    }

}
