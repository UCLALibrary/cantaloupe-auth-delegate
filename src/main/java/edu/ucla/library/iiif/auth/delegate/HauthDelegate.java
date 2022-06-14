
package edu.ucla.library.iiif.auth.delegate; // NOPMD - Excessive imports

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

import info.freelibrary.iiif.presentation.v3.services.AuthCookieService1;
import info.freelibrary.iiif.presentation.v3.services.AuthTokenService1;
import info.freelibrary.iiif.presentation.v3.services.ExternalCookieService1;
import info.freelibrary.iiif.presentation.v3.services.KioskCookieService1;
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
     * The name of the X-Forwarded-For HTTP request header.
     */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

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
     * The scale numerator key for {@link #preAuthorize} responses.
     */
    private static final String SCALE_NUMERATOR = "scale_numerator";

    /**
     * The scale denominator key for {@link #preAuthorize} responses.
     */
    private static final String SCALE_DENOMINATOR = "scale_denominator";

    /**
     * The regex pattern for a single whitespace character.
     */
    private static final String SINGLE_SPACE_PATTERN = "\\s";

    /**
     * The access token JSON key.
     */
    private static final String ACCESS_TOKEN = "accessToken";

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
                LOGGER.debug(MessageCodes.CAD_010);
                return true;
            case TIERED:
                LOGGER.debug(MessageCodes.CAD_011);
                switch (getRequestType()) {
                    case INFORMATION:
                        return getTieredInfo(authorizationHeader);
                    case IMAGE:
                    default:
                        return getTieredImage();
                }
            case ALL_OR_NOTHING:
            default:
                LOGGER.debug(MessageCodes.CAD_012);
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

        LOGGER.debug(MessageCodes.CAD_013);

        // Full access from an on-campus IP
        if (token.isPresent() && token.get().isValidIP()) {
            LOGGER.debug(MessageCodes.CAD_014);
            return true;
        }

        // Degraded image request for the size we allow (probably via an earlier HTTP 302 redirect)
        if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
            return myInfoJsonShouldContainAuth = true;
        }

        // Degraded image request for a size that doesn't match what we've configured and isn't 1:1
        if (scaleConstraint[0] != scaleConstraint[1]) {
            LOGGER.debug(MessageCodes.CAD_015, scaleConstraint[0], scaleConstraint[1]);
            return false; // returns 403
        }

        // Full image request, but non-campus IP (the long types make a difference here, apparently)
        LOGGER.debug(MessageCodes.CAD_016);
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.FOUND), //
                SCALE_NUMERATOR, (long) configuredScaleConstraint[0], //
                SCALE_DENOMINATOR, (long) configuredScaleConstraint[1]);
    }

    /**
     * Gets the response for a IIIF tiered image request.
     *
     * @return True or a map with an unauthorized status response
     */
    private Object getTieredImage() {
        // For full image requests, this array value is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();
        final int[] configuredScaleConstraint = myConfig.getScaleConstraint();
        final String cookieHeader = getContext().getRequestHeaders().get(COOKIE);
        final Optional<String> xForwardedForHeader =
                Optional.ofNullable(getContext().getRequestHeaders().get(X_FORWARDED_FOR));

        LOGGER.debug(MessageCodes.CAD_017);

        // Degraded image request for the size we allow (probably via an earlier HTTP 302 redirect)
        if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
            LOGGER.debug(MessageCodes.CAD_027);
            return true;
        }

        // Degraded image request for a size we don't allow access to; return HTTP 403
        if (scaleConstraint[0] != scaleConstraint[1]) {
            LOGGER.debug(MessageCodes.CAD_028, scaleConstraint[0], scaleConstraint[1]);
            return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
        }

        // Full access from an on-campus IP
        if (cookieHeader != null && hasCampusNetworkCookie(cookieHeader, xForwardedForHeader)) {
            LOGGER.debug(MessageCodes.CAD_018);
            return true;
        }

        // Full image request, but non-campus IP (the long types make a difference here, apparently)
        LOGGER.debug(MessageCodes.CAD_019);
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.FOUND), //
                SCALE_NUMERATOR, (long) configuredScaleConstraint[0], //
                SCALE_DENOMINATOR, (long) configuredScaleConstraint[1]);
    }

    /**
     * Gets the response for a all-or-nothing image information request.
     *
     * @param aAuthHeader An authorization header
     * @return True or a map with an unauthorized status response
     */
    private Object getAllOrNothingInfo(final String aAuthHeader) {
        final Optional<HauthSinaiToken> sinaiToken = getSinaiToken(aAuthHeader);

        LOGGER.debug(MessageCodes.CAD_020);

        // Full access is granted with a valid token
        if (sinaiToken.isPresent() && sinaiToken.get().hasSinaiAffiliate()) {
            LOGGER.debug(MessageCodes.CAD_021);
            return true;
        }

        // Auth services should be added to the info.json
        myInfoJsonShouldContainAuth = true;

        // No access without a token
        LOGGER.debug(MessageCodes.CAD_022);
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
    }

    /**
     * Gets the response for an all-or-nothing image request.
     *
     * @return True or a map with an unauthorized status response
     */
    private Object getAllOrNothingImage() {
        final String cookieHeader = getContext().getRequestHeaders().get(COOKIE);
        final Optional<String> xForwardedForHeader =
                Optional.ofNullable(getContext().getRequestHeaders().get(X_FORWARDED_FOR));

        LOGGER.debug(MessageCodes.CAD_023);

        // Full access
        if (cookieHeader != null && hasSinaiAffiliateCookies(cookieHeader, xForwardedForHeader)) {
            LOGGER.debug(MessageCodes.CAD_024);
            return true;
        }

        // No access
        LOGGER.debug(MessageCodes.CAD_025);
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
    }

    /**
     * Gets the item's authentication service description.
     *
     * @return An auth service description
     */
    private Map<String, Object> getAuthServices() {
        final AuthCookieService1<?> cookieService;
        final AuthTokenService1 tokenService;
        final Map<String, Object> serviceMap;
        final String label;

        switch (myAccessMode) {
            case TIERED:
                tokenService = new AuthTokenService1(myConfig.getTokenService());
                cookieService = new KioskCookieService1(myConfig.getCookieService(), tokenService);
                label = "Internal cookie granting service";
                break;
            case ALL_OR_NOTHING:
                tokenService = new AuthTokenService1(myConfig.getSinaiTokenService());
                cookieService = new ExternalCookieService1(tokenService);
                label = "External authentication required";
                break;
            case OPEN:
            default:
                // The OPEN and default branches should not be reachable, but are included here just in case
                return Collections.emptyMap();
        }

        // Workaround for Mirador bug that requires label be present (Cf. https://bitly.com/3NllMLq+)
        serviceMap = JSON.convertValue(cookieService, MAP_TYPE_REFERENCE);
        serviceMap.putIfAbsent(JsonKeys.LABEL, label);

        return serviceMap;
    }

    /**
     * Determines whether or not the client can prove campus network access.
     *
     * @param aCookieHeader The Cookie HTTP request header
     * @param anXForwardedForHeader The X-Forwarded-For HTTP request header
     * @return Whether or not the cookie proves campus network access
     */
    private boolean hasCampusNetworkCookie(final String aCookieHeader, final Optional<String> anXForwardedForHeader) {
        final URI tokenService = myConfig.getTokenService();
        final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(tokenService).header(COOKIE, aCookieHeader);
        final ObjectMapper mapper = new ObjectMapper();

        anXForwardedForHeader.ifPresent(xff -> builder.header(X_FORWARDED_FOR, xff));

        try {
            final HttpResponse<String> response =
                    HttpClient.newHttpClient().send(builder.build(), BodyHandlers.ofString());
            final JsonNode body = mapper.readTree(response.body());

            if (body.has(ACCESS_TOKEN)) {
                final String encodedAccessToken = body.get(ACCESS_TOKEN).asText();
                final String accessToken = new String(Base64.getDecoder().decode(encodedAccessToken));
                final boolean accessAllowed = mapper.readTree(accessToken).get("campusNetwork").asBoolean();

                if (!accessAllowed) {
                    // Cookie found, but it's not what we were expecting
                    LOGGER.error(MessageCodes.CAD_008, aCookieHeader);
                }

                return accessAllowed;
            }

            return false;
        } catch (final InterruptedException | IOException details) {
            LOGGER.error(details, details.getMessage());
            return false; // QUESTION: Should we retry?
        }
    }

    /**
     * Determines whether or not the client can prove Sinai affiliation.
     *
     * @param aCookieHeader The Cookie HTTP request header
     * @param anXForwardedForHeader The X-Forwarded-For HTTP request header
     * @return Whether or not the cookies prove Sinai affiliation
     */
    private boolean hasSinaiAffiliateCookies(final String aCookieHeader, final Optional<String> anXForwardedForHeader) {
        final URI tokenService = myConfig.getSinaiTokenService();
        final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(tokenService).headers(COOKIE, aCookieHeader);
        final ObjectMapper mapper = new ObjectMapper();

        anXForwardedForHeader.ifPresent(xff -> builder.header(X_FORWARDED_FOR, xff));

        try {
            final HttpResponse<String> response =
                    HttpClient.newHttpClient().send(builder.build(), BodyHandlers.ofString());
            final JsonNode body = mapper.readTree(response.body());

            if (body.has(ACCESS_TOKEN)) {
                final String encodedAccessToken = body.get(ACCESS_TOKEN).asText();
                final String accessToken = new String(Base64.getDecoder().decode(encodedAccessToken));

                return mapper.readTree(accessToken).get("sinaiAffiliate").asBoolean();
            }

            return false;
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
