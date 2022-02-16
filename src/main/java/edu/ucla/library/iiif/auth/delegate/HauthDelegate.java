
package edu.ucla.library.iiif.auth.delegate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
public class HauthDelegate extends CantaloupeDelegate implements JavaDelegate {

    /**
     * The authorization delegate's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HauthDelegate.class, MessageCodes.BUNDLE);

    /**
     * A Jackson TypeReference for a Map.
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    /**
     * The name of the Cookie HTTP request header.
     */
    private static final String COOKIE = "Cookie";

    /**
     * The Map key, for a {@link #preAuthorize} return value, used to indicate that a WWW-Authenticate HTTP header
     * should be included in the response.
     */
    private static final String CHALLENGE = "challenge";

    /**
     * The value of the WWW-Authenticate HTTP response header.
     */
    private static final String WWW_AUTHENTICATE_HEADER_VALUE = "Bearer charset=\"UTF-8\"";

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
     * If the current request is for info.json, whether or not a IIIF authentication service description should be
     * included in the response.
     */
    private boolean myInfoJsonShouldContainAuth;

    /**
     * Creates a new Cantaloupe authorization delegate.
     */
    public HauthDelegate() {
        myConfig = new Config();
    }

    /**
     * Authorizes a request before having read an image. Not all image information will necessarily be calculated at
     * this point in time.
     */
    @Override
    public Object preAuthorize() {
        final String authorizationHeader = getContext().getRequestHeaders().get(HauthToken.HEADER);

        // Cache the result of the access level HTTP request
        myAccessMode = new HauthItem(myConfig.getAccessService(), getContext().getIdentifier()).getAccessMode();

        switch (myAccessMode) {
            case OPEN:
                return true;
            case TIERED:
                switch (getRequestType()) {
                    case INFORMATION:
                        return getTieredInfo(authorizationHeader);
                    case IMAGE:
                    default:
                        return getTieredImage();
                }
            case ALL_OR_NOTHING:
            default:
                switch (getRequestType()) {
                    case INFORMATION:
                        return getAllOrNothingInfo(authorizationHeader);
                    case IMAGE:
                    default:
                        return getAllOrNothingImage();
                }
        }
    }

    @Override
    public Map<String, Object> getExtraIIIF2InformationResponseKeys() {
        return getExtraIIIF3InformationResponseKeys();
    }

    @Override
    public Map<String, Object> getExtraIIIF3InformationResponseKeys() {
        if (myInfoJsonShouldContainAuth) {
            return Collections.singletonMap(JsonKeys.SERVICE, List.of(getAuthServices()));
        }

        return Collections.emptyMap();
    }

    /**
     * Gets the response for a tiered image information request.
     *
     * @param aAuthHeader An authorization header
     * @return True, false, or a map with scaling information
     */
    private Object getTieredInfo(final String aAuthHeader) {
        // For full image requests, this array value is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();
        final int[] configuredScaleConstraint = myConfig.getScaleConstraint();
        final Optional<HauthToken> token = getToken(aAuthHeader);

        // Full access from an on-campus IP
        if (token.isPresent() && token.get().isValidIP()) {
            return true;
        }

        // Degraded image request for the size we allow (probably via an earlier HTTP 302 redirect)
        if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
            return myInfoJsonShouldContainAuth = true;
        }

        // Degraded image request for a size we don't allow access to; return HTTP 403
        if (scaleConstraint[0] != scaleConstraint[1]) {
            return false;
        }

        // Full image request, but non-campus IP (the long types make a difference here, apparently)
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.FOUND), //
                "scale_numerator", (long) configuredScaleConstraint[0], //
                "scale_denominator", (long) configuredScaleConstraint[1]);
    }

    /**
     * Gets the response for a IIIF tiered image request.
     *
     * @return True or a map with an unauthorized status response
     */
    private Object getTieredImage() {
        final String cookieHeader = getContext().getRequestHeaders().get(COOKIE);

        if (cookieHeader != null) {
            try {
                final byte[] cookie = Base64.getDecoder().decode(cookieHeader);
                final JsonNode cookieNode = new ObjectMapper().readTree(cookie);
                final JsonNode accessAllowed = cookieNode.get(HauthToken.CAMPUS_NETWORK_KEY);

                // Cookie found, access allowed
                if (accessAllowed != null && accessAllowed.asBoolean()) {
                    return true;
                }

                // Cookie found, but it's not what we were expecting
                LOGGER.error(MessageCodes.CAD_008, cookie);
            } catch (final IOException details) {
                LOGGER.error(details, details.getMessage());
            }
        }

        // No access without a cookie
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
    }

    /**
     * Gets the response for a all-or-nothing image information request.
     *
     * @param aAuthHeader An authorization header
     * @return True or a map with an unauthorized status response
     */
    private Object getAllOrNothingInfo(final String aAuthHeader) {
        final Optional<HauthSinaiToken> sinaiToken = getSinaiToken(aAuthHeader);

        // Full access is granted with a valid token
        if (sinaiToken.isPresent() && sinaiToken.get().hasSinaiAffiliate()) {
            return true;
        }

        // Auth services should be added to the info.json
        myInfoJsonShouldContainAuth = true;

        // No access without a token
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
    }

    /**
     * Gets the response for an all-or-nothing image request.
     *
     * @return True or a map with an unauthorized status response
     */
    private Object getAllOrNothingImage() {
        final String cookieHeader = getContext().getRequestHeaders().get(COOKIE);

        // Full access
        if (cookieHeader != null && hasSinaiAffiliateCookies(cookieHeader)) {
            return true;
        }

        // No access
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
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
                // Cf. https://github.com/ksclarke/jiiify-presentation/issues/155
                cookieService = new AuthCookieService1(Profile.EXTERNAL, "http://example.com", null, tokenService);
                break;
            case OPEN:
            default:
                // The OPEN and default branches should not be reachable, but are included here just in case
                return Collections.emptyMap();
        }

        return JSON.convertValue(cookieService, MAP_TYPE_REFERENCE);
    }

    /**
     * Determines whether or not the client can prove Sinai affiliation.
     *
     * @param aCookieHeader The Cookie HTTP request header
     * @return Whether or not the cookies prove Sinai affiliation
     */
    private boolean hasSinaiAffiliateCookies(final String aCookieHeader) {
        final URI tokenService = myConfig.getSinaiTokenService();
        final HttpRequest request = HttpRequest.newBuilder().uri(tokenService).header(COOKIE, aCookieHeader).build();
        final ObjectMapper mapper = new ObjectMapper();

        try {
            final HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
            final String encodedAccessToken = mapper.readTree(response.body()).get("accessToken").asText();
            final String innerToken = new String(Base64.getDecoder().decode(encodedAccessToken));

            return mapper.readTree(innerToken).get("sinaiAffiliate").asBoolean();
        } catch (final InterruptedException | IOException details) {
            LOGGER.error(details, details.getMessage());
            return false; // QUESTION: Should we retry?
        }
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

    /**
     * The different types of requests that this delegate may process.
     */
    private enum RequestType {
        INFORMATION, IMAGE;
    }

    /**
     * Gets the request type of the current request.
     *
     * @return Whether this request is for an info.json or an image
     */
    private RequestType getRequestType() {
        if (getContext().getRequestURI().endsWith("info.json")) {
            return RequestType.INFORMATION;
        }

        return RequestType.IMAGE;
    }
}
