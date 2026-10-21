
package edu.ucla.library.iiif.auth.delegate;

import static info.freelibrary.util.Constants.COLON;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import info.freelibrary.util.Env;
import info.freelibrary.util.HTTP;

import edu.illinois.library.cantaloupe.delegate.JavaContext;
import edu.illinois.library.cantaloupe.delegate.JavaDelegate;
import edu.illinois.library.cantaloupe.delegate.Logger;

/**
 * An IP checking delegate for UCLA Library.
 */
public class UclaDelegate extends CantaloupeDelegate implements JavaDelegate {
    public static final String[] UCLA_IPS = {"178.156.217.188","5.161.118.194",
        "178.156.154.250","5.161.90.230","69.164.207.190","66.228.53.233",
        "104.237.134.64","66.228.56.140","66.228.56.189","192.155.95.211",
        "52.32.59.248","54.201.202.237","128.97.0.0/16","128.97.8.72/29",
        "128.97.67.0/26","128.97.128.0/26","128.97.238.0/24","149.142.0.0/16",
        "149.142.39.128/25","149.142.49.192/26","149.142.209.0/24","149.142.235.0/24",
        "149.142.252.0/24","164.67.0.0/16","164.67.62.192/27","164.67.128.0/24",
        "164.67.200.0/23","169.232.0.0/16","169.232.0.0/19","169.232.34.0/24",
        "169.232.35.0/24","169.232.44.64/30","169.232.46.0/23","169.232.48.0/24",
        "169.232.53.0/24","169.232.54.0/23","169.232.58.136/29","169.232.58.144/28",
        "169.232.58.160/29","169.232.61.0/24","169.232.62.0/23","169.232.192.0/24",
        "169.232.193.0/26","169.232.193.64/26","169.232.193.128/29","169.232.193.136/29",
        "169.232.193.144/29","169.232.193.152/29","169.232.193.160/27","169.232.206.0/28",
        "169.232.206.16/28","169.232.206.88/29","169.232.206.96/28","169.232.206.120/29",
        "169.232.206.128/28","169.232.206.144/28","169.232.206.160/27","169.232.206.192/28",
        "169.232.206.208/28","169.232.206.224/28","169.232.206.240/28","169.232.208.224/28",
        "169.232.224.0/23","172.16.0.0/12","172.17.90.0/25","172.17.90.128/25","172.17.94.0/24",
        "172.17.108.0/25","172.17.108.128/25","172.20.0.0/17","172.20.128.0/17","192.35.210.0/24",
        "192.35.225.0/24","192.154.2.0/24","192.175.48.1/32","192.175.48.6/32","192.175.48.42/32"};

    /**
     * An environmental property for the scale constraint to use for tiered access.
     * <p>
     * See https://cantaloupe-project.github.io/manual/5.0/access-control.html#Tiered%20Access for more information.
     */
    public static final String TIERED_ACCESS_SCALE_CONSTRAINT = "TIERED_ACCESS_SCALE_CONSTRAINT";

    /** The way we identify if a thumbnail is being requested. */
    private static final String THUMBNAIL = "/full/!200,200/0/";

    /** The scale numerator key for {@link #preAuthorize} responses. */
    private static final String SCALE_NUMERATOR = "scale_numerator";

    /** The scale denominator key for {@link #preAuthorize} responses. */
    private static final String SCALE_DENOMINATOR = "scale_denominator";

    /** The status code for the response. */
    private static final String STATUS_CODE = "status_code";

    /** The value of the WWW-Authenticate HTTP response header. */
    private static final String WWW_AUTHENTICATE_HEADER_VALUE = "Bearer charset=\"UTF-8\"";

    /** Map key for a {@link #preAuthorize} return value; indicates WWW-Authenticate HTTP header should be included. */
    private static final String CHALLENGE = "challenge";

    /** Whether the request is coming from a campus IP. */
    private boolean isCampusIP;

    /**
     * Creates a new instance of {@code UclaDelegate}.
     */
    public UclaDelegate() {
        // This is intentionally left empty
    }

    @Override
    public Object preAuthorize() {
        final JavaContext context = getContext();
        final String id = context.getIdentifier();

        // Allow any kind of thumbnail request, no questions asked
        if (context.getLocalURI().contains(THUMBNAIL)) {
            return true;
        }

        try {
            switch (new AuthDB().getAccessMode(id)) {
                case OPEN:
                    return true;
                case TIERED:
                    switch (getRequestType()) {
                        case INFORMATION:
                            return getTieredInfo();
                        case IMAGE:
                        default:
                            return getTieredImage();
                    }
                case ALL_OR_NOTHING:
                default:
                    throw new RuntimeException("Unexpected access mode value: ALL_OR_NOTHING");
            }
        } catch (final Throwable details) {
            Logger.error(Arrays.stream(details.getStackTrace()).map(element -> "\tat " + element)
                    .collect(Collectors.joining(System.lineSeparator())));
            return getTieredImage(); // Take the most conservative route if we get an exception
        }
    }

    public boolean isUclaIp(final String ipAddress) {
        return Arrays.asList(UCLA_IPS).contains(ipAddress);
    }

    /**
     * Gets the response for a tiered image information request.
     *
     * @return True, false, or a map with scaling information
     */
    private Object getTieredInfo() {
        // For full image requests, this array value is equal to { 1, 1 }
        final int[] scaleConstraint = getContext().getScaleConstraint();
        final int[] configuredScaleConstraint = getScaleConstraint();

        // Full access from an on-campus IP
        if (isCampusIP) {
            return true;
        }

        // Degraded image request for the size we allow (probably via an earlier HTTP 302 redirect)
        if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
            return true;
        }

        // Degraded image request for a size that doesn't match what we've configured and isn't 1:1
        if (scaleConstraint[0] != scaleConstraint[1]) {
            return false; // returns 403
        }

        // Full image request, but non-campus IP (the long types make a difference here)
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
        final int[] configuredScaleConstraint = getScaleConstraint();

        // Degraded image request for the size we allow (probably via an earlier HTTP 302 redirect)
        if (Arrays.equals(configuredScaleConstraint, scaleConstraint)) {
            return true;
        }

        // Degraded image request for a size we don't allow access to; return HTTP 403
        if (scaleConstraint[0] != scaleConstraint[1]) {
            return Map.of(STATUS_CODE, Long.valueOf(HTTP.UNAUTHORIZED), CHALLENGE, WWW_AUTHENTICATE_HEADER_VALUE);
        }

        // Full access from an on-campus IP
        if (isCampusIP) {
            return true;
        }

        // Full image request, but non-campus IP (the long types make a difference here, apparently)
        return Map.of(STATUS_CODE, Long.valueOf(HTTP.FOUND), //
                SCALE_NUMERATOR, (long) configuredScaleConstraint[0], //
                SCALE_DENOMINATOR, (long) configuredScaleConstraint[1]);
    }

    /**
     * Gets a scale constraint for tiered access.
     *
     * @param aScaleConstraint A scale constraint for tiered access
     * @return The scale constraint
     * @throws NumberFormatException If the scale constraint's numbers aren't valid
     * @throws NullPointerException If the scale constraint hasn't been configured
     */
    public int[] getScaleConstraint() throws NumberFormatException {
        final String scaleConstraintCfg = Env.getOrFail(TIERED_ACCESS_SCALE_CONSTRAINT, "No scale constraint found");
        final int[] scaleConstraint = Stream.of(scaleConstraintCfg.split(COLON)).mapToInt(Integer::parseInt).toArray();

        // Must be able to be mapped to an array of length 2, and numerator must be less than the denominator
        if (scaleConstraint.length != 2 || scaleConstraint[0] >= scaleConstraint[1]) {
            throw new NumberFormatException(scaleConstraintCfg);
        }

        return scaleConstraint;
    }

    /**
     * The different types of requests that this delegate may process.
     */
    private enum RequestType {

        /** An information request type. */
        INFORMATION,

        /** An image request type. */
        IMAGE;
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
