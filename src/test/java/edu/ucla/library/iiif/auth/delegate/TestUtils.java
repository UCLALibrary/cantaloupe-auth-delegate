
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.EMPTY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utilities to assist with testing.
 */
public final class TestUtils {

    /**
     * A mapper that does a rough, but consistent, sort of JSON objects and arrays.
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();

    /**
     * Creates a new test utilities class.
     */
    private TestUtils() {
        // This is intentionally left empty
    }

    /**
     * Assert that the two JSON strings are equal (regardless of order, including order of array values).
     *
     * @param aExpected A first JSON string
     * @param aFound A second JSON string
     */
    public static void assertEquals(final String aExpected, final String aFound)
            throws JsonProcessingException, IOException {
        Assert.assertEquals(MAPPER.readTree(aExpected), MAPPER.readTree(aFound));
    }

    /**
     * A sorting node factory.
     */
    private static class SortingNodeFactory extends JsonNodeFactory {

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
         * The <code>serialVersionUID</code>
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