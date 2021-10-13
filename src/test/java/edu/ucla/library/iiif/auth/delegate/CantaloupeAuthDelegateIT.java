
package edu.ucla.library.iiif.auth.delegate;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.freelibrary.util.StringUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

/**
 * A test of CantaloupeAuthDelegate.
 */
public class CantaloupeAuthDelegateIT {

    /**
     * The URL path for the test info.json file.
     */
    private static final String TEST_INFO_FILE = "/2/test.tif/info.json";

    /**
     * The file path for the services-info.json file.
     */
    private static final File TEST_FILE_PATH = new File("src/test/resources/services-info.json");

    /**
     * The test's HTTP client.
     */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    /**
     * Tests pre-authorizing a request.
     */
    @Test
    public final void testPreAuthorize() {

    }

    /**
     * Tests authorizing a request.
     */
    @Test
    public final void testAuthorize() {

    }

    /**
     * Tests getting extra IIIF (v2) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF2InformationResponseKeys() throws IOException {
        testGetExtraIIIFInformationResponseKeys();
    }

    /**
     * Tests getting extra IIIF (v3) information response keys.
     *
     * @throws IOException If there is trouble reading the test file
     */
    @Test
    public final void testGetExtraIIIF3InformationResponseKeys() throws IOException {
        testGetExtraIIIFInformationResponseKeys();
    }

    /**
     * Tests getting extra IIIF information response keys for v2 or v3. We read the keys into a sorted map to be able to
     * get a consistent representation (regardless of JSON formatting).
     *
     * @throws IOException If there is trouble reading the test file
     */
    private void testGetExtraIIIFInformationResponseKeys() throws IOException {
        final Map<String, String> envProperties = System.getenv();
        final String hauthURL = envProperties.get(TestConfig.HAUTH_URL_PROPERTY);
        final String iiifURL = envProperties.get(TestConfig.IIIF_URL_PROPERTY);
        final String[] urls = new String[] { iiifURL, hauthURL, hauthURL };
        final String json = StringUtils.format(StringUtils.read(TEST_FILE_PATH), urls);
        final Request request = new Builder().url(iiifURL + TEST_INFO_FILE).build();
        final ObjectMapper mapper = new ObjectMapper();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            assertEquals(mapper.readTree(json), mapper.readTree(response.body().string()));
        }
    }
}
