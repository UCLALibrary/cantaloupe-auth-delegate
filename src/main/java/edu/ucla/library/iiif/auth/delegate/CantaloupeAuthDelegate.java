
package edu.ucla.library.iiif.auth.delegate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.illinois.library.cantaloupe.delegate.AbstractJavaDelegate;
import edu.illinois.library.cantaloupe.delegate.JavaDelegate;
import edu.illinois.library.cantaloupe.delegate.Logger;

/**
 * A Cantaloupe delegate for handing IIIF Auth interactions.
 */
public class CantaloupeAuthDelegate extends AbstractJavaDelegate implements JavaDelegate {

    @Override
    public String serializeMetaIdentifier(final Map<String, Object> aMetaIdentifier) {
        return null;
    }

    @Override
    public Map<String, Object> deserializeMetaIdentifier(final String aMetaIdentifier) {
        return null;
    }

    @Override
    public Object preAuthorize() {
        Logger.info("The pre-authorize identifier is: " + getContext().getIdentifier());
        return true;
    }

    @Override
    public Object authorize() {
        return true;
    }

    @Override
    public Map<String, Object> getExtraIIIF2InformationResponseKeys() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getExtraIIIF3InformationResponseKeys() {
        return Collections.emptyMap();
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public String getAzureStorageSourceBlobKey() {
        return null;
    }

    @Override
    public String getFilesystemSourcePathname() {
        return null;
    }

    @Override
    public Map<String, Object> getHTTPSourceResourceInfo() {
        return null;
    }

    @Override
    public String getJDBCSourceDatabaseIdentifier() {
        return null;
    }

    @Override
    public String getJDBCSourceMediaType() {
        return null;
    }

    @Override
    public String getJDBCSourceLookupSQL() {
        return null;
    }

    @Override
    public Map<String, String> getS3SourceObjectInfo() {
        return null;
    }

    @Override
    public Map<String, Object> getOverlay() {
        return null;
    }

    @Override
    public List<Map<String, Long>> getRedactions() {
        return Collections.emptyList();
    }

    @Override
    public String getMetadata() {
        return null;
    }

}
