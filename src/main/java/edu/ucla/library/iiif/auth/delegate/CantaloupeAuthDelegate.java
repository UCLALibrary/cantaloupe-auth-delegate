
package edu.ucla.library.iiif.auth.delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1.Profile;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthTokenService1;
import info.freelibrary.iiif.presentation.v3.utils.JSON;
import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import edu.ucla.library.iiif.auth.delegate.hauth.HauthItem;
import edu.ucla.library.iiif.auth.delegate.hauth.HauthToken;

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
     * The scale constraint this delegate allows for degraded images.
     */
    private final int[] myScaleConstraint;

    /**
     * Whether or not access to the requested item is restricted.
     */
    private boolean isItemRestricted;

    /**
     * Creates a new Cantaloupe authorization delegate.
     */
    public CantaloupeAuthDelegate() {
        final Config config = new Config();

        myAccessService = config.getAccessService();
        myCookieService = config.getCookieService();
        myTokenService = config.getTokenService();
        myScaleConstraint = config.getScaleConstraint();
    }

    /**
     * Authorizes a request for image information. Not all image information will necessarily be calculated at this
     * point in time.
     */
    @Override
    public Object preAuthorize() {
        final Optional<HauthToken> token = getToken(getContext().getRequestHeaders().get(HauthToken.HEADER));
        final boolean hasValidIP = token.isPresent() && token.get().isValidIP();
        // For full image requests, this array value is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();

        // Cache the result of the access level HTTP request
        isItemRestricted = new HauthItem(myAccessService, getContext().getIdentifier()).isRestricted();

        if (scaleConstraint[0] != scaleConstraint[1]) {
            // This request is for a scaled resource (i.e., already degraded via an earlier HTTP 302 redirect)
            // Make sure the requested scale constraint is the one that we allow
            return Arrays.equals(myScaleConstraint, scaleConstraint);
        } else if (isItemRestricted && !hasValidIP) {
            // The long types make a difference here, apparently; JRuby?
            return Map.of("status_code", Long.valueOf(HTTP.FOUND), "scale_numerator", (long) myScaleConstraint[0],
                    "scale_denominator", (long) myScaleConstraint[1]);
        } else {
            // Client is authorized to view full resource
            return true;
        }
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
        return isItemRestricted ? getAuthServices() : Collections.emptyMap();
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
