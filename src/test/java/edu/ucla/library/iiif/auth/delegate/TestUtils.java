
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.EMPTY;
import static info.freelibrary.util.Constants.SPACE;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpHeaders;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v3.MediaType;

/**
 * Utilities to assist with testing.
 */
public final class TestUtils {

    /**
     * A mapper that does a rough, but consistent, sort of JSON objects and arrays.
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();

    /**
     * The test utilities logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class, MessageCodes.BUNDLE);

    /**
     * Creates a new test utilities class.
     */
    private TestUtils() {
        // This is intentionally left empty
    }

    /**
     * Gets a pair of Sinai cookies by mocking the cookie generation code of the Sinai application.
     *
     * @return A tuple of size 2 whose first element is a {@link CookieNames#SINAI_CIPHERTEXT} cookie and whose second
     *         is a {@link CookieNames#SINAI_IV} cookie
     * @throws IOException If there is an issue generating the cookies
     */
    @SuppressWarnings({ "checkstyle:LineLength" })
    public static String[] getMockSinaiCookieValues() throws IOException {
        // This code mirrors the front-end Ruby code that creates encrypted cookies; see below for the implementation:
        // https://github.com/UCLALibrary/sinaimanuscripts/blob/v2.15.7/app/controllers/application_controller.rb#L98-L103

        try {
            final LocalDate date = LocalDate.now().minusDays(new Random().nextInt(3 + 1));
            final String clearTextSuffix = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
            final String clearText = String.join(SPACE, "Authenticated", clearTextSuffix);
            final byte[] password = "ThisPasswordIsReallyHardToGuess!".getBytes();
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final SecretKey key = new SecretKeySpec(password, "AES");

            final String sinaiAuthenticated3Day;
            final String initializationVector;
            final byte[] cipherText;

            cipher.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());
            cipherText = cipher.doFinal(clearText.getBytes());
            sinaiAuthenticated3Day = Hex.encodeHexString(cipherText);
            initializationVector = Hex.encodeHexString(cipher.getIV());

            LOGGER.debug(MessageCodes.CAD_026, "sinai_authenticated_3day", sinaiAuthenticated3Day,
                    "initialization_vector", initializationVector, clearText);

            return new String[] { sinaiAuthenticated3Day, initializationVector };
        } catch (final NoSuchPaddingException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException |
                IllegalBlockSizeException details) {
            throw new IOException(details);
        }
    }

    /**
     * Checks whether the response has at least one of the given content types.
     *
     * @param aResponse The HTTP response to test
     * @param aMediaTypes The array of content types that the response is checked for
     * @return Whether or not at least one of the content types is included in the response's Content-Type header
     */
    public static boolean responseHasContentType(final HttpResponse<?> aResponse, final MediaType... aMediaTypes) {
        final Optional<String> contentType = aResponse.headers().firstValue(HttpHeaders.CONTENT_TYPE);
        return contentType.isPresent() &&
                Stream.of(aMediaTypes).map(String::valueOf).anyMatch(value -> contentType.get().contains(value));
    }

    /**
     * Assert that the two JSON strings are equal (regardless of order, including order of array values).
     *
     * @param aExpected A first JSON string
     * @param aFound A second JSON string
     * @throws JsonProcessingException If there is trouble processing the JSON tree
     * @throws IOException If there is trouble reading the JSON tree
     */
    public static void assertEquals(final String aExpected, final String aFound)
            throws JsonProcessingException, IOException {
        Assert.assertEquals(MAPPER.readTree(aExpected), MAPPER.readTree(aFound));
    }

    /**
     * A sorting node factory.
     */
    private static final class SortingNodeFactory extends JsonNodeFactory {

        /**
         * The <code>serialVersionUID</code> for the sorting node factory.
         */
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

        /**
         * The <code>serialVersionUID</code> of this node.
         */
        private static final long serialVersionUID = 4000699652392697259L;

        /**
         * An internal sorted child node list.
         */
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
                    return aNode.isValueNode() ? aNode.asText() : EMPTY;
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
