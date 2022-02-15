
package edu.ucla.library.iiif.auth.delegate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.illinois.library.cantaloupe.delegate.AbstractJavaDelegate;
import edu.illinois.library.cantaloupe.delegate.JavaDelegate;

/**
 * A generic class for a Cantaloupe authorization delegate. This is really just a no-op implementation so our real
 * delegate doesn't have to implement a bunch of methods it's not going to use.
 */
public class CantaloupeDelegate extends AbstractJavaDelegate implements JavaDelegate {

    @Override
    public Object authorize() {
        return null;
    }

    @Override
    public Map<String, Object> deserializeMetaIdentifier(final String aMetaIdentifier) {
        return Collections.emptyMap();
    }

    @Override
    public String getAzureStorageSourceBlobKey() {
        return null;
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
    public String getFilesystemSourcePathname() {
        return null;
    }

    @Override
    public Map<String, Object> getHTTPSourceResourceInfo() {
        return Collections.emptyMap();
    }

    @Override
    public String getJDBCSourceDatabaseIdentifier() {
        return null;
    }

    @Override
    public String getJDBCSourceLookupSQL() {
        return null;
    }

    @Override
    public String getJDBCSourceMediaType() {
        return null;
    }

    @Override
    public String getMetadata() {
        return null;
    }

    @Override
    public Map<String, Object> getOverlay() {
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, Long>> getRedactions() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getS3SourceObjectInfo() {
        return Collections.emptyMap();
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public Object preAuthorize() {
        return null;
    }

    @Override
    public String serializeMetaIdentifier(final Map<String, Object> arg0) {
        return null;
    }

}
