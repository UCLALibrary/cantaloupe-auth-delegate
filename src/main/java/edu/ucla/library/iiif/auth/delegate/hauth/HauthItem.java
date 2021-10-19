
package edu.ucla.library.iiif.auth.delegate.hauth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import edu.ucla.library.iiif.auth.delegate.Config;
import edu.ucla.library.iiif.auth.delegate.MessageCodes;

/**
 * A client for interacting with the Hauth service.
 */
public class HauthItem {

    /**
     * The Hauth item logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HauthItem.class, MessageCodes.BUNDLE);

    /**
     * An internal HTTP client.
     */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * The access URL of the Hauth service.
     */
    private final String myAccessService;

    /**
     * Creates a new Hauth client.
     *
     * @param aConfig The application configuration
     */
    public HauthItem(final Config aConfig) {
        myAccessService = aConfig.getAccessService();
        LOGGER.debug("Configured access service: {}", myAccessService);
    }

    /**
     * Returns whether access to the object with the supplied ID is restricted.
     *
     * @param aID An object ID
     * @return Whether access to the object with the supplied ID is restricted
     */
    public boolean isRestricted(final String aID) throws IOException {
        final URI uri = URI.create(StringUtils.format(myAccessService, aID));
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

        LOGGER.debug("Making test request: {}", uri);

        try {
            final HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());

            switch (response.statusCode()) {
                case 200:
                    LOGGER.debug(MessageCodes.CAD_000, "200");
                    break;
                case 404:
                    LOGGER.debug(MessageCodes.CAD_000, "404");
                    break;
                case 500:
                    LOGGER.debug(MessageCodes.CAD_000, "500");
                    break;
                default:
                    LOGGER.debug(MessageCodes.CAD_000, "ERROR");
                    break;
            }
        } catch (IOException | InterruptedException details) {
            LOGGER.error(details.getMessage(), details);
        }

        return true;
    }
}
