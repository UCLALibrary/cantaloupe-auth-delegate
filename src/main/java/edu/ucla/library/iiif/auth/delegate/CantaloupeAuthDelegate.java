
package edu.ucla.library.iiif.auth.delegate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthCookieService1.Profile;
import info.freelibrary.iiif.presentation.v3.services.auth.AuthTokenService1;
import info.freelibrary.iiif.presentation.v3.utils.JSON;
import info.freelibrary.iiif.presentation.v3.utils.JsonKeys;

import edu.illinois.library.cantaloupe.delegate.JavaContext;
import edu.illinois.library.cantaloupe.delegate.JavaDelegate;
import edu.illinois.library.cantaloupe.delegate.Logger;

/**
 * A Cantaloupe delegate for handing IIIF Auth interactions.
 */
public class CantaloupeAuthDelegate extends GenericAuthDelegate implements JavaDelegate {

    /**
     * A Jackson TypeReference for a Map.
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    /**
     * Authorizes a request for image information. Not all image information will necessarily be calculated at this
     * point in time.
     */
    @Override
    public Object preAuthorize() {
        Logger.info("The pre-authorize identifier is: " + getContext().getIdentifier());
        return true;
    }

    /**
     * Authorizes a request for an image.
     */
    @Override
    public Object authorize() {
        Logger.debug("authorize");
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

        if (!token.isPresent()) {
            return getAuthServices();
        }

        return Collections.emptyMap();
    }

    /**
     * Gets the authentication services available.
     *
     * @return A map of authorization services
     */
    private Map<String, Object> getAuthServices() {
        final Config config = getConfig();
        final AuthCookieService1 cookieService = new AuthCookieService1(Profile.KIOSK, config.getCookieService());
        final AuthTokenService1 tokenService = new AuthTokenService1(config.getTokenService());
        final Map<String, Object> services = JSON.convertValue(cookieService, MAP_TYPE_REFERENCE);
        final List<Map<String, Object>> relatedServices = new ArrayList<>();

        relatedServices.add(JSON.convertValue(tokenService, MAP_TYPE_REFERENCE));
        services.put(JsonKeys.SERVICE, relatedServices);

        return Collections.singletonMap(JsonKeys.SERVICE, services);
    }

    /**
     * Gets the application configuration from the system environment.
     *
     * @return Configuration properties for the authorization delegate
     * @throws ConfigException If the expected configuration property is missing or invalid
     */
    private Config getConfig() {
        final Map<String, String> envMap = System.getenv();
        final Config config = new Config();

        if (!envMap.containsKey(Config.AUTH_COOKIE_SERVICE)) {
            throw new ConfigException(Config.AUTH_COOKIE_SERVICE);
        }

        if (!envMap.containsKey(Config.AUTH_TOKEN_SERVICE)) {
            throw new ConfigException(Config.AUTH_TOKEN_SERVICE);
        }

        try {
            config.setCookieService(new URL(envMap.get(Config.AUTH_COOKIE_SERVICE)));
        } catch (final MalformedURLException details) {
            throw new ConfigException(details, envMap.get(Config.AUTH_COOKIE_SERVICE));
        }

        try {
            config.setTokenService(new URL(envMap.get(Config.AUTH_TOKEN_SERVICE)));
        } catch (final MalformedURLException details) {
            throw new ConfigException(details, envMap.get(Config.AUTH_TOKEN_SERVICE));
        }

        return config;
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
                    Logger.trace(details.getMessage(), details);
                }
            }
        }

        return Optional.empty();
    }

}
