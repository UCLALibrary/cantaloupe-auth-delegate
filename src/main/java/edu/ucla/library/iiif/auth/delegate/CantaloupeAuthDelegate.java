
package edu.ucla.library.iiif.auth.delegate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1.Profile;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthTokenService1;
import info.freelibrary.iiif.presentation.v3.utils.JSON;
import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import edu.ucla.library.iiif.auth.delegate.hauth.HauthItem;
import edu.ucla.library.iiif.auth.delegate.hauth.HauthToken;

import edu.illinois.library.cantaloupe.delegate.JavaContext;
import edu.illinois.library.cantaloupe.delegate.JavaDelegate;

/**
 * A Cantaloupe delegate for handing IIIF Auth interactions.
 */
public class CantaloupeAuthDelegate extends GenericAuthDelegate implements JavaDelegate {

    /**
     * The authorization delegate's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CantaloupeAuthDelegate.class, MessageCodes.BUNDLE);

    /**
     * A Jackson TypeReference for a Map.
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    /**
     * The access service this delegate uses.
     */
    private final String myAccessService;

    /**
     * The cookie service this delegate uses.
     */
    private final String myCookieService;

    /**
     * The token service this delegate uses.
     */
    private final String myTokenService;

    /**
     * Creates a new Cantaloupe authorization delegate.
     */
    public CantaloupeAuthDelegate() {
        final Config config = new Config();

        myAccessService = config.getAccessService();
        myCookieService = config.getCookieService();
        myTokenService = config.getTokenService();
    }

    /**
     * Authorizes a request for image information. Not all image information will necessarily be calculated at this
     * point in time.
     */
    @Override
    public Object preAuthorize() {
        // Q: Do we want to limit access to info.json if auth service is configured and an item isn't found in it?
        return true;
    }

    /**
     * Authorizes a request for an image.
     */
    @Override
    public Object authorize() {
        return true;
    }

    @Override
    public Map<String, Object> getExtraIIIF2InformationResponseKeys() {
        return getExtraInformationResponseKeys();
    }

    @Override
    public Map<String, Object> getExtraIIIF3InformationResponseKeys() {
        return getExtraInformationResponseKeys();
    }

    /**
     * Gets additional image information response keys to add to the response.
     *
     * @return A map of additional response keys
     */
    private Map<String, Object> getExtraInformationResponseKeys() {
        final JavaContext context = getContext();
        final Map<String, String> headers = context.getRequestHeaders();
        final Optional<HauthToken> token = getToken(headers.get(HauthToken.HEADER));
        final HauthItem item = new HauthItem(myAccessService, context.getIdentifier());

        try {
            if (!token.isPresent() && item.isRestricted()) {
                return getAuthServices();
            }
        } catch (final IOException details) {
            LOGGER.error(details.getMessage(), details);
        }

        return Collections.emptyMap();
    }

    /**
     * Gets the authentication services available.
     *
     * @return A map of authorization services
     */
    private Map<String, Object> getAuthServices() {
        final AuthCookieService1 cookieService = new AuthCookieService1(Profile.KIOSK, myCookieService);
        final AuthTokenService1 tokenService = new AuthTokenService1(myTokenService);
        final Map<String, Object> service = JSON.convertValue(cookieService, MAP_TYPE_REFERENCE);
        final List<Map<String, Object>> relatedServices = new ArrayList<>();
        final List<Map<String, Object>> services = new ArrayList<>();

        relatedServices.add(JSON.convertValue(tokenService, MAP_TYPE_REFERENCE));
        service.put(JsonKeys.SERVICE, relatedServices);
        services.add(service);

        return Collections.singletonMap(JsonKeys.SERVICE, services);
    }

    /**
     * Gets a Hauth token from the supplied header value.
     *
     * @param aHeaderValue An authorization header value
     * @return An optional Hauth authorization token
     */
    private Optional<HauthToken> getToken(final String aHeaderValue) {
        if (aHeaderValue != null) {
            final String[] tokenParts = aHeaderValue.split("\\s");

            if (tokenParts.length == 2 && HauthToken.TYPE.equalsIgnoreCase(tokenParts[0])) {
                final String value = new String(Base64.getDecoder().decode(tokenParts[1]));

                try {
                    return Optional.ofNullable(JSON.getReader(HauthToken.class).readValue(value));
                } catch (final JsonProcessingException details) {
                    LOGGER.trace(details.getMessage(), details);
                }
            }
        }

        return Optional.empty();
    }

}
