
package edu.ucla.library.iiif.auth.delegate.hauth;

import static edu.ucla.library.iiif.auth.delegate.hauth.HauthResponse.ACCESS_KEY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.auth.delegate.ConfigException;
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
     * An object mapper for reading JSON.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The access URI of the Hauth service.
     */
    private final URI myAccessService;

    /**
     * The ID of the item in question.
     */
    private final String myID;

    /**
     * Creates a new Hauth client.
     *
     * @param aService The URI of the authorization access service
     * @param aID The ID of the item
     */
    public HauthItem(final URI aService, final String aID) {
        myAccessService = aService;
        myID = aID;
    }

    /**
     * Gets the ID of this item.
     *
     * @return The ID of this item
     */
    public String getID() {
        return myID;
    }

    /**
     * Returns whether access to the object with the supplied ID is restricted.
     *
     * @return Whether access to the object with the supplied ID is restricted
     */
    public boolean isRestricted() {
        final HttpRequest request = HttpRequest.newBuilder().uri(getURI()).build();

        LOGGER.debug(MessageCodes.CAD_005, request.method(), request.uri());

        try {
            final HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());

            switch (response.statusCode()) {
                case 200:
                    // This will default to 'false' if value is unexpected, throw NoSuchElementException if missing
                    return Optional.of(MAPPER.readTree(response.body()).get(ACCESS_KEY)).orElseThrow().asBoolean();
                case 404:
                    LOGGER.debug(MessageCodes.CAD_003, myID);
                    // Q: Do we want to limit access to info.json if auth service is configured and an item isn't found
                    // in it?
                    return false; // The default for unknowns is that access is not restricted
                default:
                    LOGGER.error(MessageCodes.CAD_004, myID, response.statusCode(), response.body());
                    break;
            }
        } catch (IOException | InterruptedException | NoSuchElementException details) {
            LOGGER.error(details.getMessage(), details);
        }

        return true; // We treat authorization lookup errors as restricted
    }

    /**
     * Constructs the Access Service URI by appending the requested ID onto the end of the service URI's path.
     *
     * @return A URI for the access service with the requested ID included
     */
    private URI getURI() {
        final URIBuilder uriBuilder = new URIBuilder(myAccessService);
        final List<String> paths = uriBuilder.getPathSegments();

        // Add our requested ID onto the end of the service URI
        paths.add(myID);

        // The base should already be valid at this point, but we have to check the ID
        try {
            return uriBuilder.setPathSegments(paths).build();
        } catch (final URISyntaxException details) {
            throw new ConfigException(details, uriBuilder.toString());
        }
    }
}
