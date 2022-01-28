
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

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
     * A test initialization vector used to encrypt {@link #TEST_SINAI_AUTHENTICATED_3DAY}. This is just the value
     * "0123456789ABCDEF" (see Ruby script below) encoded in hexadecimal.
     */
    private static final String TEST_INITIALIZATION_VECTOR = "30313233343536373839414243444546";

    @SuppressWarnings("checkstyle:lineLengthChecker")
    /**
     * A test cookie generated using the following Ruby code, mocking the relevant part of the Sinai application:
     * <p>
     *
     * <pre>
     * #!/usr/bin/env ruby
     *
     * require "openssl"
     *
     * cipher = OpenSSL::Cipher::AES256.new :CBC
     * cipher.encrypt
     * cipher.key = "ThisPasswordIsReallyHardToGuess!"
     * cipher.iv = "0123456789ABCDEF"
     * puts (cipher.update("Authenticated #{Time.at(0).utc}") + cipher.final).unpack("H*")[0].upcase
     * </pre>
     *
     * @see <a href=
     *      "https://github.com/UCLALibrary/sinaimanuscripts/blob/44cbbd9bf508c32b742f1617205a679edf77603e/app/controllers/application_controller.rb#L98-L103">How
     *      the Sinai application encodes cookies</a>
     */
    private static final String TEST_SINAI_AUTHENTICATED_3DAY =
            "5AFF80488740353F8A11B99C7A493D871807521908500772B92E4F8FC919E305A607ADB714B22EF08D2C22FC08C8A6EC";

    /**
     * The Cookie header template for Sinai image requests.
     */
    private static final String SINAI_COOKIE_REQUEST_HEADER_TEMPLATE =
            "sinai_authenticated_3day={}; initialization_vector={}";

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
                getImageInfoURL(System.getenv().get(TestConfig.IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(imageURL + "/info.json"));

        if (aToken != null) {
            requestBuilder.header(HauthToken.HEADER, StringUtils.format("{} {}", HauthToken.TYPE, aToken));
        }

        return HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString());
    }

    /**
     * Sends an HTTP request for the image.
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aCookieHeader The Cookie HTTP header to send with the request
     * @param aImageApiVersion The IIIF Image API endpoint to use
     * @return The HTTP response
     * @throws IOException If there is trouble sending the HTTP request
     * @throws InterruptedException If there is trouble sending the HTTP request
     */
    private static HttpResponse<byte[]> sendImageRequest(final String aImageID, final String aCookieHeader,
            final int aImageApiVersion) throws IOException, InterruptedException {
        final String imageURL =
                getImageURL(System.getenv().get(TestConfig.IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(imageURL));

        if (aCookieHeader != null) {
            requestBuilder.header("Cookie", aCookieHeader);
        }

        return HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofByteArray());
    }

    /**
     * Determines the info.json that is expected in the response.
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aResponseTemplate The response template that we should use to render the response
     * @param aImageApiVersion The IIIF Image API endpoint to use
     * @return The info.json that is expected in the response
     * @throws IOException If there is trouble reading the info.json template file
     */
    private static String getExpectedImageInfo(final String aImageID, final File aResponseTemplate,
            final int aImageApiVersion) throws IOException {
        final Map<String, String> envProperties = System.getenv();
        final String descriptionResourceID =
                getImageInfoURL(envProperties.get(TestConfig.IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
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
     * Determines the image that is expected in the response.
     *
     * @param aImageID The identifier of the image that we're requesting
     * @return The image that is expected in the response
     * @throws IOException If there is trouble reading the image file
     */
    private static byte[] getExpectedImage(final String aImageID) throws IOException {
        final File imageFile = new File("src/test/resources/images/" + aImageID);

        return FileUtils.readFileToByteArray(imageFile);
    }

    /**
     * Constructs the URL of an info.json.
     *
     * @param aBaseURL The base URL of the Cantaloupe server
     * @param aImageApiVersion The IIIF Image API version
     * @param aImageID The identifier of the image
     * @return The URL of the info.json
     */
    private static String getImageInfoURL(final String aBaseURL, final int aImageApiVersion, final String aImageID) {
        return StringUtils.format(IMAGE_URL_TEMPLATE, aBaseURL, aImageApiVersion, aImageID);
    }

    /**
     * Constructs the URL of an image.
     *
     * @param aBaseURL The base URL of the Cantaloupe server
     * @param aImageApiVersion The IIIF Image API version
     * @param aImageID The identifier of the image
     * @return The URL of the image
     */
    private static String getImageURL(final String aBaseURL, final int aImageApiVersion, final String aImageID) {
        final String imageApiPathTemplate;
        final String imageApiPath;

        // Use TIFFs so that we can easily compare the response payload with the source image
        switch (aImageApiVersion) {
            case 2:
                imageApiPathTemplate = "{}/full/full/0/default.tif";
                break;
            case 3:
            default:
                imageApiPathTemplate = "{}/full/max/0/default.tif";
                break;
        }

        imageApiPath = StringUtils.format(imageApiPathTemplate, aImageID);

        return StringUtils.format(IMAGE_URL_TEMPLATE, aBaseURL, aImageApiVersion, imageApiPath);
    }

    /*********
     * Tests *
     *********/

    /**
     * An abstract base class for testing the responses to requests for both description resources (info.json) and
     * content resources (images).
     */
    private abstract static class AbstractRequestIT {

        /**
         * Tests that the HTTP response to a request for an open access item indicates full access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testFullAccessResponseOpenUnauthorized() throws IOException, InterruptedException;

        /**
         * Tests that the HTTP response to a properly authorized request for a tiered access item indicates full access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected response response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testFullAccessResponseTieredAuthorized() throws IOException, InterruptedException;

        /**
         * Tests that the HTTP response to an unauthorized request for a tiered access item indicates degraded access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected response response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testDegradedAccessResponseTieredUnauthorized() throws IOException, InterruptedException;

        /**
         * Tests that the HTTP response to a request for a tiered access item, at a disallowed scale, indicates no
         * access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected info.json
         *         response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testErrorResponseTieredDisallowedScale() throws IOException, InterruptedException;

        /**
         * Tests that the HTTP response to a properly authorized request for an all-or-nothing access item indicates
         * full access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected response response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testFullAccessResponseAllOrNothingAuthorized() throws IOException, InterruptedException;

        /**
         * Tests that the HTTP response to an unauthorized request for an all-or-nothing access item indicates no
         * access.
         *
         * @throws IOException If there is trouble sending the HTTP request(s) or getting the expected response response
         * @throws InterruptedException If there is trouble sending the HTTP request(s)
         */
        @Test
        public abstract void testNoAccessResponseAllOrNothingUnauthorized() throws IOException, InterruptedException;
    }

    /**
     * Tests for info.json requests via Image API 2.
     */
    public static class InformationRequestV2IT extends AbstractRequestIT {

        @Override
        public final void testFullAccessResponseOpenUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, null, 2);
            final String expectedResponse =
                    getExpectedImageInfo(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testFullAccessResponseTieredAuthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, ACCESS_TOKEN, 2);
            final String expectedResponse =
                    getExpectedImageInfo(TIERED_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testDegradedAccessResponseTieredUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, null, 2);
            final String expectedResponse =
                    getExpectedImageInfo(TIERED_ACCESS_IMAGE_DEGRADED_VALID, DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2, 2);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testErrorResponseTieredDisallowedScale() throws IOException, InterruptedException {
            final HttpResponse<String> response =
                    sendImageInfoRequest(TIERED_ACCESS_IMAGE_DEGRADED_UNAVAILABLE, null, 2);

            Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
        }

        @Override
        public final void testFullAccessResponseAllOrNothingAuthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response =
                    sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, SINAI_ACCESS_TOKEN, 2);
            final String expectedResponse =
                    getExpectedImageInfo(ALL_OR_NOTHING_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testNoAccessResponseAllOrNothingUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 2);
            final String expectedResponse =
                    getExpectedImageInfo(ALL_OR_NOTHING_ACCESS_IMAGE, NO_ACCESS_RESPONSE_TEMPLATE_V2, 2);

            Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }
    }

    /**
     * Tests for info.json requests via Image API 3.
     */
    public static class InformationRequestV3IT extends AbstractRequestIT {

        @Override
        public final void testFullAccessResponseOpenUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, null, 3);
            final String expectedResponse =
                    getExpectedImageInfo(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testFullAccessResponseTieredAuthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, ACCESS_TOKEN, 3);
            final String expectedResponse =
                    getExpectedImageInfo(TIERED_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testDegradedAccessResponseTieredUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, null, 3);
            final String expectedResponse =
                    getExpectedImageInfo(TIERED_ACCESS_IMAGE_DEGRADED_VALID, DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3, 3);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testErrorResponseTieredDisallowedScale() throws IOException, InterruptedException {
            final HttpResponse<String> response =
                    sendImageInfoRequest(TIERED_ACCESS_IMAGE_DEGRADED_UNAVAILABLE, null, 3);

            Assert.assertEquals(HTTP.FORBIDDEN, response.statusCode());
        }

        @Override
        public final void testFullAccessResponseAllOrNothingAuthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response =
                    sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, SINAI_ACCESS_TOKEN, 3);
            final String expectedResponse =
                    getExpectedImageInfo(ALL_OR_NOTHING_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }

        @Override
        public final void testNoAccessResponseAllOrNothingUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<String> response = sendImageInfoRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 3);
            final String expectedResponse =
                    getExpectedImageInfo(ALL_OR_NOTHING_ACCESS_IMAGE, NO_ACCESS_RESPONSE_TEMPLATE_V3, 3);

            Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
            TestUtils.assertEquals(expectedResponse, response.body());
        }
    }

    /**
     * Tests for image requests via Image API 2.
     */
    public static class ImageRequestV2IT extends AbstractRequestIT {

        @Override
        public void testFullAccessResponseOpenUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<byte[]> response = sendImageRequest(OPEN_ACCESS_IMAGE, null, 2);
            final byte[] expectedResponse = getExpectedImage(OPEN_ACCESS_IMAGE);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            Assert.assertTrue(Arrays.equals(expectedResponse, response.body()));
        }

        @Override
        @Test
        @Ignore
        public void testFullAccessResponseTieredAuthorized() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        @Test
        @Ignore
        public void testDegradedAccessResponseTieredUnauthorized() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        @Test
        @Ignore
        public void testErrorResponseTieredDisallowedScale() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        public void testFullAccessResponseAllOrNothingAuthorized() throws IOException, InterruptedException {
            final String cookieHeader = StringUtils.format(SINAI_COOKIE_REQUEST_HEADER_TEMPLATE,
                    TEST_SINAI_AUTHENTICATED_3DAY, TEST_INITIALIZATION_VECTOR);
            final HttpResponse<byte[]> response = sendImageRequest(ALL_OR_NOTHING_ACCESS_IMAGE, cookieHeader, 2);
            final byte[] expectedResponse = getExpectedImage(ALL_OR_NOTHING_ACCESS_IMAGE);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            Assert.assertTrue(Arrays.equals(expectedResponse, response.body()));
        }

        @Override
        public void testNoAccessResponseAllOrNothingUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<byte[]> response = sendImageRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 2);

            Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
        }
    }

    /**
     * Tests for image requests via Image API 3.
     */
    public static class ImageRequestV3IT extends AbstractRequestIT {

        @Override
        public void testFullAccessResponseOpenUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<byte[]> response = sendImageRequest(OPEN_ACCESS_IMAGE, null, 3);
            final byte[] expectedResponse = getExpectedImage(OPEN_ACCESS_IMAGE);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            Assert.assertTrue(Arrays.equals(expectedResponse, response.body()));
        }

        @Override
        @Test
        @Ignore
        public void testFullAccessResponseTieredAuthorized() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        @Test
        @Ignore
        public void testDegradedAccessResponseTieredUnauthorized() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        @Test
        @Ignore
        public void testErrorResponseTieredDisallowedScale() throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        public void testFullAccessResponseAllOrNothingAuthorized() throws IOException, InterruptedException {
            final String cookieHeader = StringUtils.format(SINAI_COOKIE_REQUEST_HEADER_TEMPLATE,
                    TEST_SINAI_AUTHENTICATED_3DAY, TEST_INITIALIZATION_VECTOR);
            final HttpResponse<byte[]> response = sendImageRequest(ALL_OR_NOTHING_ACCESS_IMAGE, cookieHeader, 3);
            final byte[] expectedResponse = getExpectedImage(ALL_OR_NOTHING_ACCESS_IMAGE);

            Assert.assertEquals(HTTP.OK, response.statusCode());
            Assert.assertTrue(Arrays.equals(expectedResponse, response.body()));
        }

        @Override
        public void testNoAccessResponseAllOrNothingUnauthorized() throws IOException, InterruptedException {
            final HttpResponse<byte[]> response = sendImageRequest(ALL_OR_NOTHING_ACCESS_IMAGE, null, 3);

            Assert.assertEquals(HTTP.UNAUTHORIZED, response.statusCode());
        }
    }
}
