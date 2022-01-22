
package edu.ucla.library.iiif.auth.delegate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * An access token generated using the following shell command:
     * <p>
     * <code> $ base64 <<< '{"version": "0.0.0-SNAPSHOT", "campusNetwork": true}'
     * </code>
     * <p>
     * This would be the value of the "accessToken" key shown
     * <a href="https://iiif.io/api/auth/1.0/#the-json-access-token-response">here</a>.
     */
    private static final String ACCESS_TOKEN =
            "eyJ2ZXJzaW9uIjogIjAuMC4wLVNOQVBTSE9UIiwgImNhbXB1c05ldHdvcmsiOiB0cnVlfQo=";

    /**
     * An access token generated using the following shell command:
     * <p>
     * <code> $ base64 <<< '{"version": "0.0.0-SNAPSHOT", "sinaiAffiliate": true}'
     * </code>
     * <p>
     * This would be the value of the "accessToken" key shown
     * <a href="https://iiif.io/api/auth/1.0/#the-json-access-token-response">here</a>.
     */
    private static final String SINAI_ACCESS_TOKEN =
            "eyJ2ZXJzaW9uIjogIjAuMC4wLVNOQVBTSE9UIiwgInNpbmFpQWZmaWxpYXRlIjogdHJ1ZX0K";

    /**
     * The id of the non-restricted image.
     */
    private static final String OPEN_ACCESS_IMAGE = "test-open.tif";

    /**
     * The id of the restricted image.
     */
    private static final String TIERED_ACCESS_IMAGE = "test-tiered.tif";

    /**
     * The id of the restricted image at the allowed degraded access tier.
     */
    private static final String TIERED_ACCESS_IMAGE_DEGRADED_VALID =
            StringUtils.format("test-tiered.tif;{}", System.getenv().get(Config.TIERED_ACCESS_SCALE_CONSTRAINT));

    /**
     * The id of the restricted image at a degraded access tier that is not allowed.
     */
    private static final String TIERED_ACCESS_IMAGE_DEGRADED_UNAVAILABLE = "test-tiered.tif;3:4";

    /**
     * The id of the image with all-or-nothing access.
     */
    private static final String ALL_OR_NOTHING_ACCESS_IMAGE = "test-all-or-nothing.tif";

    /**
     * The info.json template (Image API 2) for images that the client has full access to (e.g., either the image is
     * open access, or the client has an access token that grants access to either a tiered access image or an
     * all-or-nothing access image).
     */
    private static final File FULL_ACCESS_RESPONSE_TEMPLATE_V2 =
            new File("src/test/resources/json/test-full-access-v2-info.json");

    /**
     * The info.json template (Image API 3) for images that the client has full access to.
     */
    private static final File FULL_ACCESS_RESPONSE_TEMPLATE_V3 =
            new File("src/test/resources/json/test-full-access-v3-info.json");

    /**
     * The info.json template (Image API 2) for images that the client has degraded access to (e.g., a client does not
     * have an access token that grants access to a tiered access image).
     */
    private static final File DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2 =
            new File("src/test/resources/json/test-degraded-access-v2-info.json");

    /**
     * The info.json template (Image API 3) for images that the client has degraded access to.
     */
    private static final File DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3 =
            new File("src/test/resources/json/test-degraded-access-v3-info.json");

    /**
     * The info.json template (Image API 2) for images that the client has no access to (e.g., a client does not have an
     * access token that grants access to an all-or-nothing access image).
     */
    private static final File NO_ACCESS_RESPONSE_TEMPLATE_V2 =
            new File("src/test/resources/json/test-no-access-v2-info.json");

    /**
     * The info.json template (Image API 3) for images that the client has no access to.
     */
    private static final File NO_ACCESS_RESPONSE_TEMPLATE_V3 =
            new File("src/test/resources/json/test-no-access-v3-info.json");

    /**
     * An internal HTTP client.
     */
    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().followRedirects(Redirect.NORMAL).version(HttpClient.Version.HTTP_1_1).build();

    /******
     * v2 *
     ******/

    /**
     * Tests that the HTTP response to a request for an open access info.json, via Image API 2, indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullAccessResponseOpenNoTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, null, 2);
        final String expectedResponse =
                getExpectedDescriptionResource(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to a properly authorized request for a tiered access info.json, via Image API 2,
     * indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullResponseTieredWithTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, ACCESS_TOKEN, 2);
        final String expectedResponse =
                getExpectedDescriptionResource(TIERED_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to an unauthorized request for a tiered access info.json, via Image API 2, indicates
     * degraded access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testDegradedAccessResponseTieredNoTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, null, 2);
        final String expectedResponse = getExpectedDescriptionResource(TIERED_ACCESS_IMAGE_DEGRADED_VALID,
                DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to a request for a tiered access info.json, via Image API 2 and at a disallowed
     * scale, indicates no access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testErrorResponseTieredDisallowedScaleV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE_DEGRADED_UNAVAILABLE, null, 2);

        Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
    }

    /**
     * Tests that the HTTP response to a properly authorized request for an all-or-nothing access info.json, via Image
     * API 2, indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullAccessResponseAllOrNothingWithTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, SINAI_ACCESS_TOKEN, 2);
        final String expectedResponse =
                getExpectedDescriptionResource(ALL_OR_NOTHING_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to an unauthorized request for an all-or-nothing access info.json, via Image API 2,
     * indicates no access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    @Ignore
    public final void testNoAccessResponseAllOrNothingNoTokenV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 2);
        final String expectedResponse =
                getExpectedDescriptionResource(ALL_OR_NOTHING_ACCESS_IMAGE, NO_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /******
     * v3 *
     ******/

    /**
     * Tests that the HTTP response to a request for an open access info.json, via Image API 3, indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullAccessResponseOpenNoTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, null, 3);
        final String expectedResponse =
                getExpectedDescriptionResource(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to a properly authorized request for a tiered access info.json, via Image API 3,
     * indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullResponseTieredWithTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, ACCESS_TOKEN, 3);
        final String expectedResponse =
                getExpectedDescriptionResource(TIERED_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to an unauthorized request for a tiered access info.json, via Image API 3, indicates
     * degraded access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testDegradedAccessResponseTieredNoTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, null, 3);
        final String expectedResponse = getExpectedDescriptionResource(TIERED_ACCESS_IMAGE_DEGRADED_VALID,
                DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to a request for a tiered access info.json, via Image API 3 and at a disallowed
     * scale, indicates no access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testErrorResponseTieredDisallowedScaleV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE_DEGRADED_UNAVAILABLE, null, 3);

        Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
    }

    /**
     * Tests that the HTTP response to a properly authorized request for an all-or-nothing access info.json, via Image
     * API 3, indicates full access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    public final void testFullAccessResponseAllOrNothingWithTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, SINAI_ACCESS_TOKEN, 3);
        final String expectedResponse =
                getExpectedDescriptionResource(ALL_OR_NOTHING_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        Assert.assertEquals(HTTP.OK, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /**
     * Tests that the HTTP response to an unauthorized request for an all-or-nothing access info.json, via Image API 3,
     * indicates no access.
     *
     * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json response
     * @throws InterruptedException If there is trouble sending the HTTP request(s)
     */
    @Test
    @Ignore
    public final void testNoAccessResponseAllOrNothingNoTokenV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 3);
        final String expectedResponse =
                getExpectedDescriptionResource(ALL_OR_NOTHING_ACCESS_IMAGE, NO_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
        TestUtils.assertEquals(expectedResponse, response.body());
    }

    /******************
     * Helper methods *
     ******************/

    /**
     * Sends an HTTP request for the description resource containing image information (info.json).
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aToken A bearer token for authorization
     * @param aImageApiVersion The IIIF Image API endpoint to use
     * @return The HTTP response
     * @throws IOException If there is trouble sending the HTTP request
     * @throws InterruptedException If there is trouble sending the HTTP request
     */
    private static HttpResponse<String> sendImageInfoRequest(final String aImageID, final String aToken,
            final int aImageApiVersion) throws IOException, InterruptedException {
        final String imageURL =
                getDescriptionResourceID(System.getenv().get(TestConfig.IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
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
     * @param aResponseTemplate The response template that we should use to render the response
     * @param aImageApiVersion The IIIF Image API endpoint to use
     * @return The expected info.json response for the image
     * @throws IOException If there is trouble reading the test file
     */
    private static String getExpectedDescriptionResource(final String aImageID, final File aResponseTemplate,
            final int aImageApiVersion)
                    throws IOException {
        final Map<String, String> envProperties = System.getenv();
        final String descriptionResourceID =
                getDescriptionResourceID(envProperties.get(TestConfig.IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
        final List<String> responseTemplateURLs = new ArrayList<>();

        responseTemplateURLs.add(descriptionResourceID);

        if (aResponseTemplate.equals(DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2) ||
                aResponseTemplate.equals(DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3)) {
            // The Hauth service URLs need to be added to the info.json
            responseTemplateURLs.add(envProperties.get(Config.AUTH_COOKIE_SERVICE));
            responseTemplateURLs.add(envProperties.get(Config.AUTH_TOKEN_SERVICE));
        } else if (aResponseTemplate.equals(NO_ACCESS_RESPONSE_TEMPLATE_V2) ||
                aResponseTemplate.equals(NO_ACCESS_RESPONSE_TEMPLATE_V3)) {
            responseTemplateURLs.add(envProperties.get(Config.SINAI_AUTH_TOKEN_SERVICE));
        }

        return StringUtils.format(StringUtils.read(aResponseTemplate), responseTemplateURLs.toArray());
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
