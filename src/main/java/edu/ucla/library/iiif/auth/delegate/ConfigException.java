
package edu.ucla.library.iiif.auth.delegate;

import java.net.URISyntaxException;

import info.freelibrary.util.I18nRuntimeException;

/**
 * An exception thrown when the expected environmental properties are missing or invalid.
 */
public class ConfigException extends I18nRuntimeException {

    /**
     * The <code>serialVersionUID</code> code for a configuration exception.
     */
    private static final long serialVersionUID = 1620015737571747039L;

    /**
     * Create a configuration exception from the supplied message key.
     *
     * @param aMessageKey A message key for additional details
     */
    public ConfigException(final String aMessageKey) {
        super(MessageCodes.BUNDLE, MessageCodes.CAD_001, aMessageKey);
    }

    /**
     * Create a configuration exception from the supplied message key.
     *
     * @param aUriSyntaxException A parent exception indicating an invalid URI string was supplied
     * @param aMessageKey A message key with the invalid URI
     */
    public ConfigException(final URISyntaxException aUriSyntaxException, final String aMessageKey) {
        super(aUriSyntaxException, MessageCodes.BUNDLE, MessageCodes.CAD_002, aMessageKey);
    }

    /**
     * Create a configuration exception from the supplied message key.
     *
     * @param aNumberFormatException A parent exception indicating an invalid string was supplied
     * @param aMessageKey A message key with the invalid numeric string
     */
    public ConfigException(final NumberFormatException aNumberFormatException, final String aMessageKey) {
        super(aNumberFormatException, MessageCodes.BUNDLE, MessageCodes.CAD_006, aMessageKey);
    }
}
