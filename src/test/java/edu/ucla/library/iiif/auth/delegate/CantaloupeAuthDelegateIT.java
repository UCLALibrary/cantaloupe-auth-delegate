
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
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.delegate.hauth.HauthToken;

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
     * A token generated using the following shell command:
     * <p>
     * <code>$ base64 <<< '{"version": "0.0.0-SNAPSHOT", "campus-network": true}'
     * </code>
     */
    private static final String VALID_TOKEN =
            "eyJ2ZXJzaW9uIjogIjAuMC4wLVNOQVBTSE9UIiwiY2FtcHVzLW5ldHdvcmsiOiB0cnVlfQo=";

    /**
     * The id of the restricted image.
     */
    private static final String RESTRICTED_IMAGE_ID = "test-restricted.tif";

    /**
     * The id of the restricted image with a vitual scale constraint.
     */
    private static final String RESTRICTED_IMAGE_DEGRADED_ID =
            StringUtils.format("test-restricted.tif;{}", System.getenv().get(Config.DEGRADED_IMAGE_SCALE_CONSTRAINT));

    /**
     * The id of the restricted image with a vitual scale constraint that is not allowed.
     */
    private static final String RESTRICTED_IMAGE_DEGRADED_DISALLOWED_ID = "test-restricted.tif;3:4";

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
     * The key of the "Location" HTTP response header.
     */
    private static final String LOCATION = "Location";

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
     * Tests the HTTP response of a request for a non-restricted image using Image API 2.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseOpenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_IMAGE_ID, null);
        final String expectedResponse = getExpectedDescriptionResource(OPEN_IMAGE_ID);

        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests the HTTP response of a request with an Authorization header for a restricted image using Image API 2.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedWithTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(RESTRICTED_IMAGE_ID, VALID_TOKEN);
        final String expectedResponse = getExpectedDescriptionResource(RESTRICTED_IMAGE_ID);

        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests the HTTP response of a request without an Authorization header for a restricted image using Image API 2.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedNoTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> firstResponse;
        final HttpResponse<String> secondResponse;
        final Optional<String> firstResponseLocation;
        final String expectedSecondResponse;

        firstResponse = sendImageInfoRequest(RESTRICTED_IMAGE_ID, null);

        // Check first response
        firstResponseLocation = firstResponse.headers().firstValue(LOCATION);
        Assert.assertEquals(HTTP.FOUND, firstResponse.statusCode());
        Assert.assertTrue(firstResponseLocation.isPresent() &&
                firstResponseLocation.get().contains(RESTRICTED_IMAGE_DEGRADED_ID));

        // Now do the redirect
        secondResponse = sendImageInfoRequest(RESTRICTED_IMAGE_DEGRADED_ID, null);

        expectedSecondResponse = getExpectedDescriptionResource(RESTRICTED_IMAGE_DEGRADED_ID);
        TestUtils.assertEquals(expectedSecondResponse, secondResponse.body());
    }

    /**
     * Tests the HTTP response of a request without an Authorization header for a restricted image at a disallowed scale
     * using Image API 2.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedNoTokenDisallowedScaleV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(RESTRICTED_IMAGE_DEGRADED_DISALLOWED_ID, null);

        Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
    }

    /******
     * v3 *
     ******/

    /**
     * Tests the HTTP response of a request for a non-restricted image using Image API 3.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseOpenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_IMAGE_ID, null);
        final String expectedResponse = getExpectedDescriptionResource(OPEN_IMAGE_ID);

        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests the HTTP response of a request with an Authorization header for a restricted image using Image API 3.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedWithTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(RESTRICTED_IMAGE_ID, VALID_TOKEN);
        final String expectedResponse = getExpectedDescriptionResource(RESTRICTED_IMAGE_ID);

        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests the HTTP response of a request without an Authorization header for a restricted image using Image API 3.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedNoTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> firstResponse;
        final HttpResponse<String> secondResponse;
        final Optional<String> firstResponseLocation;
        final String expectedSecondResponse;

        firstResponse = sendImageInfoRequest(RESTRICTED_IMAGE_ID, null);

        // Check first response
        firstResponseLocation = firstResponse.headers().firstValue(LOCATION);
        Assert.assertEquals(HTTP.FOUND, firstResponse.statusCode());
        Assert.assertTrue(firstResponseLocation.isPresent() &&
                firstResponseLocation.get().contains(RESTRICTED_IMAGE_DEGRADED_ID));

        // Now do the redirect
        secondResponse = sendImageInfoRequest(RESTRICTED_IMAGE_DEGRADED_ID, null);

        expectedSecondResponse = getExpectedDescriptionResource(RESTRICTED_IMAGE_DEGRADED_ID);
        TestUtils.assertEquals(expectedSecondResponse, secondResponse.body());
    }

    /**
     * Tests the HTTP response of a request without an Authorization header for a restricted image at a disallowed scale
     * using Image API 3.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testResponseRestrictedNoTokenDisallowedScaleV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(RESTRICTED_IMAGE_DEGRADED_DISALLOWED_ID, null);

        Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
    }

    /******************
     * Helper methods *
     ******************/

    /**
     * Sends an HTTP request for the description resource containing image information (info.json).
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aToken A bearer token for authorization
     * @return The HTTP response
     * @throws IOException If there is trouble sending the HTTP request
     * @throws InterruptedException If there is trouble sending the HTTP request
     */
    private static HttpResponse<String> sendImageInfoRequest(final String aImageID, final String aToken)
            throws IOException, InterruptedException {
        final String imageURL =
                getDescriptionResourceID(System.getenv().get(TestConfig.IIIF_URL_PROPERTY), 2, aImageID);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(imageURL + "/info.json"));

        if (aToken != null) {
            requestBuilder.header(HauthToken.HEADER, StringUtils.format("{} {}", HauthToken.TYPE, aToken));
        }

        return HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString());
    }

    /**
     * Generates the expected info.json for a given image.
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @return The expected info.json response for the image
     * @throws IOException If there is trouble reading the test file
     */
    private static String getExpectedDescriptionResource(final String aImageID) throws IOException {
        final Map<String, String> envProperties = System.getenv();
        final String descriptionResourceID =
                getDescriptionResourceID(envProperties.get(TestConfig.IIIF_URL_PROPERTY), 2, aImageID);
        final File responseTemplate = RESPONSE_TEMPLATES.get(aImageID);
        final List<String> responseTemplateURLs = new ArrayList<>();

        responseTemplateURLs.add(descriptionResourceID);

        if (aImageID.startsWith(RESTRICTED_IMAGE_ID)) {
            // The Hauth service URLs need to be added to the info.json
            responseTemplateURLs.add(envProperties.get(Config.AUTH_COOKIE_SERVICE));
            responseTemplateURLs.add(envProperties.get(Config.AUTH_TOKEN_SERVICE));
        }

        return StringUtils.format(StringUtils.read(responseTemplate), responseTemplateURLs.toArray());
    }

    /**
     * Constructs the ID of a description resource.
     *
     * @param aBaseURL The base URL of the Cantaloupe server
     * @param aImageApiVersion The IIIF Image API version
     * @param aImageID The identifier of the image
     * @return The ID of the description resource
     */
    private static String getDescriptionResourceID(final String aBaseURL, final int aImageApiVersion,
            final String aImageID) {
        return StringUtils.format(IMAGE_URL_TEMPLATE, aBaseURL, aImageApiVersion, aImageID);
    }

}
