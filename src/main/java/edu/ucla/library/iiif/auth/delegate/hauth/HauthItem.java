
package edu.ucla.library.iiif.auth.delegate.hauth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

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
     * The access URL of the Hauth service.
     */
    private final String myAccessService;

    /**
     * The ID of the item in question.
     */
    private final String myID;

    /**
     * Creates a new Hauth client.
     *
     * @param aService The URL of the authorization access service
     * @param aID The ID of the item
     */
    public HauthItem(final String aService, final String aID) {
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
    public boolean isRestricted() throws IOException {
        final URI uri = URI.create(StringUtils.format(myAccessService, myID));
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

        LOGGER.debug(MessageCodes.CAD_005, request.method(), uri);

        try {
            final HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString());

            switch (response.statusCode()) {
                case 200:
                    final String authResponseJSON = response.body();
                    final JsonNode node = MAPPER.readTree(authResponseJSON);
                    final List<String> ids = node.findValuesAsText(HauthResponse.ID_KEY);
                    final List<String> accessCodes = node.findValuesAsText(HauthResponse.ACCESS_KEY);

                    if (!ids.isEmpty() && !accessCodes.isEmpty()) {
                        final String accessCode = accessCodes.get(0);
                        final String id = ids.get(0);

                        if (myID.equals(id)) {
                            LOGGER.debug(MessageCodes.CAD_010, myID, accessCode);
                            return Boolean.valueOf(accessCode);
                        }

                        LOGGER.error(MessageCodes.CAD_007, id, myID);
                    } else {
                        LOGGER.error(MessageCodes.CAD_008, authResponseJSON);
                    }

                    break;
                case 404:
                    LOGGER.debug(MessageCodes.CAD_003, myID);
                    return false; // The default for unknowns is that access is not restricted
                default:
                    LOGGER.error(MessageCodes.CAD_004, myID, response.statusCode(), response.body());
                    break;
            }
        } catch (IOException | InterruptedException details) {
            LOGGER.error(details.getMessage(), details);
        }

        return true; // We treat authorization lookup errors as restricted
    }
}
