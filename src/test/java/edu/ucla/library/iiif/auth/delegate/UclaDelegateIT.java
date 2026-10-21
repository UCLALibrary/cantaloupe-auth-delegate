
package edu.ucla.library.iiif.auth.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import info.freelibrary.util.Constants;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.StringUtils;

/**
 * Tests of UclaDelegate.
 */
public class UclaDelegateIT {

    /** A sample thumbnail IIIF request. */
    private static final String THUMBNAIL = "/full/!200,200/0/default.tif";

    /**
     * An environmental property for the scale constraint to use for tiered access.
     * <p>
     * See https://cantaloupe-project.github.io/manual/5.0/access-control.html#Tiered%20Access for more information.
     */
    public static final String TIERED_ACCESS_SCALE_CONSTRAINT = "TIERED_ACCESS_SCALE_CONSTRAINT";

    /**
     * The template for image URLs. The slots are:
     * <ul>
     * <li>Cantaloupe base URL</li>
     * <li>Image API version</li>
     * <li>image identifier</li>
     * </ul>
     */
    private static final String IMAGE_URL_TEMPLATE = "{}/iiif/{}/{}";

    /** The id of the non-restricted image. */
    private static final String OPEN_ACCESS_IMAGE = "test-open.tif";

    /** The id of the restricted image. */
    private static final String TIERED_ACCESS_IMAGE = "test-tiered.tif";

    /** The id of the restricted image at the allowed degraded access tier. */
    private static final String TIERED_ACCESS_IMAGE_DEGRADED_VALID =
            StringUtils.format("test-tiered.tif;{}", System.getenv().get(TIERED_ACCESS_SCALE_CONSTRAINT));

    /**
     * The info.json template (Image API 2) for images that the client has degraded access to (e.g., a client does not
     * have an access token that grants access to a tiered access image).
     */
    private static final File DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2 =
            new File("src/test/resources/json/test-degraded-access-v2-info.json");

    /** The info.json template (Image API 3) for images that the client has degraded access to. */
    private static final File DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3 =
            new File("src/test/resources/json/test-degraded-access-v3-info.json");

    /** An internal HTTP client. */
    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().followRedirects(Redirect.NORMAL).version(HttpClient.Version.HTTP_1_1).build();

    /** A IIIF server's base URL. */
    public static final String IIIF_URL_PROPERTY = "IIIF_IMAGE_URL";

    /** The application JSON media type. */
    public static final String APPLICATION_JSON = "application/json";

    /** The application JSON + linked data media type. */
    public static final String APPLICATION_LD_PLUS_JSON = "application/ld+json";

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
     * A mapper that does a rough, but consistent, sort of JSON objects and arrays.
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();

    /**
     * Tests that thumbnails of access controlled items are still displayed.
     *
     * @throws InterruptedException If the test is interrupted
     * @throws IOException If there is trouble reading and writing test resources
     */
    @Test
    public final void testTieredAccessControlledThumbnails() throws InterruptedException, IOException {
        final String imageURL = StringUtils.format(IMAGE_URL_TEMPLATE, System.getenv().get(IIIF_URL_PROPERTY), 2,
                TIERED_ACCESS_IMAGE_DEGRADED_VALID + THUMBNAIL);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(imageURL));
        final HttpResponse<byte[]> response = HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofByteArray());
        final ByteArrayInputStream byteArrayInputStream;
        final BufferedImage image;

        assertEquals(200, response.statusCode());

        byteArrayInputStream = new ByteArrayInputStream(response.body());
        image = ImageIO.read(byteArrayInputStream);

        assertEquals(200, image.getHeight());
        assertEquals(200, image.getWidth());
    }

    /**
     * Test open unauthorized v2 manifest.
     *
     * @throws InterruptedException If the test is interrupted
     * @throws IOException If there is trouble reading and writing test resources
     */
    @Test
    public final void testResponseOpenUnauthorizedV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, 2);
        final String expectedResponse = getExpectedImageInfo(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        assertEquals(HTTP.OK, response.statusCode());
        assertTrue(responseHasContentType(response, APPLICATION_JSON, APPLICATION_LD_PLUS_JSON));
        localAssertEquals(expectedResponse, response.body());
    }

    /**
     * Test tiered unauthorized v2 manifest.
     *
     * @throws InterruptedException If the test is interrupted
     * @throws IOException If there is trouble reading and writing test resources
     */
    @Test
    public final void testResponseTieredUnauthorizedV2() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, 2);
        final String expectedResponse =
                getExpectedImageInfo(TIERED_ACCESS_IMAGE, DEGRADED_ACCESS_RESPONSE_TEMPLATE_V2, 2);

        assertEquals(HTTP.OK, response.statusCode());
        assertTrue(responseHasContentType(response, APPLICATION_JSON, APPLICATION_LD_PLUS_JSON));
        localAssertEquals(expectedResponse, response.body());
    }

    /**
     * Test open unauthorized v3 manifest.
     *
     * @throws InterruptedException If the test is interrupted
     * @throws IOException If there is trouble reading and writing test resources
     */
    @Test
    public final void testResponseOpenUnauthorizedV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(OPEN_ACCESS_IMAGE, 3);
        final String expectedResponse = getExpectedImageInfo(OPEN_ACCESS_IMAGE, FULL_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        assertEquals(HTTP.OK, response.statusCode());
        assertTrue(responseHasContentType(response, APPLICATION_JSON, APPLICATION_LD_PLUS_JSON));
        localAssertEquals(expectedResponse, response.body());
    }

    /**
     * Test tiered unauthorized v3 manifest.
     *
     * @throws InterruptedException If the test is interrupted
     * @throws IOException If there is trouble reading and writing test resources
     */
    @Test
    public final void testResponseTieredUnauthorizedV3() throws IOException, InterruptedException {
        final HttpResponse<String> response = sendImageInfoRequest(TIERED_ACCESS_IMAGE, 3);
        final String expectedResponse =
                getExpectedImageInfo(TIERED_ACCESS_IMAGE, DEGRADED_ACCESS_RESPONSE_TEMPLATE_V3, 3);

        assertEquals(HTTP.OK, response.statusCode());
        assertTrue(responseHasContentType(response, APPLICATION_JSON, APPLICATION_LD_PLUS_JSON));
        localAssertEquals(expectedResponse, response.body());
    }

    /**
     * Assert that the two JSON strings are equal (regardless of order, including order of array values).
     *
     * @param aExpected A first JSON string
     * @param aFound A second JSON string
     * @throws JsonProcessingException If there is trouble processing the JSON tree
     * @throws IOException If there is trouble reading the JSON tree
     */
    private void localAssertEquals(final String aExpected, final String aFound)
            throws JsonProcessingException, IOException {
        assertEquals(MAPPER.readTree(aExpected), MAPPER.readTree(aFound));
    }

    /**
     * Sends an HTTP request for the description resource containing image information (info.json).
     *
     * @param aImageID The identifier of the image whose info we're requesting
     * @param aImageApiVersion The IIIF Image API endpoint to use
     * @return The HTTP response
     * @throws IOException If there is trouble sending the HTTP request
     * @throws InterruptedException If there is trouble sending the HTTP request
     */
    private HttpResponse<String> sendImageInfoRequest(final String aImageID, final int aImageApiVersion)
            throws IOException, InterruptedException {
        final String imageURL = getImageInfoURL(System.getenv().get(IIIF_URL_PROPERTY), aImageApiVersion, aImageID);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(imageURL + "/info.json"));

        return HTTP_CLIENT.send(requestBuilder.build(), BodyHandlers.ofString());
    }

    /**
     * Constructs the URL of an info.json.
     *
     * @param aBaseURL The base URL of the Cantaloupe server
     * @param aImageApiVersion The IIIF Image API version
     * @param aImageID The identifier of the image
     * @return The URL of the info.json
     */
    private String getImageInfoURL(final String aBaseURL, final int aImageApiVersion, final String aImageID) {
        return StringUtils.format(IMAGE_URL_TEMPLATE, aBaseURL, aImageApiVersion, aImageID);
    }

    /**
     * Checks whether the response has at least one of the given content types.
     *
     * @param aResponse The HTTP response to test
     * @param aMediaTypes The array of content types that the response is checked for
     * @return Whether or not at least one of the content types is included in the response's Content-Type header
     */
    private boolean responseHasContentType(final HttpResponse<?> aResponse, final String... aMediaTypes) {
        final Optional<String> contentType = aResponse.headers().firstValue("Content-Type");
        return contentType.isPresent() && Stream.of(aMediaTypes).anyMatch(value -> contentType.get().contains(value));
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
    private String getExpectedImageInfo(final String aImageID, final File aResponseTemplate, final int aImageApiVersion)
            throws IOException {
        final Map<String, String> envProperties = System.getenv();
        final String baseURL = envProperties.get(IIIF_URL_PROPERTY);
        final String descriptionResourceID = getImageInfoURL(baseURL, aImageApiVersion, aImageID);
        final List<String> responseTemplateURLs = new ArrayList<>();

        responseTemplateURLs.add(descriptionResourceID);

        return StringUtils.format(StringUtils.read(aResponseTemplate), responseTemplateURLs.toArray());
    }

    /**
     * A sorting node factory.
     */
    private static final class SortingNodeFactory extends JsonNodeFactory {

        /** The <code>serialVersionUID</code> for the sorting node factory. */
        private static final long serialVersionUID = -8215749011595412795L;

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<String, JsonNode>());
        }

        @Override
        public ArrayNode arrayNode() {
            return new SortedArrayNode(this);
        }
    }

    /**
     * A sorted array node. This is just for comparison purposes. The array node values are not modified.
     */
    private static class SortedArrayNode extends ArrayNode {

        /** The <code>serialVersionUID</code> of this node. */
        private static final long serialVersionUID = 4000699652392697259L;

        /** An internal sorted child node list. */
        private final List<JsonNode> myList;

        /**
         * A sorted array node.
         *
         * @param aNodeFactory A node factory
         */
        SortedArrayNode(final JsonNodeFactory aNodeFactory) {
            super(aNodeFactory);
            myList = new ArrayList<>();
        }

        @Override
        public ArrayNode add(final JsonNode aNode) {
            _add(aNode); // Keep the underlying list the same size as our sortable one
            myList.add(aNode);

            // We need a predictable sort of the array's values
            Collections.sort(myList, new Comparator<JsonNode>() {

                @Override
                public int compare(final JsonNode a1stNode, final JsonNode a2ndNode) {
                    return getString(a1stNode).compareTo(getString(a2ndNode));
                }

                /**
                 * Gets a sortable string representation of a JsonNode.
                 *
                 * @param aNode A JSON node
                 * @return A sortable string representation of a JsonNode
                 */
                private String getString(final JsonNode aNode) {
                    // The asText() casts to text if value isn't textual
                    return aNode.isValueNode() ? aNode.asText() : Constants.EMPTY;
                }
            });

            // Reorder the ArrayNode's underlying list with each addition
            for (int index = 0; index < myList.size(); index++) {
                set(index, myList.get(index));
            }

            return this;
        }
    }
}
