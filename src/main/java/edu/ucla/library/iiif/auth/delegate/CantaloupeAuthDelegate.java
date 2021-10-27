
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
     * The scale constraint this delegate uses for degraded images.
     */
    private final long[] myScaleConstraint;

    /**
     * Whether or not access to the requested item is restricted.
     */
    private boolean myItemIsRestricted;

    /**
     * Whether or not the client is allowed to access restricted content.
     */
    private boolean myIsValidIP;

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
     * Called by {@link #preAuthorize()}, caches the results of some computations that are used by more than one method
     * in the delegate. It would be preferable if this code could run in the constructor instead, but that does not
     * appear possible.
     */
    private void cacheRequestMetadata() {
        final Optional<HauthToken> token = getToken(getContext().getRequestHeaders().get(HauthToken.HEADER));
        boolean itemIsRestricted;

        try {
            final HauthItem item = new HauthItem(myAccessService, getContext().getIdentifier());

            itemIsRestricted = item.isRestricted();
        } catch (final IOException details) {
            LOGGER.error(details.getMessage(), details);
            // Q: Do we want to limit access to info.json if auth service is configured and an item isn't found in it?
            itemIsRestricted = false;
        }
        myItemIsRestricted = itemIsRestricted;

        if (token.isPresent() && token.get().isValidIP()) {
            myIsValidIP = true;
        } else {
            myIsValidIP = false;
        }
    }

    /**
     * Authorizes a request for image information. Not all image information will necessarily be calculated at this
     * point in time.
     */
    @Override
    public Object preAuthorize() {
        // Careful with this array; for plain old requests, it is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();

        // This method must be called here, since it sets member variables that are referenced below and by other
        // methods which get called later
        cacheRequestMetadata();

        if (scaleConstraint[0] != scaleConstraint[1]) {
            // This request is for a scaled resource (i.e., already degraded via an earlier HTTP 302 redirect)
            if (scaleConstraint[0] == myScaleConstraint[0] && scaleConstraint[1] == myScaleConstraint[1]) {
                return true;
            } else {
                // The client is requesting something other than the allowed scale constraint
                return false;
            }
        } else if (myItemIsRestricted && !myIsValidIP) {
            // The long types make a difference here, apparently; JRuby?
            return Map.of("status_code", Long.valueOf(HTTP.FOUND), "scale_numerator", myScaleConstraint[0],
                    "scale_denominator", myScaleConstraint[1]);
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
        if (myItemIsRestricted) {
            return getAuthServices();
        } else {
            return Collections.emptyMap();
        }
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
