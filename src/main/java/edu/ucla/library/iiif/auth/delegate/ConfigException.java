package edu.ucla.library.iiif.auth.delegate;

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
        super(MessageCodes.CAD_001, aMessageKey);
    }

    /**
     * Create a configuration exception from the supplied message key.
     *
     * @param aThrowable A parent exception
     * @param aMessageKey A message key for additional details
     */
    public ConfigException(final Throwable aThrowable, final String aMessageKey) {
        super(aThrowable, MessageCodes.CAD_002, aMessageKey);
    }
}
