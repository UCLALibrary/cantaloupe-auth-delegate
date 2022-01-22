
package edu.ucla.library.iiif.auth.delegate;

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

import edu.ucla.library.iiif.auth.delegate.hauth.AccessMode;
import edu.ucla.library.iiif.auth.delegate.hauth.HauthItem;
import edu.ucla.library.iiif.auth.delegate.hauth.HauthSinaiToken;
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
     * The status code key for {@link #preAuthorize} responses.
     */
    private static final String STATUS_CODE = "status_code";

    /**
     * The regex pattern for a single whitespace character.
     */
    private static final String SINGLE_SPACE_PATTERN = "\\s";

    /**
     * The configuration for this delegate.
     */
    private final Config myConfig;

    /**
     * Whether or not access to the requested item is restricted.
     */
    private AccessMode myAccessMode;

    /**
     * Whether or not it is unnecessary to include IIIF authentication services in the info.json response.
     */
    private boolean myClientAlreadyHasFullAccess = true;

    /**
     * Creates a new Cantaloupe authorization delegate.
     */
    public CantaloupeAuthDelegate() {
        myConfig = new Config();
    }

    /**
     * Authorizes a request for image information. Not all image information will necessarily be calculated at this
     * point in time.
     */
    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public Object preAuthorize() {
        final String authorizationHeader = getContext().getRequestHeaders().get(HauthToken.HEADER);
        final int[] configuredScaleConstraint = myConfig.getScaleConstraint();
        // For full image requests, this array value is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();

        // Cache the result of the access level HTTP request
        myAccessMode = new HauthItem(myConfig.getAccessModeService(), getContext().getIdentifier()).getAccessMode();

        switch (myAccessMode) {
            case OPEN:
                return true;
            case TIERED:
                final Optional<HauthToken> token = getToken(authorizationHeader);

                if (token.isPresent() && token.get().isValidIP()) {
                    // Full access
                    return true;
                } else if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
                    // Degraded image request for the size we allow access to
                    // (Probably via an earlier HTTP 302 redirect)
                    myClientAlreadyHasFullAccess = false;

                    return true;
                } else if (scaleConstraint[0] != scaleConstraint[1]) {
                    // Degraded image request for a size we don't allow access to; return HTTP 403
                    return false;
                } else {
                    // Full image request, but non-campus IP
                    // (The long types make a difference here, apparently)
                    return Map.of(STATUS_CODE, Long.valueOf(HTTP.FOUND), //
                            "scale_numerator", (long) configuredScaleConstraint[0], //
                            "scale_denominator", (long) configuredScaleConstraint[1]);
                }
            case ALL_OR_NOTHING:
            default:
                final Optional<HauthSinaiToken> sinaiToken = getSinaiToken(authorizationHeader);

                if (sinaiToken.isPresent() && sinaiToken.get().hasSinaiAffiliate()) {
                    // Full access
                    return true;
                } else {
                    // No access
                    myClientAlreadyHasFullAccess = false;

                    return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), //
                            "challenge", "Bearer charset=\"UTF-8\"");
                }
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
        if (myClientAlreadyHasFullAccess) {
            return Collections.emptyMap();
        } else {
            return Collections.singletonMap(JsonKeys.SERVICE, List.of(getAuthServices()));
        }
    }

    /**
     * Gets the item's authentication service description.
     *
     * @return An auth service description
     */
    private Map<String, Object> getAuthServices() {
        final AuthCookieService1 cookieService;
        final AuthTokenService1 tokenService;

        switch (myAccessMode) {
            case TIERED:
                tokenService = new AuthTokenService1(myConfig.getTokenService());
                cookieService = new AuthCookieService1(Profile.KIOSK, myConfig.getCookieService(), null, tokenService);
                break;
            case ALL_OR_NOTHING:
                tokenService = new AuthTokenService1(myConfig.getSinaiTokenService());
                // https://github.com/ksclarke/jiiify-presentation/issues/155
                cookieService = new AuthCookieService1(Profile.EXTERNAL, "http://example.com", null, tokenService);
                break;
            case OPEN:
            default:
                // This branch should never be reached, but just in case
                return Collections.emptyMap();
        }

        return JSON.convertValue(cookieService, MAP_TYPE_REFERENCE);
    }

    /**
     * Gets a Hauth token from the supplied header value.
     *
     * @param aHeaderValue An authorization header value
     * @return An optional Hauth authorization token
     */
    private Optional<HauthToken> getToken(final String aHeaderValue) {
        if (aHeaderValue != null) {
            final String[] tokenParts = aHeaderValue.split(SINGLE_SPACE_PATTERN);

            if (tokenParts.length == 2 && HauthToken.TYPE.equalsIgnoreCase(tokenParts[0])) {
                try {
                    final String value = new String(Base64.getDecoder().decode(tokenParts[1]));

                    LOGGER.debug(value);

                    return Optional.ofNullable(JSON.getReader(HauthToken.class).readValue(value));
                } catch (final IllegalArgumentException | JsonProcessingException details) {
                    LOGGER.trace(details.getMessage(), details);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Gets a Sinai token from the supplied header value.
     *
     * @param aHeaderValue An authorization header value
     * @return An optional Sinai authorization token
     */
    private Optional<HauthSinaiToken> getSinaiToken(final String aHeaderValue) {
        if (aHeaderValue != null) {
            final String[] tokenParts = aHeaderValue.split(SINGLE_SPACE_PATTERN);

            if (tokenParts.length == 2 && HauthToken.TYPE.equalsIgnoreCase(tokenParts[0])) {
                try {
                    final String value = new String(Base64.getDecoder().decode(tokenParts[1]));

                    LOGGER.debug(value);

                    return Optional.ofNullable(JSON.getReader(HauthSinaiToken.class).readValue(value));
                } catch (final IllegalArgumentException | JsonProcessingException details) {
                    LOGGER.trace(details.getMessage(), details);
                }
            }
        }

        return Optional.empty();
    }
}
