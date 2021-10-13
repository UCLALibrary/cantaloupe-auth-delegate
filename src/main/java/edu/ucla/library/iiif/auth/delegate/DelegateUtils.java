
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.EQUALS;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities that Cantaloupe delegates can use.
 */
public final class DelegateUtils {

    /**
     * Creates a new utility class.
     */
    private DelegateUtils() {
        // This is intentionally left empty
    }

    /**
     * Returns a human-friendly string representation of a Map.
     *
     * @param aMap A map of objects identified by strings
     * @return A string representation of the supplied map
     */
    public static String toString(final Map<String, Object> aMap) {
        final Stream<String> keyStream = aMap.keySet().stream();
        return keyStream.map(key -> key + EQUALS + aMap.get(key)).collect(Collectors.joining(", ", "{", "}"));
    }

}
